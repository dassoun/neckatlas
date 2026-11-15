package com.sensitivemobility.neckatlas.feature.fretboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.NoteNotation
import com.sensitivemobility.neckatlas.domain.quiz.FretArea
import com.sensitivemobility.neckatlas.ui.CorrectColor
import com.sensitivemobility.neckatlas.ui.LocalAccentColors
import com.sensitivemobility.neckatlas.ui.LocalHapticsEnabled
import com.sensitivemobility.neckatlas.ui.OnCorrectColor
import com.sensitivemobility.neckatlas.ui.OnSeventhColor
import com.sensitivemobility.neckatlas.ui.SeventhColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val OPEN_ROW_WEIGHT = 0.7f
private const val GUTTER_DP = 30f
private const val MARGIN_DP = 12f
private const val EDGE_DP = 6f
private const val MAX_STRING_GAP_DP = 60f
private const val MAX_ROW_DP = 72f
private const val MAX_MARKER_DP = 32f

// Repères du manche : un point aux cases 3, 5, 7, 9, 15, 17, 19, 21 et deux points aux cases 12 et 24.
private val SingleInlays = setOf(3, 5, 7, 9, 15, 17, 19, 21)
private val DoubleInlays = setOf(12, 24)

// Le manche est toujours un manche en bois, en thème clair comme en thème sombre : un palissandre sombre. Un thème clair le
// montre sur fond clair, ce qui est aussi ce qu'on voit au naturel.
internal val WoodEdge = Color(0xFF28170F)
internal val WoodCenter = Color(0xFF4B2E20)
internal val FretWire = Color(0xFFCFCBD8)
internal val NutIvory = Color(0xFFE9E0C8)
private val PearlLight = Color(0xFFFFF8EA)
private val PearlShade = Color(0xFFD6CBB5)
internal val PlainStringEdge = Color(0xFF7C8593)
internal val PlainStringCore = Color(0xFFF3F6FA)
internal val WoundStringEdge = Color(0xFF6A4820)
internal val WoundStringCore = Color(0xFFEBC27C)

/** La bande qui surligne la corde interrogée par le quiz : un turquoise clair, lisible sur le bois quel que soit le thème. */
private val BandColor = Color(0xFF6FE6D8)

/** Au-dessous de cette note (Mi3 = 52), une corde est filée (bobinée) : les trois graves d'une guitare, toutes celles d'une basse. */
private const val WOUND_BELOW_MIDI = 53

/** Durée de la vibration d'une corde touchée, de l'onde qui part de la pastille, en millisecondes. */
private const val VIBRATION_MS = 1500
private const val RIPPLE_MS = 520
private const val STRING_SEGMENTS = 56

/**
 * Ce qu'un [NeckMarker] annonce : le quiz (une case à nommer avec un « ? », une bonne réponse, une erreur) ou le rôle d'une
 * note dans un accord (fondamentale, tierce, quinte, septième : une couleur chacune). [PICKED] : une case que l'utilisateur
 * vient de toucher, pas encore corrigée. [HEAT] : une case de la carte de chaleur, remplie de [NeckMarker.tint].
 */
enum class MarkerKind { TARGET, CORRECT, WRONG, ROOT, THIRD, FIFTH, SEVENTH, PICKED, HEAT, SCALE, MUTED }

/**
 * Un barré : un doigt à plat sur la case [fret], de la corde [fromString] à la corde [toString] (0 = la plus grave), dessiné
 * comme une barre claire sous les pastilles de l'accord.
 */
class NeckBar(val fret: Int, val fromString: Int, val toString: Int)

/**
 * Une pastille posée sur une case, par-dessus les notes éclairées. Elle porte le nom de la note, ou [label] s'il est donné
 * (un rôle d'accord : « R », « ♭3 »…). [faded] : discrète, pour les positions qu'on ne regarde pas.
 */
class NeckMarker(
    val position: FretPosition,
    val kind: MarkerKind,
    val label: String? = null,
    val faded: Boolean = false,
    /** La couleur d'une pastille [MarkerKind.HEAT]. */
    val tint: Color? = null,
)

/** Le texte posé sur une pastille de chaleur (aucune n'en porte pour l'instant, mais la couleur reste lisible dessus). */
private val HeatInk = Color(0xFF06121C)

