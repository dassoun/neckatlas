package com.sensitivemobility.neckatlas.feature.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sensitivemobility.neckatlas.audio.NotePlayer
import com.sensitivemobility.neckatlas.data.stats.StatsRepository
import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.quiz.Answer
import com.sensitivemobility.neckatlas.domain.quiz.ChordQuestionGenerator
import com.sensitivemobility.neckatlas.domain.quiz.NoteStat
import com.sensitivemobility.neckatlas.domain.quiz.Question
import com.sensitivemobility.neckatlas.domain.quiz.QuestionGenerator
import com.sensitivemobility.neckatlas.domain.quiz.QuestionSource
import com.sensitivemobility.neckatlas.domain.quiz.QuizConfig
import com.sensitivemobility.neckatlas.domain.quiz.QuizRules
import com.sensitivemobility.neckatlas.domain.quiz.QuizStats
import com.sensitivemobility.neckatlas.domain.quiz.SessionEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Tout ce qu'il faut pour lancer une série : l'instrument et la zone du moment, le réglage du quiz, et le son. */
class QuizSetup(val instrument: Instrument, val zone: FretZone, val config: QuizConfig, val soundEnabled: Boolean)

/** La correction d'une réponse, affichée avant la question suivante. */
class Feedback(
    val correct: Boolean,
    /** Case touchée (exercices « trouver »). */
    val chosenPosition: FretPosition?,
    /** Note choisie (exercice « nommer »), note de la case touchée, ou fondamentale choisie (« nommer l'accord »). */
    val chosenPitchClass: Int,
    /** Les cases qui étaient justes. */
    val correctPositions: List<FretPosition>,
    /** Type d'accord choisi (exercices d'accords à choix). */
    val chosenQuality: ChordQuality? = null,
    /** « Placer l'accord » : chaque case touchée, et si elle était juste. */
    val placement: List<Pair<FretPosition, Boolean>> = emptyList(),
)

/** Le bilan d'une série. */
class SessionResult(
    val setup: QuizSetup,
    val correct: Int,
    val answered: Int,
    val durationMillis: Long,
    val averageMillis: Long,
    /** Notes ratées pendant la série, les plus ratées d'abord. */
    val missed: List<Int>,
    /** Types d'accords ratés (exercices d'accords), les plus ratés d'abord. */
    val missedQualities: List<ChordQuality>,
)

sealed interface QuizPhase {
    /** Le réglage de la série, avant de commencer. */
    data object Setup : QuizPhase

    /**
     * Une question est posée ([feedback] non nul : on montre la correction). [number] : rang de la question, à partir de 1.
     * [picked] : pour « placer l'accord », les cases déjà touchées.
     */
    class Running(
        val setup: QuizSetup,
        val number: Int,
        val question: Question,
        val correctSoFar: Int,
        val feedback: Feedback?,
        val picked: List<FretPosition> = emptyList(),
    ) : QuizPhase

    class Done(val result: SessionResult) : QuizPhase
}

/**
 * Conduit une série de questions : tire les questions, juge les réponses, chronomètre, joue les sons, puis enregistre les
 * résultats. Il vit aussi longtemps que l'appli (au-dessus des onglets), pour qu'un coup d'œil aux réglages ne perde pas une
 * série en cours. À utiliser depuis le fil de l'interface.
 */
