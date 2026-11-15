package com.sensitivemobility.neckatlas.domain.quiz

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class QuestionGeneratorTest {
    private val guitar = Fretboard(Instrument.GUITAR_6)

    private fun draw(generator: QuestionGenerator, count: Int): List<Question> {
        val questions = mutableListOf<Question>()
        var previous: Question? = null
        repeat(count) {
            val question = generator.next(previous)
            questions += question
            previous = question
        }
        return questions
    }

    @Test
    fun everyFindQuestionHasAnAnswerInTheZone() {
        for (zone in listOf(FretZone(0, 12), FretZone(8, 12), FretZone(3, 4), FretZone(0, 24))) {
            for (mode in listOf(QuizMode.FIND_ON_STRING, QuizMode.FIND_ANYWHERE)) {
                val generator = QuestionGenerator(guitar, zone, QuizConfig(mode = mode, naturalsOnly = false), random = Random(1))
                for (question in draw(generator, 300)) {
                    question as Question.Find
                    assertTrue(QuizRules.correctPositions(guitar, zone, question).isNotEmpty(), "$mode $zone $question")
                }
            }
        }
    }

    @Test
    fun findOnStringNamesAStringAndFindAnywhereDoesNot() {
        val onString = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(mode = QuizMode.FIND_ON_STRING), random = Random(2))
        assertTrue(draw(onString, 50).all { (it as Question.Find).string != null })
        val anywhere = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(mode = QuizMode.FIND_ANYWHERE), random = Random(2))
        assertTrue(draw(anywhere, 50).all { (it as Question.Find).string == null })
    }

    @Test
    fun naturalsOnlyKeepsToNaturalNotes() {
        val config = QuizConfig(naturalsOnly = true)
        for (mode in QuizMode.entries.filter { !it.isChord }) {
            val generator = QuestionGenerator(guitar, FretZone.DEFAULT, config.copy(mode = mode), random = Random(3))
            assertTrue(draw(generator, 300).all { it.pitchClass in NaturalPitchClasses }, "$mode")
        }
    }

    @Test
    fun allNotesAreAskedWhenNaturalsOnlyIsOff() {
        val generator = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(naturalsOnly = false), random = Random(4))
        assertEquals((0..11).toSet(), draw(generator, 600).map { it.pitchClass }.toSet())
    }

    @Test
    fun stringFilterIsRespected() {
        val onString = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(strings = setOf(1, 3)), random = Random(5))
        assertEquals(setOf(1, 3), draw(onString, 200).map { (it as Question.Find).string }.toSet())

        val naming = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(mode = QuizMode.NAME_NOTE, strings = setOf(0)), random = Random(5))
        assertEquals(setOf(0), draw(naming, 200).map { (it as Question.Name).position.string }.toSet())
    }

    @Test
    fun anEmptyOrUnknownStringSelectionMeansAllStrings() {
        for (strings in listOf(emptySet(), setOf(9))) {
            val generator = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(strings = strings), random = Random(6))
            assertEquals((0..5).toSet(), draw(generator, 400).map { (it as Question.Find).string }.toSet())
        }
    }

    @Test
    fun namingQuestionsStayInsideTheZone() {
        val zone = FretZone(8, 12)
        val generator = QuestionGenerator(guitar, zone, QuizConfig(mode = QuizMode.NAME_NOTE, naturalsOnly = false), random = Random(7))
        for (question in draw(generator, 300)) {
            question as Question.Name
            assertTrue(question.position.fret in zone)
            assertEquals(guitar.pitchAt(question.position).pitchClass, question.pitchClass)
        }
    }

    @Test
    fun theSameQuestionNeverComesTwiceInARow() {
        val generator = QuestionGenerator(guitar, FretZone(5, 6), QuizConfig(mode = QuizMode.NAME_NOTE, naturalsOnly = false), random = Random(8))
        val questions = draw(generator, 300)
        for (i in 1 until questions.size) assertNotEquals(questions[i - 1], questions[i])
    }

    @Test
    fun aSingleQuestionPoolStillWorks() {
        // Une seule case, une seule corde : il n'y a qu'une question possible, elle revient.
        val generator = QuestionGenerator(guitar, FretZone(5, 5), QuizConfig(mode = QuizMode.NAME_NOTE, strings = setOf(0), naturalsOnly = false))
        val first = generator.next()
        assertEquals(first, generator.next(first))
    }

    @Test
    fun aZoneWithoutNaturalNotesFallsBackToAccidentals() {
        // Corde de Mi grave, case 1 : Fa ; case 4 : Sol♯. Seule la case 4 ne donne pas de naturelle.
        val generator = QuestionGenerator(guitar, FretZone(4, 4), QuizConfig(strings = setOf(0), naturalsOnly = true), random = Random(9))
        assertEquals(8, generator.next().pitchClass)
    }

    @Test
    fun weakNotesComeBackMoreOften() {
        val weights: (Int) -> Double = { if (it == 0) 20.0 else 1.0 }
        val generator = QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(naturalsOnly = true, strings = setOf(0, 1, 2, 3, 4, 5)), weights, Random(10))
        val questions = draw(generator, 1000)
        val share = questions.count { it.pitchClass == 0 }.toDouble() / questions.size
        assertTrue(share > 0.45, "part des Do : $share")
    }

    @Test
    fun weakCellsComeBackMoreOftenWhenNaming() {
        // Corde de La, case 5 (un Ré) très ratée ; les autres cases sont neutres.
        val weakCell = FretPosition(1, 5)
        val factor: (FretPosition) -> Double = { if (it == weakCell) 20.0 else 1.0 }
        val generator = QuestionGenerator(
            guitar,
            FretZone(0, 12),
            QuizConfig(mode = QuizMode.NAME_NOTE, naturalsOnly = false),
            random = Random(11),
            cellFactor = factor,
        )
        val questions = draw(generator, 1000)
        val share = questions.count { (it as Question.Name).position == weakCell }.toDouble() / questions.size
        // Sans le poids, la case reviendrait une fois sur 78 (6 cordes, 13 cases) ; avec, elle domine.
        assertTrue(share > 0.15, "part de la case ratée : $share")
    }

    @Test
    fun weakCellsComeBackMoreOftenWhenFindingOnAString() {
        // « Où est le Ré sur la corde de La ? » : la seule case (5) est très ratée.
        val factor: (FretPosition) -> Double = { if (it == FretPosition(1, 5)) 30.0 else 1.0 }
        val generator = QuestionGenerator(guitar, FretZone(0, 12), QuizConfig(naturalsOnly = true), random = Random(12), cellFactor = factor)
        val questions = draw(generator, 1000)
        val share = questions.count { it == Question.Find(pitchClass = 2, string = 1) }.toDouble() / questions.size
        assertTrue(share > 0.2, "part de la question ratée : $share")
    }

    @Test
    fun cellWeightsDoNotAffectTheAnywhereMode() {
        val factor: (FretPosition) -> Double = { 100.0 }
        val plain = draw(QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(mode = QuizMode.FIND_ANYWHERE), random = Random(13)), 200)
        val weighted = draw(
            QuestionGenerator(guitar, FretZone.DEFAULT, QuizConfig(mode = QuizMode.FIND_ANYWHERE), random = Random(13), cellFactor = factor),
            200,
        )
        assertEquals(plain, weighted)
    }

    @Test
    fun theJudgeAcceptsTheRightPositionsOnly() {
        val zone = FretZone.DEFAULT
        val onString = Question.Find(pitchClass = 9, string = 1) // La sur la corde de La : à vide et case 12
        assertTrue(QuizRules.isCorrect(guitar, zone, onString, FretPosition(1, 0)))
        assertTrue(QuizRules.isCorrect(guitar, zone, onString, FretPosition(1, 12)))
        assertTrue(!QuizRules.isCorrect(guitar, zone, onString, FretPosition(1, 5)))
        assertTrue(!QuizRules.isCorrect(guitar, zone, onString, FretPosition(0, 5))) // un La, mais sur une autre corde
        assertEquals(listOf(FretPosition(1, 0), FretPosition(1, 12)), QuizRules.correctPositions(guitar, zone, onString))

        val anywhere = Question.Find(pitchClass = 9, string = null)
        assertTrue(QuizRules.isCorrect(guitar, zone, anywhere, FretPosition(0, 5)))
        assertTrue(QuizRules.isCorrect(guitar, zone, anywhere, FretPosition(5, 5)))
        assertTrue(!QuizRules.isCorrect(guitar, zone, anywhere, FretPosition(5, 6)))
        assertEquals(7, QuizRules.correctPositions(guitar, zone, anywhere).size)

        // Hors de la zone, une bonne note ne compte pas.
        assertTrue(!QuizRules.isCorrect(guitar, FretZone(1, 4), onString, FretPosition(1, 12)))

        val naming = Question.Name(FretPosition(2, 3), pitchClass = 5) // corde de Ré, case 3 : Fa
        assertTrue(QuizRules.isCorrect(naming, 5))
        assertTrue(!QuizRules.isCorrect(naming, 6))
    }
}
