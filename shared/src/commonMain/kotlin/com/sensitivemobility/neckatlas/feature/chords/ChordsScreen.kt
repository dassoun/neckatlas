package com.sensitivemobility.neckatlas.feature.chords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sensitivemobility.neckatlas.audio.NotePlayer
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.chord.Chord
import com.sensitivemobility.neckatlas.domain.chord.ChordPosition
import com.sensitivemobility.neckatlas.domain.chord.ChordPositions
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.chord.Voicing
import com.sensitivemobility.neckatlas.domain.chord.Voicings
import com.sensitivemobility.neckatlas.feature.fretboard.FretboardView
import com.sensitivemobility.neckatlas.feature.fretboard.MarkerKind
import com.sensitivemobility.neckatlas.feature.fretboard.NeckBar
import com.sensitivemobility.neckatlas.feature.fretboard.NeckMarker
import com.sensitivemobility.neckatlas.feature.setup.SetupSheetHost
import com.sensitivemobility.neckatlas.feature.setup.SetupSummary
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.chords_arpeggio_info
import com.sensitivemobility.neckatlas.shared.resources.chords_fingers_toggle
import com.sensitivemobility.neckatlas.shared.resources.chords_fret_single
import com.sensitivemobility.neckatlas.shared.resources.chords_frets_range
import com.sensitivemobility.neckatlas.shared.resources.chords_kind_barre
import com.sensitivemobility.neckatlas.shared.resources.chords_kind_movable
import com.sensitivemobility.neckatlas.shared.resources.chords_kind_open
import com.sensitivemobility.neckatlas.shared.resources.chords_mode_arpeggio
import com.sensitivemobility.neckatlas.shared.resources.chords_mode_positions
import com.sensitivemobility.neckatlas.shared.resources.chords_mode_shape
import com.sensitivemobility.neckatlas.shared.resources.chords_names_toggle
import com.sensitivemobility.neckatlas.shared.resources.chords_no_position
import com.sensitivemobility.neckatlas.shared.resources.chords_no_shape
import com.sensitivemobility.neckatlas.shared.resources.chords_play_arpeggio
import com.sensitivemobility.neckatlas.shared.resources.chords_play_chord
import com.sensitivemobility.neckatlas.shared.resources.chords_position_chip
import com.sensitivemobility.neckatlas.shared.resources.chords_position_info
import com.sensitivemobility.neckatlas.shared.resources.chords_root_next
import com.sensitivemobility.neckatlas.shared.resources.chords_root_previous
import com.sensitivemobility.neckatlas.shared.resources.chords_sheet_type
import com.sensitivemobility.neckatlas.shared.resources.chords_strings_chip
import com.sensitivemobility.neckatlas.shared.resources.chords_title
import com.sensitivemobility.neckatlas.shared.resources.chords_type_next
import com.sensitivemobility.neckatlas.shared.resources.chords_type_previous
import com.sensitivemobility.neckatlas.shared.resources.chords_voicing_info
import com.sensitivemobility.neckatlas.ui.InfoCard
import com.sensitivemobility.neckatlas.ui.PlayButton
import com.sensitivemobility.neckatlas.ui.RootAndTypeSheet
import com.sensitivemobility.neckatlas.ui.Stepper
import com.sensitivemobility.neckatlas.ui.displayName
import com.sensitivemobility.neckatlas.ui.inversionName
import com.sensitivemobility.neckatlas.ui.stringsText
import org.jetbrains.compose.resources.stringResource
import kotlin.math.sqrt

private fun kindOf(role: Int): MarkerKind = when (role) {
    0 -> MarkerKind.ROOT
    1 -> MarkerKind.THIRD
    2 -> MarkerKind.FIFTH
    else -> MarkerKind.SEVENTH
}

/** Les trois façons de regarder un accord. */
private const val MODE_SHAPE = 0
private const val MODE_ARPEGGIO = 1
private const val MODE_POSITIONS = 2

/**
 * L'onglet Accords : on choisit une fondamentale et une famille (triades, septièmes), puis soit une **forme** d'accord
 * (une note par corde sur des cordes voisines, avec son renversement), soit un **arpège** (toutes les notes de l'accord dans
 * la zone), soit les **positions** du dictionnaire (les accords tels qu'on les joue : cordes à vide, barrés, doigtés).
 * Chaque note porte la couleur de son rôle : fondamentale ambre, tierce turquoise, quinte bleue, septième violette.
 * Tout se joue : l'accord d'un coup, ou ses notes l'une après l'autre.
 */
