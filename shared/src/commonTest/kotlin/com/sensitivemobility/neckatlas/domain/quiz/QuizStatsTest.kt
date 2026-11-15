package com.sensitivemobility.neckatlas.domain.quiz

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuizStatsTest {
    private fun entry(at: Long = 0L) = SessionEntry(at, QuizMode.NAME_NOTE, correct = 1, answered = 2, durationMillis = 5_000)

    @Test
    fun anEmptyStatHasNoAccuracyNorTime() {
        val stat = NoteStat()
        assertNull(stat.accuracy)
        assertNull(stat.averageMillis)
    }

    @Test
    fun answersAreAddedToTheirNote() {
        val stats = QuizStats().withAnswers(
            listOf(Answer(9, true, 1_000), Answer(9, false, 3_000), Answer(0, true, 500)),
            entry = null,
        )
        assertEquals(NoteStat(attempts = 2, correct = 1, totalMillis = 4_000), stats.notes[9])
        assertEquals(0.5, stats.notes[9].accuracy)
        assertEquals(2_000L, stats.notes[9].averageMillis)
        assertEquals(NoteStat(1, 1, 500), stats.notes[0])
        assertEquals(3, stats.answered)
        assertEquals(2, stats.correct)
        assertEquals(NoteStat(), stats.notes[5])
    }

    @Test
    fun aVeryLongAnswerIsCapped() {
        val stats = QuizStats().withAnswers(listOf(Answer(4, true, 10 * 60_000L), Answer(4, true, -5)), entry = null)
        assertEquals(NoteStat.MAX_ANSWER_MILLIS, stats.notes[4].totalMillis)
    }

    @Test
    fun statsAreNotModifiedInPlace() {
        val before = QuizStats()
        before.withAnswers(listOf(Answer(0, true, 100)), entry())
        assertEquals(QuizStats(), before)
    }

    @Test
    fun theHistoryKeepsTheLatestSessions() {
        var stats = QuizStats()
        for (i in 1..40) stats = stats.withAnswers(emptyList(), entry(at = i.toLong()))
        assertEquals(30, stats.history.size)
        assertEquals(11L, stats.history.first().epochMillis)
        assertEquals(40L, stats.history.last().epochMillis)
        // Sans série à ajouter, l'historique ne change pas.
        assertEquals(stats.history, stats.withAnswers(emptyList(), null).history)
        assertEquals(5, stats.withAnswers(emptyList(), entry(at = 41), keepHistory = 5).history.size)
    }

    @Test
    fun weightsGoDownWithSuccessAndUpWithFailures() {
        val fresh = QuizStats()
        assertEquals(3.0, fresh.weight(0), 1e-9)

        val good = fresh.withAnswers(List(10) { Answer(0, true, 800) }, null)
        val bad = fresh.withAnswers(List(10) { Answer(0, false, 800) }, null)
        assertTrue(good.weight(0) < 1.5, "maîtrisée : ${good.weight(0)}")
        assertTrue(bad.weight(0) > 4.0, "toujours ratée : ${bad.weight(0)}")
        assertTrue(good.weight(0) < fresh.weight(0) && fresh.weight(0) < bad.weight(0))
        assertEquals(fresh.weight(1), good.weight(1), 1e-9) // les autres notes ne bougent pas
    }

    @Test
    fun aSingleAnswerDoesNotFlipAWeightCompletely() {
        val oneMiss = QuizStats().withAnswers(listOf(Answer(2, false, 1_000)), null)
        assertTrue(oneMiss.weight(2) in 3.0..4.5)
    }

    @Test
    fun answersCreditTheirCellsToTheInstrument() {
        val a = FretPosition(1, 7)
        val b = FretPosition(1, 19)
        val stats = QuizStats().withAnswers(
            listOf(
                Answer(0, true, 1_000, positions = listOf(a)),
                Answer(0, false, 3_000, positions = listOf(a, b)), // erreur : les deux cases qu'il fallait trouver
                Answer(2, true, 500), // aucune case visée
            ),
            entry = null,
            instrument = Instrument.GUITAR_6,
        )
        assertEquals(NoteStat(2, 1, 4_000), stats.cellStats(Instrument.GUITAR_6)[a])
        assertEquals(NoteStat(1, 0, 3_000), stats.cellStats(Instrument.GUITAR_6)[b])
        assertEquals(2, stats.cellStats(Instrument.GUITAR_6).size)
        // Un autre instrument n'a rien ; les notes comptent comme avant.
        assertEquals(emptyMap(), stats.cellStats(Instrument.BASS_4))
        assertEquals(NoteStat(2, 1, 4_000), stats.notes[0])
        assertEquals(NoteStat(1, 1, 500), stats.notes[2])
    }

    @Test
    fun cellsAccumulateAcrossSessionsAndStayPerInstrument() {
        val position = FretPosition(0, 5)
        val first = QuizStats().withAnswers(listOf(Answer(9, true, 800, positions = listOf(position))), null, instrument = Instrument.GUITAR_6)
        val second = first.withAnswers(listOf(Answer(9, true, 600, positions = listOf(position))), null, instrument = Instrument.BASS_4)
        val third = second.withAnswers(listOf(Answer(9, false, 900, positions = listOf(position))), null, instrument = Instrument.GUITAR_6)
        assertEquals(NoteStat(2, 1, 1_700), third.cellStats(Instrument.GUITAR_6)[position])
        assertEquals(NoteStat(1, 1, 600), third.cellStats(Instrument.BASS_4)[position])
    }

    @Test
    fun cellsAreIgnoredWithoutAnInstrument() {
        val stats = QuizStats().withAnswers(listOf(Answer(9, true, 800, positions = listOf(FretPosition(0, 5)))), null)
        assertEquals(emptyMap(), stats.cells)
    }

    @Test
    fun chordAnswersMayAlsoCarryNoCells() {
        val stats = QuizStats().withAnswers(listOf(Answer(0, true, 800, ChordQuality.MAJOR)), null, instrument = Instrument.GUITAR_6)
        assertEquals(emptyMap(), stats.cellStats(Instrument.GUITAR_6))
    }

    @Test
    fun cellFactorsAreNeutralForNewCellsAndFollowTheResults() {
        val good = FretPosition(0, 1)
        val bad = FretPosition(0, 2)
        val stats = QuizStats().withAnswers(
            List(10) { Answer(0, true, 800, positions = listOf(good)) } + List(10) { Answer(0, false, 800, positions = listOf(bad)) },
            null,
            instrument = Instrument.GUITAR_6,
        )
        assertEquals(1.0, stats.cellFactor(Instrument.GUITAR_6, FretPosition(3, 3)), 1e-9) // jamais interrogée
        assertEquals(1.0, stats.cellFactor(Instrument.BASS_4, good), 1e-9) // autre instrument
        assertTrue(stats.cellFactor(Instrument.GUITAR_6, good) < 0.6, "maîtrisée : ${stats.cellFactor(Instrument.GUITAR_6, good)}")
        assertTrue(stats.cellFactor(Instrument.GUITAR_6, bad) > 1.4, "ratée : ${stats.cellFactor(Instrument.GUITAR_6, bad)}")
    }

    @Test
    fun weakestNotesNeedEnoughAttemptsAndBelowNinetyPercent() {
        val answers = buildList {
            repeat(10) { add(Answer(0, it < 5, 1_000)) } // Do : 50 %
            repeat(10) { add(Answer(2, it < 8, 1_000)) } // Ré : 80 %
            repeat(10) { add(Answer(4, true, 1_000)) } // Mi : 100 %
            repeat(2) { add(Answer(5, false, 1_000)) } // Fa : 0 %, mais seulement 2 réponses
            repeat(10) { add(Answer(7, it < 5, 4_000)) } // Sol : 50 %, plus lent que Do
        }
        val stats = QuizStats().withAnswers(answers, null)
        // Les pires d'abord ; à réussite égale, la plus lente passe en premier.
        assertEquals(listOf(7, 0, 2), stats.weakest())
        assertEquals(listOf(7), stats.weakest(limit = 1))
        assertEquals(listOf(5, 7, 0, 2), stats.weakest(limit = 10, minAttempts = 2))
    }
}
