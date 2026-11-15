package com.sensitivemobility.neckatlas.feature.scales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.scale.Scale
import com.sensitivemobility.neckatlas.domain.scale.ScalePositions
import com.sensitivemobility.neckatlas.domain.scale.ScaleType
import com.sensitivemobility.neckatlas.feature.chords.stepRoot
import com.sensitivemobility.neckatlas.feature.fretboard.FretboardView
import com.sensitivemobility.neckatlas.feature.fretboard.MarkerKind
import com.sensitivemobility.neckatlas.feature.fretboard.NeckMarker
import com.sensitivemobility.neckatlas.feature.setup.SetupSheetHost
import com.sensitivemobility.neckatlas.feature.setup.SetupSummary
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.chords_frets_range
import com.sensitivemobility.neckatlas.shared.resources.chords_position_chip
import com.sensitivemobility.neckatlas.shared.resources.chords_root_next
import com.sensitivemobility.neckatlas.shared.resources.chords_root_previous
import com.sensitivemobility.neckatlas.shared.resources.scales_all
import com.sensitivemobility.neckatlas.shared.resources.scales_info_all
import com.sensitivemobility.neckatlas.shared.resources.scales_info_position
import com.sensitivemobility.neckatlas.shared.resources.scales_names_toggle
import com.sensitivemobility.neckatlas.shared.resources.scales_play
import com.sensitivemobility.neckatlas.shared.resources.scales_sheet_type
import com.sensitivemobility.neckatlas.shared.resources.scales_title
import com.sensitivemobility.neckatlas.shared.resources.scales_type_next
import com.sensitivemobility.neckatlas.shared.resources.scales_type_previous
import com.sensitivemobility.neckatlas.ui.InfoCard
import com.sensitivemobility.neckatlas.ui.PlayButton
import com.sensitivemobility.neckatlas.ui.RootAndTypeSheet
import com.sensitivemobility.neckatlas.ui.Stepper
import com.sensitivemobility.neckatlas.ui.displayName
import org.jetbrains.compose.resources.stringResource

/**
 * L'onglet Gammes, le dictionnaire de gammes : on choisit une fondamentale et une gamme (majeure, modes, pentatoniques, blues…),
 * et le manche montre ses notes, chacune avec son degré (1, ♭3, 5…), la fondamentale en ambre. On les voit toutes dans la zone
 * de travail, ou une **position** à la fois : quatre cases, sans bouger la main, comme on apprend une gamme au manche.
 * Toucher une case fait entendre sa note ; « Jouer la gamme » la fait monter.
 */
@Composable
fun ScalesScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, notes: NotePlayer) {
    val instrument = settings.instrument
    val zone = settings.zone
    var rootPc by rememberSaveable { mutableIntStateOf(0) }
    var typeName by rememberSaveable { mutableStateOf(ScaleType.MAJOR.name) }
    // -1 : toute la zone ; sinon le rang de la position.
    var positionChoice by rememberSaveable { mutableIntStateOf(-1) }
    var showNames by rememberSaveable { mutableStateOf(false) }
    var setupOpen by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    val type = ScaleType.entries.firstOrNull { it.name == typeName } ?: ScaleType.MAJOR
    val scale = Scale(rootPc, type)
    val board = remember(instrument) { Fretboard(instrument) }
    val positions = remember(board, scale) { ScalePositions.find(board, scale) }
    val positionIndex = clampPosition(positionChoice, positions.size)
    val position = positions.getOrNull(positionIndex)

    // Vue d'ensemble : la zone de travail. Sur une position : ses cases, avec une case de chaque côté pour situer la main.
    val shownZone = remember(zone, position) {
        if (position == null) {
            zone
        } else {
            FretZone(
                first = if (position.firstFret <= 1) 0 else position.firstFret - 1,
                last = (position.lastFret + 1).coerceAtMost(FretZone.MAX_FRET),
            )
        }
    }
    val shown = remember(board, scale, shownZone) { ScalePositions.allNotes(board, shownZone, scale) }
    val markers = remember(shown, position, showNames, type) {
        shown.map { note ->
            val inside = position == null || note.position.fret in position.firstFret..position.lastFret
            NeckMarker(
                position = note.position,
                kind = if (note.degree == 0) MarkerKind.ROOT else MarkerKind.SCALE,
                label = if (showNames) null else type.degrees[note.degree],
                faded = !inside,
            )
        }
    }

    // La gamme qui monte : une octave depuis la fondamentale la plus grave de la zone ; pour une position, tout son dessin.
    val sequence = remember(board, shown, position) {
        if (position == null) {
            ScalePositions.ascending(board, shown)
        } else {
            shown.filter { it.position.fret in position.firstFret..position.lastFret }
                .map { board.pitchAt(it.position) }
                .distinctBy { it.midi }
                .sortedBy { it.midi }
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SetupSummary(instrument, zone, onClick = { setupOpen = true })

        Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper(
                NoteNames.label(rootPc, settings.notation, settings.accidentals),
                stringResource(Res.string.chords_root_previous),
                stringResource(Res.string.chords_root_next),
                onStep = { rootPc = stepRoot(rootPc, it) },
                onOpen = { pickerOpen = true },
                modifier = Modifier.weight(1f),
            )
            Stepper(
                type.displayName().replaceFirstChar { it.uppercase() },
                stringResource(Res.string.scales_type_previous),
                stringResource(Res.string.scales_type_next),
                onStep = { typeName = stepScaleType(type, it).name },
                onOpen = { pickerOpen = true },
                modifier = Modifier.weight(2f),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                FilterChip(
                    selected = positionIndex < 0,
                    onClick = { positionChoice = -1 },
                    label = { Text(stringResource(Res.string.scales_all)) },
                )
            }
            items(positions.size) { index ->
                FilterChip(
                    selected = index == positionIndex,
                    onClick = { positionChoice = index },
                    label = { Text(stringResource(Res.string.chords_position_chip, index + 1)) },
                )
            }
        }

        InfoCard(
            title = stringResource(Res.string.scales_title, NoteNames.label(rootPc, settings.notation, settings.accidentals), type.displayName()),
            actions = {
                if (settings.soundEnabled && sequence.isNotEmpty()) {
                    PlayButton(stringResource(Res.string.scales_play)) {
                        notes.playSequence(sequence, instrument.timbre, intervalMillis = if (position == null) SCALE_MS else RUN_MS)
                    }
                }
            },
        ) {
            Text(
                scale.pitchClasses.joinToString("  ·  ") { NoteNames.label(it, settings.notation, settings.accidentals) },
                Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (position == null) {
                    stringResource(Res.string.scales_info_all, type.noteCount)
                } else {
                    stringResource(
                        Res.string.scales_info_position,
                        positionIndex + 1,
                        positions.size,
                        stringResource(Res.string.chords_frets_range, position.firstFret, position.lastFret),
                    )
                },
                Modifier.padding(end = 12.dp, top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        typeTitle = stringResource(Res.string.scales_sheet_type),
        types = ScaleType.entries,
        selectedType = type,
        typeLabel = { it.displayName().replaceFirstChar { c -> c.uppercase() } },
        onType = { typeName = it.name },
        namesToggle = stringResource(Res.string.scales_names_toggle),
        showNames = showNames,
        onShowNames = { showNames = it },
    )
}

/** Écart entre les notes d'une gamme sur une octave, et d'une position entière, en millisecondes. */
private const val SCALE_MS = 260L
private const val RUN_MS = 190L