private class NeckLabels(val notes: List<TextLayoutResult>, val question: TextLayoutResult, val style: TextStyle)

/**
 * Géométrie du manche en pixels. Le manche est debout, comme sur un diagramme d'accord : le sillet en haut, une case par
 * rangée, une corde par colonne (la plus grave à gauche ; à droite pour un gaucher, en miroir). Les rangées se partagent
 * toute la hauteur disponible : la zone entière est toujours visible, sans défilement.
 */
private class NeckGeometry(
    val zone: FretZone,
    val stringCount: Int,
    widthPx: Float,
    heightPx: Float,
    val dp: Float,
    val leftHanded: Boolean,
    /** Une rangée au-dessus de la première case, pour les croix des cordes étouffées d'un accord (sans cordes à vide dans la zone). */
    val header: Boolean = false,
) {
    val frets: List<Int> = zone.frets.toList()
    private val weights = frets.map { if (it == 0) OPEN_ROW_WEIGHT else 1f }
    private val headerWeight = if (header) OPEN_ROW_WEIGHT else 0f

    val gutterWidth = GUTTER_DP * dp
    val stringGap = min(MAX_STRING_GAP_DP * dp, (widthPx - gutterWidth - 2 * MARGIN_DP * dp) / stringCount)
    val neckWidth = stringGap * stringCount
    val gutterLeft = (widthPx - gutterWidth - neckWidth) / 2f
    val neckLeft = gutterLeft + gutterWidth

    val top = EDGE_DP * dp

    /** Hauteur d'une case ordinaire ; la rangée des cordes à vide est un peu plus basse. */
    val unit = min(MAX_ROW_DP * dp, (heightPx - 2 * EDGE_DP * dp) / (weights.sum() + headerWeight))
    val rowHeights: List<Float> = weights.map { it * unit }
    private val headerHeight = headerWeight * unit
    private val rowTops: List<Float> = buildList {
        var y = top + headerHeight
        rowHeights.forEach { add(y); y += it }
    }
    val bottom = top + headerHeight + rowHeights.sum()

    fun rowCenterY(row: Int): Float = rowTops[row] + rowHeights[row] / 2f

    /** Ordonnée du milieu de la rangée d'en-tête (seulement si [header]). */
    fun headerCenterY(): Float = top + headerHeight / 2f

    /** Ordonnée de la frontière au-dessus de la rangée [index] (0 = haut du manche). */
    fun boundaryY(index: Int): Float = if (index >= rowTops.size) bottom else rowTops[index]

    fun stringX(string: Int): Float {
        val column = if (leftHanded) stringCount - 1 - string else string
        return neckLeft + stringGap * (column + 0.5f)
    }

    /** Hauteur d'une pastille de note sur la rangée [row] : elle tient dans la case et dans l'écart entre deux cordes. */
    fun markerHeight(row: Int): Float = min(min(rowHeights[row] * 0.88f, stringGap * 0.9f), MAX_MARKER_DP * dp)

    fun hit(x: Float, y: Float): FretPosition? {
        if (x < neckLeft || x > neckLeft + neckWidth || y < top || y > bottom) return null
        val column = ((x - neckLeft) / stringGap).toInt().coerceIn(0, stringCount - 1)
        val string = if (leftHanded) stringCount - 1 - column else column
        val row = rowTops.indexOfLast { it <= y }
        if (row < 0) return null // l'en-tête d'un diagramme d'accord
        return FretPosition(string, frets[row])
    }
}

/** Un fil du bois : sa place sur la largeur (0 à 1), son épaisseur (dp), son opacité, et s'il est plus sombre ou plus clair. */
private class Grain(val xFraction: Float, val widthDp: Float, val alpha: Float, val dark: Boolean)

/** Une corde : son épaisseur (px), si elle est filée, son dégradé (cylindre éclairé de face) et la couleur de son cœur. */
private class StringLook(val thickness: Float, val wound: Boolean, val brush: Brush, val core: Color)

/** Tout ce qui sert à dessiner le manche et ne change qu'avec sa géométrie ou son instrument : calculé une fois. */
private class NeckLook(val wood: Brush, val shade: Brush, val grain: List<Grain>, val strings: List<StringLook>)

