package com.sensitivemobility.neckatlas.feature.fretboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sensitivemobility.neckatlas.audio.NotePlayer
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.feature.setup.SetupSheetHost
import com.sensitivemobility.neckatlas.feature.setup.SetupSummary
import com.sensitivemobility.neckatlas.feature.tuner.TunerScreen
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.info_details
import com.sensitivemobility.neckatlas.shared.resources.info_details_open
import com.sensitivemobility.neckatlas.shared.resources.info_hint
import com.sensitivemobility.neckatlas.shared.resources.notes_show_names
import com.sensitivemobility.neckatlas.shared.resources.play_open_strings
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val NONE = -1

// Un emplacement tient dans un entier pour survivre à une rotation de l'écran : corde * 100 + case.
private fun FretPosition.encode(): Int = string * 100 + fret
private fun decodePosition(code: Int): FretPosition? = if (code < 0) null else FretPosition(code / 100, code % 100)

/**
 * L'explorateur de manche, pensé pour tenir sur un écran de téléphone en hauteur : un résumé de l'instrument et de la zone
 * (qui ouvre leurs réglages), la rangée des notes à éclairer, la note touchée, puis le manche debout qui prend toute la
 * place restante.
 *
 * @param onChange applique une modification aux réglages (qui sont enregistrés sur le téléphone).
 */
@Composable
fun FretboardScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, notes: NotePlayer) {
    val instrument = settings.instrument
    val zone = settings.zone
    var highlighted by rememberSaveable { mutableIntStateOf(NONE) }
    // Lié à l'instrument : un emplacement choisi sur une autre guitare ne désignerait plus la même note.
    var selectedCode by rememberSaveable(instrument) { mutableIntStateOf(NONE) }
    var showNames by rememberSaveable { mutableStateOf(false) }
    var setupOpen by rememberSaveable { mutableStateOf(false) }

    var tunerOpen by rememberSaveable { mutableStateOf(false) }

    val board = remember(instrument) { Fretboard(instrument) }
    // Un emplacement hors de la zone n'est pas montré (il revient si l'on élargit la zone).
    val selected = decodePosition(selectedCode)?.takeIf { it.fret in zone }

    if (tunerOpen) {
        TunerScreen(settings, onChange, notes, onClose = { tunerOpen = false })
        return
    }

    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SetupSummary(instrument, zone, onClick = { setupOpen = true })
        NoteChips(settings, highlighted, showNames, onHighlight = { highlighted = it }, onShowNames = { showNames = it })
        NoteInfo(
            selected = selected,
            board = board,
            settings = settings,
            onPlayOpenStrings = if (settings.soundEnabled) {
                { notes.playSequence(instrument.openStrings, instrument.timbre) }
            } else {
                null
            },
        )
        FretboardView(
            instrument = instrument,
            zone = zone,
            notation = settings.notation,
            accidentals = settings.accidentals,
            leftHanded = settings.leftHanded,
            highlightedPitchClass = highlighted.takeIf { it != NONE },
            showNames = showNames,
            selected = selected,
            onSelect = { position ->
                selectedCode = position.encode()
                if (settings.soundEnabled) notes.play(board.pitchAt(position), instrument.timbre)
            },
            modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp),
        )
    }

    SetupSheetHost(open = setupOpen, onDismiss = { setupOpen = false }, settings = settings, onChange = onChange, onTuner = { tunerOpen = true })
}

/** Les douze notes à éclairer sur le manche, plus un bouton qui affiche le nom de toutes les notes. */
@Composable
private fun NoteChips(
    settings: AppSettings,
    highlighted: Int,
    showNames: Boolean,
    onHighlight: (Int) -> Unit,
    onShowNames: (Boolean) -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = showNames,
                onClick = { onShowNames(!showNames) },
                label = { Text(stringResource(Res.string.notes_show_names)) },
            )
        }
        items(12) { pitchClass ->
            FilterChip(
                selected = highlighted == pitchClass,
                onClick = { onHighlight(if (highlighted == pitchClass) NONE else pitchClass) },
                label = { Text(NoteNames.label(pitchClass, settings.notation, settings.accidentals)) },
            )
        }
    }
}

@Composable
private fun NoteInfo(selected: FretPosition?, board: Fretboard, settings: AppSettings, onPlayOpenStrings: (() -> Unit)?) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected == null) {
                Text(
                    stringResource(Res.string.info_hint),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    NoteNames.label(board.pitchAt(selected).pitchClass, settings.notation, settings.accidentals),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (onPlayOpenStrings != null) {
                TextButton(onClick = onPlayOpenStrings) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.play_open_strings))
                }
            }
        }
        if (selected != null) {
            val pitch = board.pitchAt(selected)
            val name = NoteNames.label(pitch, settings.notation, settings.accidentals)
            val stringNumber = board.instrument.stringNumber(selected.string)
            Text(
                if (selected.fret == 0) {
                    stringResource(Res.string.info_details_open, name, pitch.frequencyHz.roundToInt(), stringNumber)
                } else {
                    stringResource(Res.string.info_details, name, pitch.frequencyHz.roundToInt(), stringNumber, selected.fret)
                },
                Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