@Composable
fun ChordsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, notes: NotePlayer) {
    val instrument = settings.instrument
    val zone = settings.zone
    var rootPc by rememberSaveable { mutableIntStateOf(0) }
    var qualityName by rememberSaveable { mutableStateOf(ChordQuality.MAJOR.name) }
    var mode by rememberSaveable { mutableIntStateOf(MODE_SHAPE) }
    var inversionChoice by rememberSaveable { mutableIntStateOf(0) }
    // Lié à l'instrument : le nombre de cordes change les groupes possibles. -1 : le groupe le plus aigu.
    var setChoice by rememberSaveable(instrument) { mutableIntStateOf(-1) }
    var showNames by rememberSaveable { mutableStateOf(false) }
    var showFingers by rememberSaveable { mutableStateOf(true) }
    var setupOpen by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    val arpeggioMode = mode == MODE_ARPEGGIO
    val positionsMode = mode == MODE_POSITIONS
    val quality = ChordQuality.entries.firstOrNull { it.name == qualityName } ?: ChordQuality.MAJOR
    val chord = Chord(rootPc, quality)
    val board = remember(instrument) { Fretboard(instrument) }
    val count = quality.toneCount
    val inversion = inversionChoice.coerceIn(0, count - 1)
    val sets = Voicings.stringSets(instrument, count)
    // Sans choix de l'utilisateur : le groupe de cordes le plus aigu qui offre une forme dans la zone (à défaut, le plus aigu).
    val start = remember(board, zone, chord, inversion, setChoice) {
        if (setChoice >= 0) {
            setChoice.coerceIn(sets.first(), sets.last())
        } else {
            sets.lastOrNull { Voicings.find(board, zone, chord, inversion, it).isNotEmpty() } ?: sets.last()
        }
    }

    val voicings = remember(board, zone, chord, inversion, start) { Voicings.find(board, zone, chord, inversion, start) }
    // Quand l'accord, le renversement, le groupe de cordes ou la zone change, on revient à la première position.
    var voicingChoice by rememberSaveable(chord, inversion, start, zone) { mutableIntStateOf(0) }
    val voicingIndex = voicingChoice.coerceIn(0, maxOf(0, voicings.lastIndex))
    val voicing = voicings.getOrNull(voicingIndex)
    val arpeggioNotes = remember(board, zone, chord) { Voicings.arpeggio(board, zone, chord) }

    // Le dictionnaire : les positions de l'accord sur cet instrument, indépendantes de la zone de travail.
    val positions = remember(instrument, chord) { ChordPositions.find(instrument, chord) }
    var positionChoice by rememberSaveable(chord, instrument) { mutableIntStateOf(0) }
    val positionIndex = positionChoice.coerceIn(0, maxOf(0, positions.lastIndex))
    val position = positions.getOrNull(positionIndex)

    val markers = remember(mode, voicings, voicingIndex, arpeggioNotes, position, showNames, showFingers, quality) {
        fun label(role: Int): String? = if (showNames) null else quality.roleLabels[role]
        when {
            positionsMode -> position?.let { positionMarkers(it, quality, showNames, showFingers) }.orEmpty()
            arpeggioMode -> arpeggioNotes.map { NeckMarker(it.position, kindOf(it.role), label(it.role)) }
            // La position choisie est dessinée en dernier, donc par-dessus ; les autres restent discrètes.
            else -> voicings.withIndex().sortedBy { it.index == voicingIndex }.flatMap { (index, voicing) ->
                voicing.positions.mapIndexed { string, position ->
                    NeckMarker(position, kindOf(voicing.roles[string]), label(voicing.roles[string]), faded = index != voicingIndex)
                }
            }
        }
    }
    val bars = remember(position, positionsMode) {
        position?.barre?.takeIf { positionsMode }?.let { listOf(NeckBar(it.fret, it.fromString, it.toString)) }.orEmpty()
    }
    val shownZone = if (positionsMode && position != null) ChordPositions.zoneOf(position) else zone

    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SetupSummary(instrument, zone, onClick = { setupOpen = true })

        // Une barre d'accord (fondamentale et famille, avec des flèches pour passer à la suivante) et une seule rangée d'options :
        // quatre rangées de pastilles prenaient un quart de l'écran, que le manche récupère.
        ChordBar(
            rootLabel = NoteNames.label(rootPc, settings.notation, settings.accidentals),
            qualityLabel = quality.symbol,
            onStepRoot = { rootPc = stepRoot(rootPc, it) },
            onStepQuality = { qualityName = stepQuality(quality, it).name },
            onOpen = { pickerOpen = true },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                FilterChip(
                    selected = mode == MODE_SHAPE,
                    onClick = { mode = MODE_SHAPE },
                    label = { Text(stringResource(Res.string.chords_mode_shape)) },
                )
            }
            item {
                FilterChip(
                    selected = arpeggioMode,
                    onClick = { mode = MODE_ARPEGGIO },
                    label = { Text(stringResource(Res.string.chords_mode_arpeggio)) },
                )
            }
            item {
                FilterChip(
                    selected = positionsMode,
                    onClick = { mode = MODE_POSITIONS },
                    label = { Text(stringResource(Res.string.chords_mode_positions)) },
                )
            }
            if (positionsMode) {
                item { Divider() }
                item {
                    FilterChip(
                        selected = showFingers,
                        onClick = { showFingers = !showFingers },
                        label = { Text(stringResource(Res.string.chords_fingers_toggle)) },
                    )
                }
            }
            if (mode == MODE_SHAPE) {
                item { Divider() }
                items(count) { index ->
                    FilterChip(
                        selected = index == inversion,
                        onClick = { inversionChoice = index },
                        label = { Text(inversionName(index, short = true)) },
                    )
                }
                items(sets) { first ->
                    FilterChip(
                        selected = first == start,
                        onClick = { setChoice = first },
                        label = { Text(stringResource(Res.string.chords_strings_chip, stringsText(instrument, first, count))) },
                    )
                }
                if (voicings.size > 1) {
                    items(voicings.size) { index ->
                        FilterChip(
                            selected = index == voicingIndex,
                            onClick = { voicingChoice = index },
                            label = { Text(stringResource(Res.string.chords_position_chip, index + 1)) },
                        )
                    }
                }
            }
        }
        ChordInfo(
            chord = chord,
            settings = settings,
            mode = mode,
            voicing = voicing,
            inversion = inversion,
            start = start,
            arpeggioNoteCount = arpeggioNotes.size,
            position = position,
            onPlayChord = when {
                !settings.soundEnabled -> null
                positionsMode -> position?.let { p ->
                    { notes.playSequence(p.pitches, instrument.timbre, intervalMillis = STRUM_MS, gain = 0.85f / sqrt(p.pitches.size.toFloat())) }
                }
                !arpeggioMode && voicing != null -> {
                    { notes.playSequence(voicing.pitches, instrument.timbre, intervalMillis = STRUM_MS, gain = 0.85f / sqrt(count.toFloat())) }
                }
                else -> null
            },
            onPlayArpeggio = when {
                !settings.soundEnabled -> null
                positionsMode -> position?.let { p -> { notes.playSequence(p.pitches, instrument.timbre, intervalMillis = VOICING_ARPEGGIO_MS) } }
                arpeggioMode -> if (arpeggioNotes.isNotEmpty()) {
                    { notes.playSequence(Voicings.ascendingPitches(board, arpeggioNotes), instrument.timbre, intervalMillis = ARPEGGIO_MS) }
                } else {
                    null
                }
                voicing != null -> {
                    { notes.playSequence(voicing.pitches, instrument.timbre, intervalMillis = VOICING_ARPEGGIO_MS) }
                }
                else -> null
            },
        )

        // Le dictionnaire : un petit diagramme par position, la choisie encadrée.
        if (positionsMode && positions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(positions.size) { index ->
                    ChordDiagramCard(
                        position = positions[index],
                        selected = index == positionIndex,
                        leftHanded = settings.leftHanded,
                        description = stringResource(Res.string.chords_position_chip, index + 1),
                        onClick = { positionChoice = index },
                    )
                }
            }
        }

        FretboardView(
            instrument = instrument,
            zone = shownZone,
            notation = settings.notation,
            accidentals = settings.accidentals,
            leftHanded = settings.leftHanded,
            highlightedPitchClass = null,
            showNames = false,
            selected = null,
            // Toucher une case fait entendre sa note, comme sur l'onglet Manche.
            onSelect = { touched -> if (settings.soundEnabled) notes.play(board.pitchAt(touched), instrument.timbre) },
            markers = markers,
            bars = bars,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp),
        )
    }

    SetupSheetHost(open = setupOpen, onDismiss = { setupOpen = false }, settings = settings, onChange = onChange)
    RootAndTypeSheet(
        open = pickerOpen,
        onDismiss = { pickerOpen = false },
        rootPc = rootPc,
        onRoot = { rootPc = it },
        notation = settings.notation,
        accidentals = settings.accidentals,
        typeTitle = stringResource(Res.string.chords_sheet_type),
        types = ChordQuality.entries,
        selectedType = quality,
        typeLabel = { it.symbol },
        onType = { qualityName = it.name },
        namesToggle = stringResource(Res.string.chords_names_toggle),
        showNames = showNames,
        onShowNames = { showNames = it },
    )
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/**
 * Les pastilles d'une position du dictionnaire : une croix sur chaque corde étouffée, et sur les autres une pastille de la
 * couleur de son rôle, qui porte le numéro du doigt (ou le rôle, ou le nom de la note, selon les interrupteurs).
 */
