package com.sensitivemobility.neckatlas.feature.chords

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensitivemobility.neckatlas.domain.chord.ChordPosition
import kotlin.math.max
import kotlin.math.min

/** Le nombre de rangées de cases d'un diagramme : la main tient dans quatre cases. */
private const val DIAGRAM_ROWS = 4

/**
 * La première case montrée par le diagramme d'une position : la première case du manche quand tout y tient (le sillet est
 * dessiné en haut), sinon la case la plus basse de la main (son numéro est écrit à gauche).
 */
internal fun diagramBaseFret(position: ChordPosition): Int = if (position.highestFret <= DIAGRAM_ROWS) 1 else position.lowestFret

/**
 * Un petit diagramme d'accord, comme dans un dictionnaire : les cordes en colonnes (la plus grave à gauche), quatre cases en
 * rangées, une croix au-dessus des cordes étouffées, un rond au-dessus de celles qui sonnent à vide, un point avec son doigt
 * sur chaque corde appuyée, une barre pour un barré. Toucher la carte choisit la position.
 */
@Composable
fun ChordDiagramCard(position: ChordPosition, selected: Boolean, leftHanded: Boolean, description: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val fingerStyle = remember { TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold) }
    val fingerLabels = remember(measurer) { (1..4).map { measurer.measure(it.toString(), fingerStyle) } }
    val baseStyle = remember { TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium) }
    val base = diagramBaseFret(position)
    val baseLabel = remember(measurer, base) { measurer.measure(base.toString(), baseStyle) }
    val shape = RoundedCornerShape(14.dp)

    Box(
        Modifier
            .width(76.dp)
            .height(92.dp)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .then(if (selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(8.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dp = density
            val stringCount = position.frets.size
            val labelWidth = if (base > 1) 11 * dp else 0f
            val top = 13 * dp
            val gridLeft = labelWidth + 2 * dp
            val gridWidth = size.width - gridLeft - 2 * dp
            val gridHeight = size.height - top - 1 * dp
            val gap = gridWidth / (stringCount - 1)
            val rowHeight = gridHeight / DIAGRAM_ROWS

            fun xOf(string: Int): Float = gridLeft + gap * (if (leftHanded) stringCount - 1 - string else string)
            fun yOf(fret: Int): Float = top + rowHeight * (fret - base + 0.5f)

            // Cases : le sillet (trait épais) quand le diagramme part du manche, sinon un trait comme les autres.
            for (i in 0..DIAGRAM_ROWS) {
                val y = top + rowHeight * i
                val nut = i == 0 && base == 1
                drawLine(
                    if (nut) colors.onSurface else colors.outline,
                    Offset(gridLeft, y),
                    Offset(gridLeft + gridWidth, y),
                    strokeWidth = (if (nut) 2.6f else 1f) * dp,
                )
            }
            // Cordes.
            for (string in 0 until stringCount) {
                drawLine(colors.outline, Offset(xOf(string), top), Offset(xOf(string), top + gridHeight), strokeWidth = 1f * dp)
            }
            // Numéro de la première case, à gauche, quand le diagramme ne part pas du sillet.
            if (base > 1) {
                drawText(baseLabel, colors.onSurfaceVariant, Offset(0f, yOf(base) - baseLabel.size.height / 2f))
            }
            // Au-dessus des cordes : une croix si elle est étouffée, un rond si elle sonne à vide.
            val markY = top / 2f
            val arm = 3f * dp
            for (string in 0 until stringCount) {
                val x = xOf(string)
                when (position.frets[string]) {
                    null -> {
                        drawLine(colors.onSurfaceVariant, Offset(x - arm, markY - arm), Offset(x + arm, markY + arm), strokeWidth = 1.5f * dp, cap = StrokeCap.Round)
                        drawLine(colors.onSurfaceVariant, Offset(x - arm, markY + arm), Offset(x + arm, markY - arm), strokeWidth = 1.5f * dp, cap = StrokeCap.Round)
                    }
                    0 -> drawCircle(colors.onSurfaceVariant, radius = arm, center = Offset(x, markY), style = Stroke(1.5f * dp))
                }
            }
            // Barré : une barre pleine, avec le « 1 » de l'index à son bout.
            val barre = position.barre
            val dotRadius = min(rowHeight * 0.4f, gap * 0.46f)
            if (barre != null && barre.fret - base in 0 until DIAGRAM_ROWS) {
                val x1 = min(xOf(barre.fromString), xOf(barre.toString)) - dotRadius
                val x2 = max(xOf(barre.fromString), xOf(barre.toString)) + dotRadius
                drawRoundRect(colors.primary, Offset(x1, yOf(barre.fret) - dotRadius), Size(x2 - x1, dotRadius * 2), CornerRadius(dotRadius))
            }
            // Un point sur chaque corde appuyée, avec le numéro du doigt ; sous le barré, la barre suffit.
            for (string in 0 until stringCount) {
                val fret = position.frets[string] ?: continue
                val finger = position.fingers[string] ?: continue
                if (fret - base !in 0 until DIAGRAM_ROWS) continue
                val underBar = barre != null && finger == 1 && fret == barre.fret
                val center = Offset(xOf(string), yOf(fret))
                if (!underBar) drawCircle(colors.primary, dotRadius, center)
                val showNumber = !underBar || string == (if (leftHanded) barre!!.toString else barre!!.fromString)
                if (showNumber && finger in 1..4) {
                    val layout = fingerLabels[finger - 1]
                    drawText(layout, colors.onPrimary, Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
                }
            }
        }
    }
}
