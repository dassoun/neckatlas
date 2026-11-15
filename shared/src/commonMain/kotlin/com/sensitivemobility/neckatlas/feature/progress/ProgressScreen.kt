package com.sensitivemobility.neckatlas.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.quiz.NoteStat
import com.sensitivemobility.neckatlas.domain.quiz.QuizStats
import com.sensitivemobility.neckatlas.feature.fretboard.FretboardView
import com.sensitivemobility.neckatlas.feature.fretboard.MarkerKind
import com.sensitivemobility.neckatlas.feature.fretboard.NeckIllustration
import com.sensitivemobility.neckatlas.feature.fretboard.NeckMarker
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.cancel
import com.sensitivemobility.neckatlas.shared.resources.history_row
import com.sensitivemobility.neckatlas.shared.resources.progress_by_chord
import com.sensitivemobility.neckatlas.shared.resources.progress_by_note
import com.sensitivemobility.neckatlas.shared.resources.progress_empty
import com.sensitivemobility.neckatlas.shared.resources.progress_empty_cta
import com.sensitivemobility.neckatlas.shared.resources.progress_empty_title
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_cell
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_cell_none
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_cell_open
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_cell_stats
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_empty
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_hint
import com.sensitivemobility.neckatlas.shared.resources.progress_heat_title
import com.sensitivemobility.neckatlas.shared.resources.progress_history
import com.sensitivemobility.neckatlas.shared.resources.progress_reset
import com.sensitivemobility.neckatlas.shared.resources.progress_reset_body
import com.sensitivemobility.neckatlas.shared.resources.progress_reset_confirm
import com.sensitivemobility.neckatlas.shared.resources.progress_reset_title
import com.sensitivemobility.neckatlas.shared.resources.progress_summary
import com.sensitivemobility.neckatlas.shared.resources.progress_weakest
import com.sensitivemobility.neckatlas.shared.resources.progress_weakest_none
import com.sensitivemobility.neckatlas.ui.CorrectColor
import com.sensitivemobility.neckatlas.ui.LocalAccentColors
import com.sensitivemobility.neckatlas.ui.ScoreRing
import com.sensitivemobility.neckatlas.ui.Section
import com.sensitivemobility.neckatlas.ui.accuracyColor
import com.sensitivemobility.neckatlas.ui.clockText
import com.sensitivemobility.neckatlas.ui.dateText
import com.sensitivemobility.neckatlas.ui.label
import com.sensitivemobility.neckatlas.ui.percentText
import com.sensitivemobility.neckatlas.ui.secondsText
import com.sensitivemobility.neckatlas.ui.shortLabel
import org.jetbrains.compose.resources.stringResource

private const val HISTORY_SHOWN = 10

