package com.sensitivemobility.neckatlas.feature.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.quiz.ChordPool
import com.sensitivemobility.neckatlas.domain.quiz.ChordQuestionGenerator
import com.sensitivemobility.neckatlas.domain.quiz.ChordSpec
import com.sensitivemobility.neckatlas.domain.quiz.NaturalPitchClasses
import com.sensitivemobility.neckatlas.domain.quiz.Question
import com.sensitivemobility.neckatlas.domain.quiz.QuizConfig
import com.sensitivemobility.neckatlas.domain.quiz.QuizLength
import com.sensitivemobility.neckatlas.domain.quiz.QuizMode
import com.sensitivemobility.neckatlas.domain.quiz.QuizStats
import com.sensitivemobility.neckatlas.domain.quiz.SessionEntry
import com.sensitivemobility.neckatlas.feature.fretboard.FretboardView
import com.sensitivemobility.neckatlas.feature.fretboard.MarkerKind
import com.sensitivemobility.neckatlas.feature.fretboard.NeckMarker
import com.sensitivemobility.neckatlas.feature.setup.SetupSheetHost
import com.sensitivemobility.neckatlas.feature.setup.SetupSummary
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.chords_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_adaptive_hint
import com.sensitivemobility.neckatlas.shared.resources.quiz_adaptive_hint_chords
import com.sensitivemobility.neckatlas.shared.resources.quiz_again
import com.sensitivemobility.neckatlas.shared.resources.quiz_chord_unavailable
import com.sensitivemobility.neckatlas.shared.resources.quiz_chords_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_chords_root_only
import com.sensitivemobility.neckatlas.shared.resources.quiz_chords_with_inversions
import com.sensitivemobility.neckatlas.shared.resources.quiz_done
import com.sensitivemobility.neckatlas.shared.resources.quiz_last_score
import com.sensitivemobility.neckatlas.shared.resources.quiz_exercise_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_feedback_correct
import com.sensitivemobility.neckatlas.shared.resources.quiz_feedback_wrong
import com.sensitivemobility.neckatlas.shared.resources.quiz_length_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_notes_all
import com.sensitivemobility.neckatlas.shared.resources.quiz_notes_naturals
import com.sensitivemobility.neckatlas.shared.resources.quiz_notes_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_progress
import com.sensitivemobility.neckatlas.shared.resources.quiz_question_ear_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_question_find_anywhere
import com.sensitivemobility.neckatlas.shared.resources.quiz_question_find_on_string
import com.sensitivemobility.neckatlas.shared.resources.quiz_question_name_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_question_name_note
import com.sensitivemobility.neckatlas.shared.resources.quiz_question_place_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_replay
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_accuracy
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_average
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_duration
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_missed
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_perfect
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_score
import com.sensitivemobility.neckatlas.shared.resources.quiz_result_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_roots_title
import com.sensitivemobility.neckatlas.shared.resources.quiz_start
import com.sensitivemobility.neckatlas.shared.resources.quiz_stop
import com.sensitivemobility.neckatlas.shared.resources.quiz_string_chip
import com.sensitivemobility.neckatlas.shared.resources.quiz_strings_all
import com.sensitivemobility.neckatlas.shared.resources.quiz_strings_title
import com.sensitivemobility.neckatlas.ui.CorrectColor
import com.sensitivemobility.neckatlas.ui.LocalAccentColors
import com.sensitivemobility.neckatlas.ui.OnCorrectColor
import com.sensitivemobility.neckatlas.ui.ScoreRing
import com.sensitivemobility.neckatlas.ui.Section
import com.sensitivemobility.neckatlas.ui.accuracyColor
import com.sensitivemobility.neckatlas.ui.clockText
import com.sensitivemobility.neckatlas.ui.description
import com.sensitivemobility.neckatlas.ui.displayName
import com.sensitivemobility.neckatlas.ui.inversionName
import com.sensitivemobility.neckatlas.ui.label
import com.sensitivemobility.neckatlas.ui.percentText
import com.sensitivemobility.neckatlas.ui.secondsText
import com.sensitivemobility.neckatlas.ui.stringsText
import org.jetbrains.compose.resources.stringResource