/** Les fils du bois, tirés avec une graine fixe : le manche garde le même grain d'un lancement à l'autre. */
private fun makeGrain(): List<Grain> {
    val random = Random(7)
    return List(22) { Grain(random.nextFloat(), 0.5f + random.nextFloat() * 1.1f, 0.05f + random.nextFloat() * 0.12f, random.nextFloat() < 0.7f) }
}

private fun makeLook(geometry: NeckGeometry, instrument: Instrument): NeckLook {
    val left = geometry.neckLeft
    val right = geometry.neckLeft + geometry.neckWidth
    val wood = Brush.horizontalGradient(0f to WoodEdge, 0.5f to WoodCenter, 1f to WoodEdge, startX = left, endX = right)
    // Les bords du manche s'assombrissent : il paraît bombé.
    val shade = Brush.horizontalGradient(
        0f to Color.Black.copy(alpha = 0.38f),
        0.14f to Color.Transparent,
        0.86f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.38f),
        startX = left,
        endX = right,
    )
    val strings = instrument.openStrings.mapIndexed { index, open ->
        // L'épaisseur suit la hauteur de la corde à vide : Mi aigu fin, Mi grave de basse épais, quel que soit l'instrument.
        val thickness = (1.0f + (64 - open.midi).coerceAtLeast(0) * 0.07f) * geometry.dp
        val wound = open.midi < WOUND_BELOW_MIDI
        val edge = if (wound) WoundStringEdge else PlainStringEdge
        val core = if (wound) WoundStringCore else PlainStringCore
        val x = geometry.stringX(index)
        StringLook(
            thickness,
            wound,
            Brush.horizontalGradient(0f to edge, 0.5f to core, 1f to edge, startX = x - thickness / 2, endX = x + thickness / 2),
            core,
        )
    }
    return NeckLook(wood, shade, makeGrain(), strings)
}

/**
 * Le manche, dessiné en hauteur et ajusté à la place disponible : toutes les cases de la zone sont visibles en même temps,
 * et plus la zone est petite, plus les cases sont grandes. Les cordes graves sont à gauche.
 *
 * C'est un manche en bois : touche en palissandre, frettes argentées, sillet d'os, incrustations nacrées, cordes de plus en
 * plus épaisses vers le grave (filées pour les graves). Quand on le touche, la corde vibre et s'éteint, une onde part de la
 * case, la pastille de la note « claque » en place, et le téléphone vibre légèrement (si le réglage est actif).
 *
 * @param highlightedPitchClass classe de hauteur (0 = Do) à éclairer partout, ou null.
 * @param showNames affiche le nom de chaque note du manche.
 * @param selected l'emplacement touché, entouré d'ambre.
 * @param enabled faux : les touchers sont ignorés (le quiz attend la fin d'une correction).
 * @param markers pastilles du quiz (case à nommer, bonne réponse, erreur), des rôles d'un accord ou des notes d'une gamme. Une
 * pastille [MarkerKind.MUTED] est une croix : la corde est étouffée, elle se pose sur la rangée du sillet ou, si la zone n'en
 * a pas, sur une rangée d'en-tête ajoutée au-dessus de la première case.
 * @param bars barrés à dessiner sous les pastilles.
 * @param emphasizedStrings cordes (indices, 0 = la plus grave) sur lesquelles on interroge : elles sont surlignées.
 * @param focus fenêtre où l'on doit répondre : le reste du manche est assombri et les touchers qui tombent dehors sont ignorés.
 * @param touchFeedback faux : ni vibration de la corde ni retour vibré (une carte à consulter plutôt qu'un instrument à jouer).
 */