/** L'onglet Progrès : bilan global, résultats note par note, notes à retravailler et dernières séries. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(stats: QuizStats, settings: AppSettings, onReset: () -> Unit, onStartQuiz: () -> Unit) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (stats.answered == 0) {
            EmptyProgress(onStartQuiz)
            return@Column
        }

        ProgressSummary(stats)

        // Les notes n'apparaissent que si elles ont été interrogées ; de même pour les types d'accords.
        if (stats.notes.any { it.attempts > 0 }) {
            Section(stringResource(Res.string.progress_by_note)) {
                (0 until 12).chunked(4).forEach { rowNotes ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowNotes.forEach { pitchClass ->
                            StatCell(
                                NoteNames.label(pitchClass, settings.notation, settings.accidentals),
                                stats.notes[pitchClass],
                                Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (stats.qualities.any { it.attempts > 0 }) {
            Section(stringResource(Res.string.progress_by_chord)) {
                ChordQuality.entries.chunked(3).forEach { rowQualities ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowQualities.forEach { quality ->
                            StatCell(quality.symbol, stats.qualities[quality.ordinal], Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (stats.cells.isNotEmpty()) HeatMap(stats, settings)

        Section(stringResource(Res.string.progress_weakest)) {
            val weakestNotes = stats.weakest()
            val weakestQualities = stats.weakestQualities()
            if (weakestNotes.isEmpty() && weakestQualities.isEmpty()) {
                Text(
                    stringResource(Res.string.progress_weakest_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weakestNotes.forEach { pitchClass ->
                        val accuracy = stats.notes[pitchClass].let { percentText(it.correct, it.attempts) }
                        AssistChip(
                            onClick = {},
                            label = { Text("${NoteNames.label(pitchClass, settings.notation, settings.accidentals)} · $accuracy") },
                        )
                    }
                    weakestQualities.forEach { quality ->
                        val accuracy = stats.qualities[quality.ordinal].let { percentText(it.correct, it.attempts) }
                        AssistChip(onClick = {}, label = { Text("${quality.symbol} · $accuracy") })
                    }
                }
            }
        }

        if (stats.history.isNotEmpty()) {
            Section(stringResource(Res.string.progress_history)) {
                stats.history.asReversed().take(HISTORY_SHOWN).forEach { entry ->
                    Text(
                        stringResource(
                            Res.string.history_row,
                            dateText(entry.epochMillis),
                            entry.mode.shortLabel(),
                            entry.correct,
                            entry.answered,
                            clockText(entry.durationMillis),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        TextButton(onClick = { confirmReset = true }, Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(Res.string.progress_reset), color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(Res.string.progress_reset_title)) },
            text = { Text(stringResource(Res.string.progress_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) {
                    Text(stringResource(Res.string.progress_reset_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

/** Rien à montrer encore : une invitation plutôt qu'un écran vide, avec de quoi lancer un quiz d'un geste. */
@Composable
private fun EmptyProgress(onStartQuiz: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NeckIllustration(Modifier.size(width = 210.dp, height = 240.dp))
        Text(stringResource(Res.string.progress_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(Res.string.progress_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onStartQuiz, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(Res.string.progress_empty_cta), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Le bilan global : un anneau de réussite, de la couleur de la tranche (vert, turquoise, ambre, rouge), et le nombre de réponses. */
@Composable
private fun ProgressSummary(stats: QuizStats) {
    val colors = MaterialTheme.colorScheme
    val accuracy = stats.correct.toDouble() / stats.answered
    val tint = accuracyColor(colors, LocalAccentColors.current, accuracy) ?: colors.primary
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScoreRing(accuracy.toFloat(), tint, size = 92.dp, strokeWidth = 9.dp) {
            Text(percentText(stats.correct, stats.answered), style = MaterialTheme.typography.titleSmall)
        }
        Text(
            stringResource(Res.string.progress_summary, stats.answered, percentText(stats.correct, stats.answered)),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

// Un manche en hauteur : chaque case garde environ cette hauteur, la carte grandit avec le nombre de cases.
private const val HEAT_ROW_DP = 30
private const val HEAT_MIN_FRETS = 12

/**
 * La carte de chaleur : le manche de l'instrument choisi, avec une pastille sur chaque case où le quiz a interrogé, colorée
 * selon la réussite. Une case vide n'a jamais été demandée. Toucher une case en donne le détail.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeatMap(stats: QuizStats, settings: AppSettings) {
    var viewedName by rememberSaveable { mutableStateOf(settings.instrument.name) }
    val instrument = Instrument.entries.firstOrNull { it.name == viewedName } ?: settings.instrument
    var selectedCode by rememberSaveable(instrument) { mutableIntStateOf(-1) }
    val cells = stats.cellStats(instrument)
    val colors = MaterialTheme.colorScheme
    val accents = LocalAccentColors.current

    Section(stringResource(Res.string.progress_heat_title)) {
        Text(
            stringResource(Res.string.progress_heat_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Instrument.entries.forEach { option ->
                FilterChip(
                    selected = option == instrument,
                    onClick = { viewedName = option.name },
                    label = { Text(option.label()) },
                )
            }
        }
        if (cells.isEmpty()) {
            Text(stringResource(Res.string.progress_heat_empty), style = MaterialTheme.typography.bodyMedium)
            return@Section
        }

        val lastFret = maxOf(HEAT_MIN_FRETS, cells.keys.maxOf { it.fret })
        val markers = remember(cells, colors, accents) {
            cells.map { (position, stat) ->
                NeckMarker(position, MarkerKind.HEAT, label = "", tint = accuracyColor(colors, accents, stat.accuracy))
            }
        }
        val selected = if (selectedCode < 0) null else FretPosition(selectedCode / 100, selectedCode % 100)
        FretboardView(
            instrument = instrument,
            zone = FretZone(0, lastFret),
            notation = settings.notation,
            accidentals = settings.accidentals,
            leftHanded = settings.leftHanded,
            highlightedPitchClass = null,
            showNames = false,
            selected = selected,
            onSelect = { selectedCode = it.string * 100 + it.fret },
            markers = markers,
            touchFeedback = false, // une carte à consulter, pas un instrument à jouer : ni vibration de corde ni retour vibré
            modifier = Modifier.fillMaxWidth().height(((lastFret + 1) * HEAT_ROW_DP).dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendDot(CorrectColor, "≥ 90 %")
            LegendDot(accents.teal, "70–89 %")
            LegendDot(colors.tertiary, "50–69 %")
            LegendDot(colors.error, "< 50 %")
        }
        if (selected != null && selected.string < instrument.stringCount) HeatCellDetail(instrument, selected, cells[selected], settings)
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

/** Le détail d'une case touchée sur la carte : sa corde, sa case, sa note, et ce qu'on en sait. */
@Composable
private fun HeatCellDetail(instrument: Instrument, position: FretPosition, stat: NoteStat?, settings: AppSettings) {
    val note = NoteNames.label(Fretboard(instrument).pitchAt(position), settings.notation, settings.accidentals)
    val stringNumber = instrument.stringNumber(position.string)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (position.fret == 0) {
                stringResource(Res.string.progress_heat_cell_open, stringNumber, note)
            } else {
                stringResource(Res.string.progress_heat_cell, stringNumber, position.fret, note)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            if (stat == null) {
                stringResource(Res.string.progress_heat_cell_none)
            } else {
                stringResource(
                    Res.string.progress_heat_cell_stats,
                    percentText(stat.correct, stat.attempts),
                    stat.attempts,
                    secondsText(stat.averageMillis ?: 0L),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Une note ou un type d'accord : son nom, sa réussite et son temps moyen, teintée selon la réussite (vert, turquoise, ambre, rouge). */
@Composable
private fun StatCell(label: String, stat: NoteStat, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val accuracy = stat.accuracy
    val tint = accuracyColor(colors, LocalAccentColors.current, accuracy)
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint?.copy(alpha = 0.22f) ?: colors.surfaceContainerHigh)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, textAlign = TextAlign.Center)
        Text(
            if (accuracy == null) "–" else percentText(stat.correct, stat.attempts),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stat.averageMillis?.let { secondsText(it) } ?: " ",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}