/**
 * L'onglet Quiz : le réglage de la série, puis les questions sur le manche, puis le bilan. L'état de la série est dans
 * [controller], qui survit aux changements d'onglet.
 */
@Composable
fun QuizScreen(
    controller: QuizController,
    settings: AppSettings,
    stats: QuizStats,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
) {
    when (val phase = controller.phase) {
        QuizPhase.Setup -> QuizSetupContent(controller, settings, stats, onChange)
        is QuizPhase.Running -> QuizRunningContent(controller, phase, settings)
        is QuizPhase.Done -> QuizResultContent(controller, phase.result, settings, stats)
    }
}

// --- Réglage de la série -----------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizSetupContent(
    controller: QuizController,
    settings: AppSettings,
    stats: QuizStats,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
) {
    val config = settings.quiz
    val instrument = settings.instrument
    val isChord = config.mode.isChord
    var setupOpen by rememberSaveable { mutableStateOf(false) }
    val update: (QuizConfig) -> Unit = { new -> onChange { it.copy(quiz = new) } }
    // Après un changement d'instrument, des indices de cordes retenus peuvent ne plus exister : on ne compte que les autres.
    val picked = config.strings.filter { it < instrument.stringCount }.toSet()
    // Une zone trop petite ne contient aucune forme d'accord : pas de série possible.
    val shapesAvailable = remember(instrument, settings.zone, config) {
        !config.mode.isChord || ChordQuestionGenerator(Fretboard(instrument), settings.zone, config).available
    }

    // Le bouton « Commencer » reste hors du défilement : toujours sous le pouce, quelle que soit la longueur du réglage.
    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SetupSummary(instrument, settings.zone, onClick = { setupOpen = true })

            Section(stringResource(Res.string.quiz_exercise_title)) {
                // Six exercices, deux par rangée : une carte chacun, avec le dernier score obtenu.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    QuizMode.entries.forEach { mode ->
                        ExerciseCard(
                            mode = mode,
                            selected = mode == config.mode,
                            last = stats.history.lastOrNull { it.mode == mode },
                            onClick = { update(config.copy(mode = mode)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(config.mode.description(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // « Trouver n'importe où » et les accords ne se limitent pas à des cordes.
            if (!isChord && config.mode != QuizMode.FIND_ANYWHERE) {
                Section(stringResource(Res.string.quiz_strings_title)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = picked.isEmpty(),
                            onClick = { update(config.copy(strings = emptySet())) },
                            label = { Text(stringResource(Res.string.quiz_strings_all)) },
                        )
                        for (string in 0 until instrument.stringCount) {
                            val isPicked = string in picked
                            FilterChip(
                                selected = isPicked,
                                onClick = {
                                    val next = if (isPicked) picked - string else picked + string
                                    // Toutes les cordes cochées, c'est « toutes ».
                                    update(config.copy(strings = if (next.size >= instrument.stringCount) emptySet() else next))
                                },
                                label = {
                                    val open = NoteNames.label(instrument.openStrings[string].pitchClass, settings.notation, settings.accidentals)
                                    Text(stringResource(Res.string.quiz_string_chip, instrument.stringNumber(string), open))
                                },
                            )
                        }
                    }
                }
            }

            Section(stringResource(if (isChord) Res.string.quiz_roots_title else Res.string.quiz_notes_title)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = config.naturalsOnly,
                        onClick = { update(config.copy(naturalsOnly = true)) },
                        label = { Text(stringResource(Res.string.quiz_notes_naturals)) },
                    )
                    FilterChip(
                        selected = !config.naturalsOnly,
                        onClick = { update(config.copy(naturalsOnly = false)) },
                        label = { Text(stringResource(Res.string.quiz_notes_all)) },
                    )
                }
            }

            if (isChord) {
                Section(stringResource(Res.string.quiz_chords_title)) {
                    // Deux choix indépendants, donc deux rangées : le type d'accords, puis les renversements.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChordPool.entries.forEach { pool ->
                            FilterChip(
                                selected = pool == config.chordPool,
                                onClick = { update(config.copy(chordPool = pool)) },
                                label = { Text(pool.label()) },
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !config.chordInversions,
                            onClick = { update(config.copy(chordInversions = false)) },
                            label = { Text(stringResource(Res.string.quiz_chords_root_only)) },
                        )
                        FilterChip(
                            selected = config.chordInversions,
                            onClick = { update(config.copy(chordInversions = true)) },
                            label = { Text(stringResource(Res.string.quiz_chords_with_inversions)) },
                        )
                    }
                }
            }

            Section(stringResource(Res.string.quiz_length_title)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizLength.entries.forEach { length ->
                        FilterChip(
                            selected = length == config.length,
                            onClick = { update(config.copy(length = length)) },
                            label = { Text(length.label()) },
                        )
                    }
                }
            }

        }
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(
                    when {
                        !shapesAvailable -> Res.string.quiz_chord_unavailable
                        isChord -> Res.string.quiz_adaptive_hint_chords
                        else -> Res.string.quiz_adaptive_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (shapesAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
            Button(
                onClick = { controller.start(QuizSetup(instrument, settings.zone, config.copy(strings = picked), settings.soundEnabled), stats) },
                Modifier.fillMaxWidth().heightIn(min = 52.dp),
                enabled = shapesAvailable,
            ) {
                Text(stringResource(Res.string.quiz_start), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    SetupSheetHost(open = setupOpen, onDismiss = { setupOpen = false }, settings = settings, onChange = onChange)
}

private fun QuizMode.icon(): ImageVector = when (this) {
    QuizMode.FIND_ON_STRING -> Icons.Filled.LinearScale
    QuizMode.FIND_ANYWHERE -> Icons.Filled.Search
    QuizMode.NAME_NOTE -> Icons.Filled.TextFields
    QuizMode.NAME_CHORD -> Icons.Filled.LibraryMusic
    QuizMode.PLACE_CHORD -> Icons.Filled.TouchApp
    QuizMode.EAR_CHORD -> Icons.Filled.Hearing
}

/** Un exercice : son icône, son nom et, s'il a déjà été joué, le score de la dernière série. Cerclé de la couleur d'accent quand il est choisi. */
@Composable
private fun ExerciseCard(mode: QuizMode, selected: Boolean, last: SessionEntry?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(mode.icon(), contentDescription = null, Modifier.size(24.dp), tint = if (selected) colors.primary else colors.onSurfaceVariant)
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    mode.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 2,
                )
                // Le score n'apparaît qu'une fois l'exercice joué.
                if (last != null) {
                    Text(
                        stringResource(Res.string.quiz_last_score, last.correct, last.answered),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
// --- Question en cours -------------------------------------------------------------------------------------------------

private fun roleKind(role: Int): MarkerKind = when (role) {
    0 -> MarkerKind.ROOT
    1 -> MarkerKind.THIRD
    2 -> MarkerKind.FIFTH
    else -> MarkerKind.SEVENTH
}

/** Les cases d'une forme d'accord (de la corde la plus grave à la plus aiguë), colorées et nommées selon leur rôle. */
private fun roleMarkers(spec: ChordSpec, positions: List<FretPosition>): List<NeckMarker> {
    val labels = spec.chord.quality.roleLabels
    return positions.mapIndexed { index, position ->
        val role = (spec.inversion + index) % labels.size
        NeckMarker(position, roleKind(role), labels[role])
    }
}

private fun markersFor(question: Question, feedback: Feedback?, picked: List<FretPosition>): List<NeckMarker> = when (question) {
    is Question.Name ->
        listOf(NeckMarker(question.position, if (feedback == null) MarkerKind.TARGET else MarkerKind.CORRECT))
    is Question.Find -> when {
        feedback == null -> emptyList()
        feedback.correct -> listOfNotNull(feedback.chosenPosition?.let { NeckMarker(it, MarkerKind.CORRECT) })
        else -> listOfNotNull(feedback.chosenPosition?.let { NeckMarker(it, MarkerKind.WRONG) }) +
            feedback.correctPositions.map { NeckMarker(it, MarkerKind.CORRECT) }
    }
    // La forme montrée reste anonyme (étiquette vide) jusqu'à la réponse, puis chaque note révèle son rôle.
    is Question.NameChord ->
        if (feedback == null) question.positions.map { NeckMarker(it, MarkerKind.TARGET, label = "") } else roleMarkers(question.spec, question.positions)
    is Question.EarChord -> if (feedback == null) emptyList() else roleMarkers(question.spec, question.positions)
    is Question.PlaceChord ->
        if (feedback == null) {
            picked.map { NeckMarker(it, MarkerKind.PICKED) }
        } else {
            // Chaque case touchée, juste ou fausse ; et, sur les cordes ratées, la note qu'il fallait jouer.
            val wrongStrings = feedback.placement.filter { !it.second }.map { it.first.string }.toSet()
            feedback.placement.map { (position, right) -> NeckMarker(position, if (right) MarkerKind.CORRECT else MarkerKind.WRONG) } +
                roleMarkers(question.spec, feedback.correctPositions).filter { it.position.string in wrongStrings }
        }
}

private fun emphasizedStringsFor(question: Question): Set<Int> = when (question) {
    is Question.Find -> setOfNotNull(question.string)
    is Question.PlaceChord -> (question.spec.start until question.spec.start + question.spec.chord.quality.toneCount).toSet()
    else -> emptySet()
}

@Composable
private fun QuizRunningContent(controller: QuizController, phase: QuizPhase.Running, settings: AppSettings) {
    val setup = phase.setup
    val question = phase.question
    val feedback = phase.feedback
    val markers = remember(phase) { markersFor(question, feedback, phase.picked) }
    val tapsAllowed = (question is Question.Find || question is Question.PlaceChord) && feedback == null

    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuizHeader(phase, controller.remainingMillis, onQuit = controller::quit)
        QuestionCard(phase, settings)
        FretboardView(
            instrument = setup.instrument,
            zone = setup.zone,
            notation = settings.notation,
            accidentals = settings.accidentals,
            leftHanded = settings.leftHanded,
            highlightedPitchClass = null,
            showNames = false,
            selected = null,
            onSelect = { position ->
                if (question is Question.PlaceChord) controller.tapChord(position) else controller.answerPosition(position)
            },
            enabled = tapsAllowed,
            markers = markers,
            emphasizedStrings = emphasizedStringsFor(question),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = if (question is Question.Find || question is Question.PlaceChord) 8.dp else 0.dp),
        )
        when (question) {
            is Question.Name -> NoteAnswers(question, feedback, setup.config.naturalsOnly, settings, onAnswer = controller::answerNote)
            is Question.NameChord -> ChordNameAnswers(question, feedback, setup.config, settings, onAnswer = controller::answerChord)
            is Question.EarChord -> EarAnswers(question, feedback, setup.config, onAnswer = controller::answerQuality, onReplay = controller::replaySound)
            else -> Unit
        }
    }
}

@Composable
private fun QuizHeader(phase: QuizPhase.Running, remainingMillis: Long, onQuit: () -> Unit) {
    val length = phase.setup.config.length
    val total = length.questions
    val fraction = if (total != null) {
        (phase.number - 1 + if (phase.feedback != null) 1 else 0) / total.toFloat()
    } else {
        remainingMillis / ((length.seconds ?: 1) * 1000f)
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (total != null) stringResource(Res.string.quiz_progress, phase.number, total) else clockText(remainingMillis),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp), tint = CorrectColor)
            Text(" ${phase.correctSoFar}", style = MaterialTheme.typography.titleMedium, color = CorrectColor)
            IconButton(onClick = onQuit) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.quiz_stop))
            }
        }
        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun QuestionCard(phase: QuizPhase.Running, settings: AppSettings) {
    val question = phase.question
    val feedback = phase.feedback
    val instrument = phase.setup.instrument
    val colors = MaterialTheme.colorScheme
    val rootName = NoteNames.label(question.pitchClass, settings.notation, settings.accidentals)
    // La réponse en toutes lettres : « Do » pour une note, « Do majeur » pour un accord.
    val answerName = question.chordQuality?.let { stringResource(Res.string.chords_title, rootName, it.displayName()) } ?: rootName
    val (background, textColor) = when {
        feedback == null -> colors.surfaceContainerHigh to colors.onSurface
        feedback.correct -> CorrectColor.copy(alpha = 0.22f) to CorrectColor
        else -> colors.error.copy(alpha = 0.22f) to colors.error
    }
    val text = when {
        feedback == null -> when (question) {
            is Question.Find ->
                if (question.string != null) {
                    val open = NoteNames.label(instrument.openStrings[question.string].pitchClass, settings.notation, settings.accidentals)
                    stringResource(Res.string.quiz_question_find_on_string, rootName, instrument.stringNumber(question.string), open)
                } else {
                    stringResource(Res.string.quiz_question_find_anywhere, rootName)
                }
            is Question.Name -> stringResource(Res.string.quiz_question_name_note)
            is Question.NameChord -> stringResource(Res.string.quiz_question_name_chord)
            is Question.PlaceChord -> stringResource(
                Res.string.quiz_question_place_chord,
                answerName,
                inversionName(question.spec.inversion, short = false),
                stringsText(instrument, question.spec.start, question.spec.chord.quality.toneCount),
            )
            is Question.EarChord -> stringResource(Res.string.quiz_question_ear_chord, rootName)
        }
        feedback.correct -> stringResource(Res.string.quiz_feedback_correct)
        else -> stringResource(Res.string.quiz_feedback_wrong, answerName)
    }
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = textColor)
    }
}