@Composable
fun FretboardView(
    instrument: Instrument,
    zone: FretZone,
    notation: NoteNotation,
    accidentals: AccidentalStyle,
    leftHanded: Boolean,
    highlightedPitchClass: Int?,
    showNames: Boolean,
    selected: FretPosition?,
    onSelect: (FretPosition) -> Unit,
    enabled: Boolean = true,
    markers: List<NeckMarker> = emptyList(),
    bars: List<NeckBar> = emptyList(),
    emphasizedStrings: Set<Int> = emptySet(),
    focus: FretArea? = null,
    touchFeedback: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val board = remember(instrument) { Fretboard(instrument) }
    val textMeasurer = rememberTextMeasurer(cacheSize = 128)
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val accents = LocalAccentColors.current
    val currentOnSelect by rememberUpdatedState(onSelect)

    // Animations d'un toucher. Chacune va de 0 à 1 ; au repos elles valent 1 (rien n'est dessiné).
    val scope = rememberCoroutineScope()
    val vibration = remember { Animatable(1f) }
    val ripple = remember { Animatable(1f) }
    val pop = remember { Animatable(1f) }
    var tapped by remember { mutableStateOf<FretPosition?>(null) }
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current

    BoxWithConstraints(modifier) {
        val dp = density.density
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val header = !zone.includesOpenStrings && markers.any { it.kind == MarkerKind.MUTED }
        val geometry = remember(zone, instrument.stringCount, widthPx, heightPx, dp, leftHanded, header) {
            NeckGeometry(zone, instrument.stringCount, widthPx, heightPx, dp, leftHanded, header)
        }
        val look = remember(geometry, instrument) { makeLook(geometry, instrument) }
        val winding = remember(dp) { PathEffect.dashPathEffect(floatArrayOf(1.1f * dp, 1.1f * dp)) }

        // Taille des noms : proportionnelle à la pastille, puis réduite si le plus long (Sol♯/La♭) dépasse l'écart entre cordes.
        val regularMarker = min(min(geometry.unit * 0.88f, geometry.stringGap * 0.9f), MAX_MARKER_DP * dp)
        val showLabels = regularMarker >= 11 * dp
        val labels: NeckLabels = remember(notation, accidentals, regularMarker, geometry.stringGap, textMeasurer) {
            fun style(fontPx: Float) = TextStyle(fontSize = with(density) { fontPx.toSp() }, fontWeight = FontWeight.SemiBold)
            fun measure(fontPx: Float) = (0..11).map { textMeasurer.measure(NoteNames.label(it, notation, accidentals), style(fontPx)) }
            var fontPx = min(regularMarker * 0.5f, 13 * dp)
            var layouts = measure(fontPx)
            val allowed = geometry.stringGap * 0.94f
            val widest = layouts.maxOf { it.size.width }
            if (widest > allowed) {
                fontPx *= allowed / widest
                layouts = measure(fontPx)
            }
            NeckLabels(layouts, textMeasurer.measure("?", style(fontPx)), style(fontPx))
        }
        val noteLabels = labels.notes
        val numberStyle = remember(geometry.unit) {
            TextStyle(fontSize = with(density) { min(11 * dp, geometry.unit * 0.6f).toSp() }, fontWeight = FontWeight.Medium)
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(geometry, enabled, touchFeedback, hapticsOn, focus) {
                    if (enabled) {
                        detectTapGestures { offset ->
                            val position = geometry.hit(offset.x, offset.y) ?: return@detectTapGestures
                            if (focus != null && position !in focus) return@detectTapGestures
                            currentOnSelect(position)
                            if (touchFeedback) {
                                tapped = position
                                // « Touche virtuelle » : le clic bref que tous les téléphones associent à une vibration. Essayé avec
                                // « TextHandleMove » (poignée de texte) : sur l'émulateur Android 15, rien ne vibre.
                                if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                scope.launch { vibration.snapTo(0f); vibration.animateTo(1f, tween(VIBRATION_MS, easing = LinearEasing)) }
                                scope.launch { ripple.snapTo(0f); ripple.animateTo(1f, tween(RIPPLE_MS, easing = FastOutSlowInEasing)) }
                                // Un ressort qui dépasse 1 un instant : la pastille « claque » en place.
                                scope.launch { pop.snapTo(0f); pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 450f)) }
                            }
                        }
                    }
                },
        ) {
            val dpf = geometry.dp
            val left = geometry.neckLeft
            val right = geometry.neckLeft + geometry.neckWidth
            val neckTop = geometry.top
            val neckSize = Size(geometry.neckWidth, geometry.bottom - geometry.top)
            val corner = CornerRadius(7 * dpf)

            // Bois : dégradé de bord à bord, fils du grain, bords assombris, contour.
            drawRoundRect(look.wood, Offset(left, neckTop), neckSize, corner)
            for (grain in look.grain) {
                val x = left + grain.xFraction * geometry.neckWidth
                val ink = if (grain.dark) Color.Black.copy(alpha = grain.alpha) else Color.White.copy(alpha = grain.alpha * 0.5f)
                drawLine(ink, Offset(x, neckTop + 2 * dpf), Offset(x, geometry.bottom - 2 * dpf), strokeWidth = grain.widthDp * dpf)
            }
            drawRoundRect(look.shade, Offset(left, neckTop), neckSize, corner)
            drawRoundRect(Color.Black.copy(alpha = 0.55f), Offset(left, neckTop), neckSize, corner, style = Stroke(1.2f * dpf))

            // Repères nacrés.
            val inlayRadius = 5.5f * dpf
            val centerX = left + geometry.neckWidth / 2
            geometry.frets.forEachIndexed { row, fret ->
                val y = geometry.rowCenterY(row)
                when (fret) {
                    in SingleInlays -> drawPearl(Offset(centerX, y), inlayRadius, dpf)
                    in DoubleInlays -> {
                        drawPearl(Offset(centerX - geometry.neckWidth * 0.2f, y), inlayRadius, dpf)
                        drawPearl(Offset(centerX + geometry.neckWidth * 0.2f, y), inlayRadius, dpf)
                    }
                }
            }

            // Frettes, et sillet d'os entre la corde à vide et la case 1.
            for (boundary in 1 until geometry.frets.size) {
                val y = geometry.boundaryY(boundary)
                if (geometry.frets[boundary - 1] == 0) drawNut(y, left, right, dpf) else drawFretWire(y, left, right, dpf)
            }
            if (!zone.includesOpenStrings) drawFretWire(geometry.boundaryY(0), left, right, dpf)

            // Corde interrogée par le quiz : une bande claire derrière elle, bien cernée.
            for (string in emphasizedStrings) {
                if (string !in 0 until geometry.stringCount) continue
                val band = geometry.stringGap * 0.9f
                val topLeft = Offset(geometry.stringX(string) - band / 2, neckTop)
                val size = Size(band, geometry.bottom - neckTop)
                drawRoundRect(BandColor.copy(alpha = 0.42f), topLeft, size, CornerRadius(8 * dpf))
                drawRoundRect(BandColor.copy(alpha = 0.95f), topLeft, size, CornerRadius(8 * dpf), style = Stroke(2f * dpf))
            }

            // Cordes : plus épaisses vers le grave, filées pour les graves. Celle qu'on vient de toucher vibre.
            val vibrationProgress = vibration.value
            val vibrating = if (vibrationProgress < 1f) tapped?.string else null
            for (string in 0 until geometry.stringCount) {
                if (string == vibrating) continue
                val x = geometry.stringX(string)
                val stringLook = look.strings[string]
                drawLine(
                    Color.Black.copy(alpha = 0.38f),
                    Offset(x + 1.6f * dpf, neckTop),
                    Offset(x + 1.6f * dpf, geometry.bottom),
                    strokeWidth = stringLook.thickness + 0.8f * dpf,
                )
                drawLine(stringLook.brush, Offset(x, neckTop), Offset(x, geometry.bottom), strokeWidth = stringLook.thickness)
                if (stringLook.wound) {
                    drawLine(
                        Color.Black.copy(alpha = 0.34f),
                        Offset(x, neckTop),
                        Offset(x, geometry.bottom),
                        strokeWidth = stringLook.thickness * 0.96f,
                        pathEffect = winding,
                    )
                }
            }
            if (vibrating != null && vibrating in 0 until geometry.stringCount) {
                drawVibratingString(geometry, look.strings[vibrating], vibrating, vibrationProgress)
            }

            // Fenêtre de réponse : le reste du manche est assombri, la fenêtre cernée.
            if (focus != null) {
                val firstRow = geometry.frets.indexOfFirst { it >= focus.zone.first }.coerceAtLeast(0)
                val lastRow = geometry.frets.indexOfLast { it <= focus.zone.last }.coerceAtLeast(firstRow)
                val xa = geometry.stringX(focus.firstString)
                val xb = geometry.stringX(focus.lastString)
                val x0 = minOf(xa, xb) - geometry.stringGap / 2
                val x1 = maxOf(xa, xb) + geometry.stringGap / 2
                val y0 = geometry.boundaryY(firstRow)
                val y1 = geometry.boundaryY(lastRow + 1)
                val veil = Color(0xFF05090F).copy(alpha = 0.62f)
                drawRect(veil, Offset(left, neckTop), Size(geometry.neckWidth, y0 - neckTop))
                drawRect(veil, Offset(left, y1), Size(geometry.neckWidth, geometry.bottom - y1))
                drawRect(veil, Offset(left, y0), Size(x0 - left, y1 - y0))
                drawRect(veil, Offset(x1, y0), Size(right - x1, y1 - y0))
                drawRoundRect(BandColor, Offset(x0, y0), Size(x1 - x0, y1 - y0), CornerRadius(6 * dpf), style = Stroke(2.5f * dpf))
            }

            // Numéros de cases, dans la marge de gauche.
            geometry.frets.forEachIndexed { row, fret ->
                val layout = textMeasurer.measure(fret.toString(), numberStyle)
                drawCentered(layout, colors.onSurfaceVariant, Offset(geometry.gutterLeft + geometry.gutterWidth / 2, geometry.rowCenterY(row)))
            }

            // Notes : pastille arrondie, assez large pour son nom.
            val highlight = highlightedPitchClass
            geometry.frets.forEachIndexed { row, fret ->
                val baseHeight = geometry.markerHeight(row)
                for (string in 0 until geometry.stringCount) {
                    val position = FretPosition(string, fret)
                    val pitchClass = board.pitchAt(position).pitchClass
                    val isHighlighted = highlight != null && pitchClass == highlight
                    val isSelected = selected == position
                    if (!isHighlighted && !isSelected && !showNames) continue

                    val center = Offset(geometry.stringX(string), geometry.rowCenterY(row))
                    val label = noteLabels[pitchClass]
                    // La pastille qu'on vient de toucher « claque » : elle part petite et dépasse un instant sa taille.
                    val scale = if (isSelected && position == tapped) 0.6f + 0.4f * pop.value else 1f
                    val height = baseHeight * scale
                    val width = (if (showLabels) min(max(height, label.size.width + height * 0.5f), geometry.stringGap * 0.98f) else height)
                    val topLeft = Offset(center.x - width / 2, center.y - height / 2)
                    val shape = CornerRadius(height / 2)
                    val (fill, ink) = when {
                        isHighlighted -> colors.primary to colors.onPrimary
                        isSelected -> colors.tertiary to colors.onTertiary
                        else -> colors.surface to colors.onSurface
                    }
                    if (isSelected) {
                        // Une lueur ambrée autour de la note jouée.
                        val glow = 5 * dpf
                        drawRoundRect(
                            colors.tertiary.copy(alpha = 0.28f),
                            Offset(topLeft.x - glow, topLeft.y - glow),
                            Size(width + 2 * glow, height + 2 * glow),
                            CornerRadius(height / 2 + glow),
                        )
                    }
                    drawRoundRect(fill, topLeft, Size(width, height), shape)
                    if (!isHighlighted && !isSelected) {
                        drawRoundRect(colors.outline, topLeft, Size(width, height), shape, style = Stroke(1 * dpf))
                    }
                    if (isSelected && isHighlighted) {
                        val ring = 3 * dpf
                        drawRoundRect(
                            colors.tertiary,
                            Offset(topLeft.x - ring, topLeft.y - ring),
                            Size(width + 2 * ring, height + 2 * ring),
                            CornerRadius(height / 2 + ring),
                            style = Stroke(2.5f * dpf),
                        )
                    }
                    if (showLabels) drawCentered(label, ink, center)
                }
            }

            // Barrés : une barre claire d'une corde à l'autre, sous les pastilles.
            for (bar in bars) {
                val row = geometry.frets.indexOf(bar.fret)
                if (row < 0) continue
                val from = bar.fromString.coerceIn(0, geometry.stringCount - 1)
                val to = bar.toString.coerceIn(0, geometry.stringCount - 1)
                val height = geometry.markerHeight(row) * 0.82f
                val x1 = min(geometry.stringX(from), geometry.stringX(to)) - height / 2
                val x2 = max(geometry.stringX(from), geometry.stringX(to)) + height / 2
                val topLeft = Offset(x1, geometry.rowCenterY(row) - height / 2)
                val size = Size(x2 - x1, height)
                drawRoundRect(NutIvory.copy(alpha = 0.9f), topLeft, size, CornerRadius(height / 2))
                drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft, size, CornerRadius(height / 2), style = Stroke(1 * dpf))
            }

            // Pastilles du quiz, d'un accord ou d'une gamme, par-dessus tout le reste.
            for (marker in markers) {
                if (marker.position.string !in 0 until geometry.stringCount) continue
                val row = geometry.frets.indexOf(marker.position.fret)
                if (marker.kind == MarkerKind.MUTED) {
                    // Une croix sur la rangée du sillet, ou sur l'en-tête.
                    val y = when {
                        row >= 0 -> geometry.rowCenterY(row)
                        geometry.header -> geometry.headerCenterY()
                        else -> continue
                    }
                    val arm = (if (row >= 0) geometry.markerHeight(row) else min(geometry.unit * OPEN_ROW_WEIGHT * 0.88f, MAX_MARKER_DP * dpf)) * 0.26f
                    val x = geometry.stringX(marker.position.string)
                    val cross = NutIvory.copy(alpha = 0.92f)
                    drawLine(cross, Offset(x - arm, y - arm), Offset(x + arm, y + arm), strokeWidth = 2.4f * dpf, cap = StrokeCap.Round)
                    drawLine(cross, Offset(x - arm, y + arm), Offset(x + arm, y - arm), strokeWidth = 2.4f * dpf, cap = StrokeCap.Round)
                    continue
                }
                if (row < 0) continue
                val height = geometry.markerHeight(row)
                val center = Offset(geometry.stringX(marker.position.string), geometry.rowCenterY(row))
                val label = when {
                    marker.label != null -> textMeasurer.measure(marker.label, labels.style)
                    marker.kind == MarkerKind.TARGET -> labels.question
                    else -> noteLabels[board.pitchAt(marker.position).pitchClass]
                }
                val width = if (showLabels) min(max(height, label.size.width + height * 0.5f), geometry.stringGap * 0.98f) else height
                // Fondamentale ambre, tierce turquoise, quinte bleue, septième violette : des couleurs qui disent un rôle, donc
                // indépendantes de la couleur de la marque.
                val (solid, solidInk) = when (marker.kind) {
                    MarkerKind.TARGET -> colors.tertiary to colors.onTertiary
                    MarkerKind.CORRECT -> CorrectColor to OnCorrectColor
                    MarkerKind.WRONG -> colors.error to colors.onError
                    MarkerKind.ROOT -> colors.tertiary to colors.onTertiary
                    MarkerKind.THIRD -> accents.teal to accents.onTeal
                    MarkerKind.FIFTH -> accents.blue to accents.onBlue
                    MarkerKind.SEVENTH -> SeventhColor to OnSeventhColor
                    MarkerKind.PICKED -> colors.onSurface to colors.surface
                    MarkerKind.HEAT -> (marker.tint ?: colors.outline) to HeatInk
                    MarkerKind.SCALE -> colors.primary to colors.onPrimary
                    MarkerKind.MUTED -> colors.outline to colors.surface // jamais atteint : une croix se dessine plus haut
                }
                val fill = if (marker.faded) solid.copy(alpha = 0.3f) else solid
                val ink = if (marker.faded) solidInk.copy(alpha = 0.55f) else solidInk
                val topLeft = Offset(center.x - width / 2, center.y - height / 2)
                drawRoundRect(fill, topLeft, Size(width, height), CornerRadius(height / 2))
                if (marker.kind == MarkerKind.TARGET) {
                    val ring = 3 * dpf
                    drawRoundRect(
                        colors.tertiary,
                        Offset(topLeft.x - ring, topLeft.y - ring),
                        Size(width + 2 * ring, height + 2 * ring),
                        CornerRadius(height / 2 + ring),
                        style = Stroke(2.5f * dpf),
                    )
                }
                if (showLabels) drawCentered(label, ink, center)
            }

            // L'onde qui part de la case touchée, par-dessus tout.
            val rippleProgress = ripple.value
            val rippleAt = tapped
            if (rippleProgress < 1f && rippleAt != null) {
                val row = geometry.frets.indexOf(rippleAt.fret)
                if (row >= 0 && rippleAt.string in 0 until geometry.stringCount) {
                    val markerHeight = geometry.markerHeight(row)
                    drawCircle(
                        color = colors.tertiary.copy(alpha = 0.6f * (1f - rippleProgress)),
                        radius = markerHeight / 2 + markerHeight * 0.95f * rippleProgress,
                        center = Offset(geometry.stringX(rippleAt.string), geometry.rowCenterY(row)),
                        style = Stroke((2.4f * (1f - rippleProgress) + 0.8f) * dpf),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCentered(layout: TextLayoutResult, color: Color, center: Offset) {
    drawText(layout, color = color, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

/** Un repère de nacre : une ombre portée, puis un disque éclairé en haut à gauche. */
internal fun DrawScope.drawPearl(center: Offset, radius: Float, dpf: Float) {
    drawCircle(Color.Black.copy(alpha = 0.45f), radius + 0.8f * dpf, Offset(center.x, center.y + 0.9f * dpf))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PearlLight, PearlShade),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = center,
    )
}

/** Une frette : un fil d'argent, avec son ombre dessous et un reflet dessus. */
internal fun DrawScope.drawFretWire(y: Float, left: Float, right: Float, dpf: Float) {
    drawLine(Color.Black.copy(alpha = 0.5f), Offset(left, y + 1.3f * dpf), Offset(right, y + 1.3f * dpf), strokeWidth = 2.2f * dpf)
    drawLine(FretWire, Offset(left, y), Offset(right, y), strokeWidth = 1.7f * dpf)
    drawLine(Color.White.copy(alpha = 0.55f), Offset(left, y - 0.45f * dpf), Offset(right, y - 0.45f * dpf), strokeWidth = 0.7f * dpf)
}

/** Le sillet : une barre d'os un peu plus large que le manche, avec son ombre et un reflet. */
private fun DrawScope.drawNut(y: Float, left: Float, right: Float, dpf: Float) {
    val thickness = 6 * dpf
    val overhang = 1.5f * dpf
    val topLeft = Offset(left - overhang, y - thickness / 2)
    val size = Size(right - left + 2 * overhang, thickness)
    val corner = CornerRadius(2 * dpf)
    drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(topLeft.x, topLeft.y + 1.5f * dpf), size, corner)
    drawRoundRect(NutIvory, topLeft, size, corner)
    drawLine(Color.White.copy(alpha = 0.6f), Offset(left, topLeft.y + 0.9f * dpf), Offset(right, topLeft.y + 0.9f * dpf), strokeWidth = 0.9f * dpf)
}

/**
 * Une corde qui vient d'être pincée : une onde stationnaire, qui s'éteint en [VIBRATION_MS]. Une seconde copie, plus pâle et
 * déphasée, donne l'effet de flou d'une corde qui bouge vite. Les cordes graves vibrent plus lentement, comme au naturel.
 */
private fun DrawScope.drawVibratingString(geometry: NeckGeometry, look: StringLook, string: Int, progress: Float) {
    val dpf = geometry.dp
    val seconds = progress * VIBRATION_MS / 1000f
    val frequency = 6f + 10f * string / max(1, geometry.stringCount - 1)
    val phase = 2f * PI.toFloat() * frequency * seconds
    val decay = (1f - progress).pow(1.8f)
    val amplitude = min(geometry.stringGap * 0.2f, 4.2f * dpf) * decay
    val x = geometry.stringX(string)

    fun wave(amp: Float, shift: Float, offset: Float): Path = Path().apply {
        for (i in 0..STRING_SEGMENTS) {
            val s = i / STRING_SEGMENTS.toFloat()
            val px = x + offset + amp * sin(PI.toFloat() * s) * cos(phase + shift)
            val py = geometry.top + s * (geometry.bottom - geometry.top)
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
    }

    val stroke = Stroke(look.thickness, cap = StrokeCap.Butt, join = StrokeJoin.Round)
    drawPath(wave(amplitude, 0f, 1.6f * dpf), Color.Black.copy(alpha = 0.38f), style = Stroke(look.thickness + 0.8f * dpf, join = StrokeJoin.Round))
    drawPath(wave(amplitude * 0.7f, 0.9f, 0f), look.core.copy(alpha = 0.3f), style = Stroke(look.thickness + 1.6f * dpf, join = StrokeJoin.Round))
    drawPath(wave(amplitude, 0f, 0f), look.core, style = stroke)
}