private fun positionMarkers(position: ChordPosition, quality: ChordQuality, showNames: Boolean, showFingers: Boolean): List<NeckMarker> =
    position.frets.mapIndexed { string, fret ->
        if (fret == null) {
            NeckMarker(FretPosition(string, 0), MarkerKind.MUTED)
        } else {
            val role = position.roles[string] ?: 0
            val finger = position.fingers[string]
            val label = when {
                showFingers && finger != null -> finger.toString()
                showFingers && fret == 0 -> "0" // une corde à vide : aucun doigt, comme dans une tablature
                showNames -> null
                else -> quality.roleLabels[role]
            }
            NeckMarker(FretPosition(string, fret), kindOf(role), label)
        }
    }

@Composable
private fun ChordInfo(
    chord: Chord,
    settings: AppSettings,
    mode: Int,
    voicing: Voicing?,
    inversion: Int,
    start: Int,
    arpeggioNoteCount: Int,
    position: ChordPosition?,
    onPlayChord: (() -> Unit)?,
    onPlayArpeggio: (() -> Unit)?,
) {
    val name = NoteNames.label(chord.root, settings.notation, settings.accidentals)
    val tones = chord.pitchClasses.mapIndexed { role, pitchClass ->
        "${chord.quality.roleLabels[role]} ${NoteNames.label(pitchClass, settings.notation, settings.accidentals)}"
    }.joinToString("  ·  ")

    InfoCard(
        title = stringResource(Res.string.chords_title, name, chord.quality.displayName()),
        actions = {
            if (onPlayChord != null) PlayButton(stringResource(Res.string.chords_play_chord), onPlayChord)
            if (onPlayArpeggio != null) PlayButton(stringResource(Res.string.chords_play_arpeggio), onPlayArpeggio)
        },
    ) {
        Text(tones, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium)
        Text(
            when {
                mode == MODE_ARPEGGIO -> stringResource(Res.string.chords_arpeggio_info, arpeggioNoteCount)
                mode == MODE_POSITIONS -> position?.let { positionDescription(it) } ?: stringResource(Res.string.chords_no_position)
                voicing == null -> stringResource(Res.string.chords_no_shape)
                else -> stringResource(
                    Res.string.chords_voicing_info,
                    inversionName(inversion, short = false),
                    stringsText(settings.instrument, start, chord.quality.toneCount),
                    if (voicing.minFret == voicing.maxFret) {
                        stringResource(Res.string.chords_fret_single, voicing.minFret)
                    } else {
                        stringResource(Res.string.chords_frets_range, voicing.minFret, voicing.maxFret)
                    },
                )
            },
            Modifier.padding(end = 12.dp, top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** « Accord ouvert · x 3 2 0 1 0 » : ce que c'est, puis la position écrite comme sur une tablature (x : corde étouffée). */
@Composable
private fun positionDescription(position: ChordPosition): String {
    val kind = when {
        position.usesOpenStrings -> stringResource(Res.string.chords_kind_open)
        position.barre != null -> stringResource(Res.string.chords_kind_barre, position.lowestFret)
        else -> stringResource(Res.string.chords_kind_movable, position.lowestFret)
    }
    return stringResource(Res.string.chords_position_info, kind, position.tab)
}

/** La barre d'accord : la fondamentale et la famille, chacune avec ses flèches ; toucher un nom ouvre la feuille de choix. */
@Composable
private fun ChordBar(rootLabel: String, qualityLabel: String, onStepRoot: (Int) -> Unit, onStepQuality: (Int) -> Unit, onOpen: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stepper(
            rootLabel,
            stringResource(Res.string.chords_root_previous),
            stringResource(Res.string.chords_root_next),
            onStepRoot,
            onOpen,
            Modifier.weight(1f),
        )
        Stepper(
            qualityLabel,
            stringResource(Res.string.chords_type_previous),
            stringResource(Res.string.chords_type_next),
            onStepQuality,
            onOpen,
            Modifier.weight(1f),
        )
    }
}

/** Écart entre les cordes d'un accord « gratté », en millisecondes. */
private const val STRUM_MS = 35L

/** Écart entre les notes d'un arpège sur la forme choisie, et d'un arpège sur toute la zone. */
private const val VOICING_ARPEGGIO_MS = 330L
private const val ARPEGGIO_MS = 240L