/** Couleurs d'un bouton de réponse : bonne réponse en vert, mauvais choix en rouge, choix en cours en couleur d'accent, sinon neutre. */
private fun answerColors(colors: ColorScheme, isAnswer: Boolean, isWrongChoice: Boolean, isSelected: Boolean): Pair<Color, Color> = when {
    isAnswer -> CorrectColor to OnCorrectColor
    isWrongChoice -> colors.error to colors.onError
    isSelected -> colors.primary to colors.onPrimary
    else -> colors.surfaceContainerHighest to colors.onSurface
}

@Composable
private fun AnswerButton(label: String, colors: Pair<Color, Color>, modifier: Modifier, height: Int = 48, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.first, contentColor = colors.second),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        Text(label, fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Les boutons de réponse de « Nommer la note » : les 7 naturelles, ou les 12 notes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteAnswers(question: Question.Name, feedback: Feedback?, naturalsOnly: Boolean, settings: AppSettings, onAnswer: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Une zone sans aucune note naturelle peut poser une altérée : il faut alors pouvoir la nommer.
    val pitchClasses = if (naturalsOnly && question.pitchClass in NaturalPitchClasses) NaturalPitchClasses else (0..11).toList()
    FlowRow(
        Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4,
    ) {
        pitchClasses.forEach { pitchClass ->
            val isAnswer = feedback != null && pitchClass == question.pitchClass
            val isWrongChoice = feedback != null && !feedback.correct && pitchClass == feedback.chosenPitchClass
            AnswerButton(
                NoteNames.label(pitchClass, settings.notation, settings.accidentals),
                answerColors(colors, isAnswer, isWrongChoice, isSelected = false),
                Modifier.weight(1f),
            ) { onAnswer(pitchClass) }
        }
    }
}

/** Les familles d'accords proposées en réponse : les triades, ou aussi les septièmes. */
private fun answerQualities(config: QuizConfig): List<ChordQuality> =
    ChordQuality.entries.filter { config.chordPool == ChordPool.ALL || !it.isSeventh }

/**
 * Les réponses de « Nommer l'accord » : une fondamentale et un type, dans l'ordre qu'on veut ; la réponse part dès que les
 * deux sont choisis. Le choix en cours est en turquoise et peut être changé tant qu'il manque l'autre moitié.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChordNameAnswers(
    question: Question.NameChord,
    feedback: Feedback?,
    config: QuizConfig,
    settings: AppSettings,
    onAnswer: (Int, ChordQuality) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var root by remember(question) { mutableIntStateOf(-1) }
    var quality by remember(question) { mutableStateOf<ChordQuality?>(null) }
    val roots = if (config.naturalsOnly && question.pitchClass in NaturalPitchClasses) NaturalPitchClasses else (0..11).toList()
    // Quatre par rangée quand les noms doubles (Do♯/Ré♭) sont longs, sinon plus serré.
    val rootsPerRow = if (roots.size == 7 || settings.accidentals == AccidentalStyle.BOTH) 4 else 6
    val qualities = answerQualities(config)

    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Arrangement.spacedBy(6.dp), maxItemsInEachRow = rootsPerRow) {
            roots.forEach { pitchClass ->
                val isAnswer = feedback != null && pitchClass == question.pitchClass
                val isWrongChoice = feedback != null && !feedback.correct && pitchClass == feedback.chosenPitchClass
                AnswerButton(
                    NoteNames.label(pitchClass, settings.notation, settings.accidentals),
                    answerColors(colors, isAnswer, isWrongChoice, isSelected = feedback == null && pitchClass == root),
                    Modifier.weight(1f),
                    height = 40,
                ) {
                    if (feedback != null) return@AnswerButton
                    root = pitchClass
                    quality?.let { onAnswer(pitchClass, it) }
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Arrangement.spacedBy(6.dp), maxItemsInEachRow = if (qualities.size > 4) 5 else 4) {
            qualities.forEach { option ->
                val isAnswer = feedback != null && option == question.chordQuality
                val isWrongChoice = feedback != null && !feedback.correct && option == feedback.chosenQuality
                AnswerButton(
                    option.symbol,
                    answerColors(colors, isAnswer, isWrongChoice, isSelected = feedback == null && option == quality),
                    Modifier.weight(1f),
                    height = 40,
                ) {
                    if (feedback != null) return@AnswerButton
                    quality = option
                    if (root >= 0) onAnswer(root, option)
                }
            }
        }
    }
}

/** Les réponses de « Accord à l'oreille » : le type de l'accord entendu, et de quoi le réécouter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EarAnswers(
    question: Question.EarChord,
    feedback: Feedback?,
    config: QuizConfig,
    onAnswer: (ChordQuality) -> Unit,
    onReplay: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val qualities = answerQualities(config)
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReplay, Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
            Text(stringResource(Res.string.quiz_replay))
        }
        FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Arrangement.spacedBy(6.dp), maxItemsInEachRow = if (qualities.size > 4) 5 else 4) {
            qualities.forEach { option ->
                val isAnswer = feedback != null && option == question.chordQuality
                val isWrongChoice = feedback != null && !feedback.correct && option == feedback.chosenQuality
                AnswerButton(option.symbol, answerColors(colors, isAnswer, isWrongChoice, isSelected = false), Modifier.weight(1f)) {
                    if (feedback == null) onAnswer(option)
                }
            }
        }
    }
}

// --- Bilan -------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizResultContent(controller: QuizController, result: SessionResult, settings: AppSettings, stats: QuizStats) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(Res.string.quiz_result_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // L'anneau se remplit de la part de réponses justes, dans la couleur de la tranche (vert, turquoise, ambre, rouge).
            val accuracy = if (result.answered > 0) result.correct.toFloat() / result.answered else 0f
            val tint = accuracyColor(MaterialTheme.colorScheme, LocalAccentColors.current, accuracy.toDouble()) ?: MaterialTheme.colorScheme.primary
            ScoreRing(accuracy, tint, Modifier.padding(vertical = 8.dp), size = 156.dp, strokeWidth = 12.dp) {
                Text(
                    stringResource(Res.string.quiz_result_score, result.correct, result.answered),
                    style = MaterialTheme.typography.displaySmall,
                    color = tint,
                )
            }
            Text(
                stringResource(Res.string.quiz_result_accuracy, percentText(result.correct, result.answered)),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultRow(stringResource(Res.string.quiz_result_average), secondsText(result.averageMillis))
            ResultRow(stringResource(Res.string.quiz_result_duration), clockText(result.durationMillis))
        }

        Section(stringResource(Res.string.quiz_result_missed)) {
            if (result.missed.isEmpty() && result.missedQualities.isEmpty()) {
                Text(stringResource(Res.string.quiz_result_perfect), style = MaterialTheme.typography.titleMedium, color = CorrectColor)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.missed.forEach { pitchClass ->
                        AssistChip(
                            onClick = {},
                            label = { Text(NoteNames.label(pitchClass, settings.notation, settings.accidentals)) },
                        )
                    }
                    result.missedQualities.forEach { quality ->
                        AssistChip(onClick = {}, label = { Text(quality.symbol) })
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { controller.replay(stats) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(Res.string.quiz_again), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = controller::backToSetup, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.quiz_done))
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