class QuizController(
    private val scope: CoroutineScope,
    private val notes: NotePlayer,
    private val stats: StatsRepository,
) {
    var phase: QuizPhase by mutableStateOf(QuizPhase.Setup)
        private set

    /** Temps restant d'une série chronométrée, en millisecondes. */
    var remainingMillis: Long by mutableLongStateOf(0L)
        private set

    private class Run(val setup: QuizSetup, val board: Fretboard) {
        lateinit var generator: QuestionSource
        val misses = IntArray(12)
        val qualityMisses = IntArray(ChordQuality.entries.size)
        val started: TimeMark = TimeSource.Monotonic.markNow()
        val answers = mutableListOf<Answer>()
        var question: Question? = null
        var questionStart: TimeMark = started
        var number = 0
        var correct = 0

        /** Temps écoulé à la dernière réponse : c'est la durée d'une série à nombre de questions fixe. */
        var lastAnswerMillis = 0L
    }

    private var run: Run? = null
    private var timer: Job? = null
    private var advance: Job? = null
    private var lastSetup: QuizSetup? = null

    /**
     * Lance une série. Sans effet si le réglage ne peut donner aucune question (une zone trop petite pour contenir la moindre
     * forme d'accord) : l'écran de réglage le signale avant, voir [ChordQuestionGenerator.available].
     */
    fun start(setup: QuizSetup, quizStats: QuizStats) {
        val board = Fretboard(setup.instrument)
        val newRun = Run(setup, board)
        val config = setup.config
        // Une note ou un type d'accord raté pendant la série revient plus souvent, en plus du poids déjà dans les statistiques.
        val source: QuestionSource = if (config.mode.isChord) {
            ChordQuestionGenerator(
                board,
                setup.zone,
                config,
                weight = { quality -> quizStats.weight(quality) * (1.0 + 1.5 * newRun.qualityMisses[quality.ordinal]) },
            ).also { if (!it.available) return }
        } else {
            QuestionGenerator(
                board,
                setup.zone,
                config,
                weight = { pitchClass -> quizStats.weight(pitchClass) * (1.0 + 1.5 * newRun.misses[pitchClass]) },
                // Les cases où l'on s'est trompé aux séries précédentes (la carte de chaleur) reviennent plus souvent.
                cellFactor = { position -> quizStats.cellFactor(setup.instrument, position) },
            )
        }
        stopJobs()
        lastSetup = setup
        newRun.generator = source
        run = newRun
        config.length.seconds?.let { startTimer(newRun, it * 1000L) }
        nextQuestion()
    }

    /** Rejoue la dernière série avec le même réglage. */
    fun replay(quizStats: QuizStats) {
        lastSetup?.let { start(it, quizStats) }
    }

    /** Réponse aux exercices « trouver » : la case touchée. */
    fun answerPosition(position: FretPosition) {
        val run = run ?: return
        val current = phase as? QuizPhase.Running ?: return
        val question = current.question as? Question.Find ?: return
        if (current.feedback != null) return
        val correct = QuizRules.isCorrect(run.board, run.setup.zone, question, position)
        val correctPositions = if (correct) listOf(position) else QuizRules.correctPositions(run.board, run.setup.zone, question)
        val chosenPitch = run.board.pitchAt(position)
        if (run.setup.soundEnabled) {
            notes.play(chosenPitch, run.setup.instrument.timbre)
            // Après une erreur, on fait entendre la bonne note : l'oreille apprend autant que l'œil.
            if (!correct) correctPositions.firstOrNull()?.let { right ->
                scope.launch {
                    delay(CORRECTION_SOUND_DELAY_MS)
                    notes.play(run.board.pitchAt(right), run.setup.instrument.timbre)
                }
            }
        }
        // Carte de chaleur : « sur une corde » vise une case précise (une réussite crédite la case touchée, une erreur les
        // cases qu'il fallait trouver) ; « n'importe où » n'en vise aucune.
        val cells = if (question.string == null) emptyList() else if (correct) listOf(position) else correctPositions
        submit(run, current, question, correct, Feedback(correct, position, chosenPitch.pitchClass, correctPositions), cells)
    }

    /** Réponse à l'exercice « nommer la note » : la note choisie. */
    fun answerNote(pitchClass: Int) {
        val run = run ?: return
        val current = phase as? QuizPhase.Running ?: return
        val question = current.question as? Question.Name ?: return
        if (current.feedback != null) return
        val correct = QuizRules.isCorrect(question, pitchClass)
        submit(run, current, question, correct, Feedback(correct, null, pitchClass, listOf(question.position)), listOf(question.position))
    }

    /** Réponse à l'exercice « nommer l'accord » : la fondamentale et le type choisis. */
    fun answerChord(root: Int, quality: ChordQuality) {
        val run = run ?: return
        val current = phase as? QuizPhase.Running ?: return
        val question = current.question as? Question.NameChord ?: return
        if (current.feedback != null) return
        val correct = QuizRules.isCorrect(question, root, quality)
        submit(run, current, question, correct, Feedback(correct, null, root, question.positions, chosenQuality = quality))
    }

    /** Réponse à l'exercice « accord à l'oreille » : le type choisi. */
    fun answerQuality(quality: ChordQuality) {
        val run = run ?: return
        val current = phase as? QuizPhase.Running ?: return
        val question = current.question as? Question.EarChord ?: return
        if (current.feedback != null) return
        val correct = QuizRules.isCorrect(question, quality)
        submit(
            run,
            current,
            question,
            correct,
            Feedback(correct, null, question.pitchClass, question.positions, chosenQuality = quality),
        )
    }

    /**
     * « Placer l'accord » : une case touchée. Une seule case par corde (toucher la même corde remplace la case) ; la réponse est
     * jugée dès que chaque corde du groupe a la sienne. Une case hors du groupe de cordes est ignorée.
     */
    fun tapChord(position: FretPosition) {
        val run = run ?: return
        val current = phase as? QuizPhase.Running ?: return
        val question = current.question as? Question.PlaceChord ?: return
        if (current.feedback != null) return
        val spec = question.spec
        if (QuizRules.placementRole(spec, position.string) == null) return

        if (run.setup.soundEnabled) notes.play(run.board.pitchAt(position), run.setup.instrument.timbre)
        val picked = (current.picked.filter { it.string != position.string } + position).sortedBy { it.string }
        if (!QuizRules.placementComplete(spec, picked)) {
            phase = QuizPhase.Running(run.setup, current.number, question, run.correct, feedback = null, picked = picked)
            return
        }

        val results = QuizRules.judgePlacement(run.board, spec, picked)
        val correct = results.all { it }
        val shape = QuizRules.correctVoicing(run.board, run.setup.zone, spec)?.positions.orEmpty()
        if (run.setup.soundEnabled && shape.isNotEmpty()) {
            // La forme juste, une fois la dernière note retombée : on entend ce qu'il fallait jouer.
            scope.launch {
                delay(CORRECTION_SOUND_DELAY_MS)
                strum(run, shape)
            }
        }
        submit(
            run,
            current,
            question,
            correct,
            Feedback(correct, null, spec.chord.root, shape, placement = picked.zip(results)),
        )
    }

    /** Fait réentendre l'accord de la question (exercices « nommer l'accord » et « accord à l'oreille »). */
    fun replaySound() {
        val run = run ?: return
        val current = phase as? QuizPhase.Running ?: return
        when (val question = current.question) {
            is Question.NameChord -> strum(run, question.positions)
            is Question.EarChord -> strum(run, question.positions)
            else -> Unit
        }
    }

    /** Abandonne la série en cours (ce qui a été répondu est quand même retenu) et revient au réglage. */
    fun quit() {
        run?.let { save(it, it.started.elapsedNow().inWholeMilliseconds) }
        stopJobs()
        run = null
        phase = QuizPhase.Setup
    }

    /** Du bilan au réglage de la série. */
    fun backToSetup() {
        phase = QuizPhase.Setup
    }

    private fun stopJobs() {
        timer?.cancel()
        advance?.cancel()
        timer = null
        advance = null
    }

    private fun startTimer(run: Run, limitMillis: Long) {
        remainingMillis = limitMillis
        timer = scope.launch {
            while (true) {
                val left = limitMillis - run.started.elapsedNow().inWholeMilliseconds
                remainingMillis = left.coerceAtLeast(0L)
                if (left <= 0L) {
                    finish()
                    break
                }
                delay(TIMER_STEP_MS)
            }
        }
    }

    /** Joue les notes d'une forme d'accord à la suite, du grave à l'aigu, comme un accord gratté. */
    private fun strum(run: Run, positions: List<FretPosition>) {
        if (!run.setup.soundEnabled || positions.isEmpty()) return
        val pitches = positions.map(run.board::pitchAt)
        notes.playSequence(pitches, run.setup.instrument.timbre, STRUM_MS, 0.85f / sqrt(pitches.size.toFloat()))
    }

    private fun nextQuestion() {
        val run = run ?: return
        val total = run.setup.config.length.questions
        if (total != null && run.number >= total) {
            finish()
            return
        }
        val question = run.generator.next(run.question)
        run.question = question
        run.number++
        run.questionStart = TimeSource.Monotonic.markNow()
        phase = QuizPhase.Running(run.setup, run.number, question, run.correct, feedback = null)
        // « Nommer » : on fait entendre la note de la case montrée (entraînement de l'oreille, sans rien révéler) ; pour les
        // accords, on fait entendre l'accord.
        when (question) {
            is Question.Name -> if (run.setup.soundEnabled) notes.play(run.board.pitchAt(question.position), run.setup.instrument.timbre)
            is Question.NameChord -> strum(run, question.positions)
            is Question.EarChord -> strum(run, question.positions)
            else -> Unit
        }
    }

    private fun submit(
        run: Run,
        current: QuizPhase.Running,
        question: Question,
        correct: Boolean,
        feedback: Feedback,
        cells: List<FretPosition> = emptyList(),
    ) {
        val quality = question.chordQuality
        run.answers += Answer(question.pitchClass, correct, run.questionStart.elapsedNow().inWholeMilliseconds, quality, cells)
        if (correct) {
            run.correct++
        } else if (quality != null) {
            run.qualityMisses[quality.ordinal]++
        } else {
            run.misses[question.pitchClass]++
        }
        run.lastAnswerMillis = run.started.elapsedNow().inWholeMilliseconds
        phase = QuizPhase.Running(run.setup, current.number, question, run.correct, feedback)
        advance?.cancel()
        advance = scope.launch {
            // Une erreur sur un accord demande plus de temps pour lire la correction.
            delay(if (correct) CORRECT_PAUSE_MS else if (quality != null) WRONG_CHORD_PAUSE_MS else WRONG_PAUSE_MS)
            nextQuestion()
        }
    }

    private fun finish() {
        val run = run ?: return
        stopJobs()
        val limit = run.setup.config.length.seconds?.let { it * 1000L }
        val duration = if (limit != null) limit else run.lastAnswerMillis
        save(run, duration)
        val answers = run.answers
        val average = if (answers.isEmpty()) 0L else answers.sumOf { it.millis.coerceIn(0L, NoteStat.MAX_ANSWER_MILLIS) } / answers.size
        val missed = run.misses.withIndex().filter { it.value > 0 }.sortedByDescending { it.value }.map { it.index }
        val missedQualities = run.qualityMisses.withIndex().filter { it.value > 0 }.sortedByDescending { it.value }
            .map { ChordQuality.entries[it.index] }
        phase = QuizPhase.Done(SessionResult(run.setup, run.correct, answers.size, duration, average, missed, missedQualities))
        this.run = null
    }

    @OptIn(ExperimentalTime::class)
    private fun save(run: Run, durationMillis: Long) {
        if (run.answers.isEmpty()) return
        val answers = run.answers.toList()
        val entry = SessionEntry(
            epochMillis = Clock.System.now().toEpochMilliseconds(),
            mode = run.setup.config.mode,
            correct = run.correct,
            answered = answers.size,
            durationMillis = durationMillis,
        )
        scope.launch { stats.record(answers, entry, run.setup.instrument) }
    }

    private companion object {
        const val CORRECT_PAUSE_MS = 750L
        const val WRONG_PAUSE_MS = 1800L
        const val WRONG_CHORD_PAUSE_MS = 2600L
        const val CORRECTION_SOUND_DELAY_MS = 650L
        const val STRUM_MS = 35L
        const val TIMER_STEP_MS = 100L
    }
}
