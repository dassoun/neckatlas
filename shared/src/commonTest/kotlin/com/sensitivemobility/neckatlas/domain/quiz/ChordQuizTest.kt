package com.sensitivemobility.neckatlas.domain.quiz

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.chord.Chord
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChordQuizTest {
    private val guitar = Fretboard(Instrument.GUITAR_6)
    private val zone = FretZone.DEFAULT

    private fun generator(
        mode: QuizMode,
        config: QuizConfig = QuizConfig(),
        zone: FretZone = this.zone,
        weight: (ChordQuality) -> Double = { 1.0 },
        seed: Int = 1,
        board: Fretboard = guitar,
    ) = ChordQuestionGenerator(board, zone, config.copy(mode = mode), weight, Random(seed))

    private fun draw(generator: QuestionSource, count: Int): List<Question> {
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
    fun theModesKnowWhichAreAboutChords() {
        assertEquals(
            setOf(QuizMode.NAME_CHORD, QuizMode.PLACE_CHORD, QuizMode.EAR_CHORD),
            QuizMode.entries.filter { it.isChord }.toSet(),
        )
    }

    @Test
    fun theGeneratorsRefuseTheWrongKindOfMode() {
        assertFailsWith<IllegalArgumentException> { QuestionGenerator(guitar, zone, QuizConfig(mode = QuizMode.NAME_CHORD)) }
        assertFailsWith<IllegalArgumentException> { ChordQuestionGenerator(guitar, zone, QuizConfig(mode = QuizMode.NAME_NOTE)) }
    }

    @Test
    fun namedChordsAreWhatTheNeckShows() {
        val questions = draw(generator(QuizMode.NAME_CHORD, QuizConfig(chordInversions = true, naturalsOnly = false)), 400)
        for (question in questions) {
            question as Question.NameChord
            val spec = question.spec
            val count = spec.chord.quality.toneCount
            assertEquals(count, question.positions.size)
            assertEquals(List(count) { spec.start + it }, question.positions.map { it.string })
            assertTrue(question.positions.all { it.fret in zone })
            // Les notes montrées sont bien celles de l'accord, dans l'ordre du renversement.
            assertEquals(
                List(count) { spec.chord.pitchClasses[(spec.inversion + it) % count] },
                question.positions.map { guitar.pitchAt(it).pitchClass },
            )
        }
    }

    @Test
    fun theChordPoolAndInversionsAreRespected() {
        val triadsOnly = draw(generator(QuizMode.PLACE_CHORD, QuizConfig(chordPool = ChordPool.TRIADS)), 300)
        assertTrue(triadsOnly.all { !it.chordQuality!!.isSeventh })
        val all = draw(generator(QuizMode.PLACE_CHORD, QuizConfig(chordPool = ChordPool.ALL, naturalsOnly = false)), 800)
        assertTrue(all.any { it.chordQuality!!.isSeventh })
        assertTrue(all.any { !it.chordQuality!!.isSeventh })

        val rootOnly = draw(generator(QuizMode.PLACE_CHORD, QuizConfig(chordInversions = false)), 300)
        assertTrue(rootOnly.all { (it as Question.PlaceChord).spec.inversion == 0 })
        val withInversions = draw(generator(QuizMode.PLACE_CHORD, QuizConfig(chordInversions = true)), 600)
        assertEquals(setOf(0, 1, 2), withInversions.map { (it as Question.PlaceChord).spec.inversion }.toSet())
    }

    @Test
    fun naturalRootsOnlyUnlessAllNotesAreAsked() {
        val naturals = draw(generator(QuizMode.EAR_CHORD, QuizConfig(naturalsOnly = true)), 300)
        assertTrue(naturals.all { it.pitchClass in NaturalPitchClasses })
        val everything = draw(generator(QuizMode.EAR_CHORD, QuizConfig(naturalsOnly = false)), 800)
        assertEquals((0..11).toSet(), everything.map { it.pitchClass }.toSet())
    }

    @Test
    fun everyQuestionIsPlayableInTheZone() {
        for (mode in listOf(QuizMode.NAME_CHORD, QuizMode.PLACE_CHORD, QuizMode.EAR_CHORD)) {
            for (zoneUnderTest in listOf(FretZone(0, 12), FretZone(5, 9), FretZone(0, 24))) {
                val source = generator(mode, QuizConfig(chordPool = ChordPool.ALL, chordInversions = true, naturalsOnly = false), zone = zoneUnderTest)
                if (!source.available) continue
                for (question in draw(source, 200)) {
                    val spec = when (question) {
                        is Question.NameChord -> question.spec
                        is Question.PlaceChord -> question.spec
                        is Question.EarChord -> question.spec
                        else -> error("question inattendue : $question")
                    }
                    assertNotNull(QuizRules.correctVoicing(guitar, zoneUnderTest, spec), "$mode $zoneUnderTest $spec")
                }
            }
        }
    }

    private fun assertNotNull(value: Any?, message: String) = assertTrue(value != null, message)

    @Test
    fun aZoneTooSmallForAnyShapeHasNoSeries() {
        // Une seule case : les cordes voisines s'accordent en quartes, aucune triade n'y tient.
        assertTrue(!generator(QuizMode.NAME_CHORD, zone = FretZone(1, 1)).available)
        assertTrue(generator(QuizMode.NAME_CHORD).available)
        assertFailsWith<IllegalStateException> { generator(QuizMode.NAME_CHORD, zone = FretZone(1, 1)).next() }
    }

    @Test
    fun theSameQuestionNeverComesTwiceInARow() {
        val questions = draw(generator(QuizMode.PLACE_CHORD, QuizConfig(naturalsOnly = true), zone = FretZone(3, 8)), 300)
        for (i in 1 until questions.size) assertNotEquals(questions[i - 1], questions[i])
    }

    @Test
    fun weakChordFamiliesComeBackMoreOften() {
        val weights: (ChordQuality) -> Double = { if (it == ChordQuality.AUGMENTED) 25.0 else 1.0 }
        val questions = draw(generator(QuizMode.EAR_CHORD, QuizConfig(naturalsOnly = false), weight = weights, seed = 5), 1000)
        val share = questions.count { it.chordQuality == ChordQuality.AUGMENTED }.toDouble() / questions.size
        assertTrue(share > 0.4, "part des accords augmentés : $share")
    }

    @Test
    fun judgingANamedChordNeedsTheRootAndTheFamily() {
        val spec = ChordSpec(Chord(0, ChordQuality.MAJOR), inversion = 0, start = 3)
        val question = Question.NameChord(spec, listOf(FretPosition(3, 5), FretPosition(4, 5), FretPosition(5, 3)))
        assertTrue(QuizRules.isCorrect(question, 0, ChordQuality.MAJOR))
        assertTrue(!QuizRules.isCorrect(question, 0, ChordQuality.MINOR))
        assertTrue(!QuizRules.isCorrect(question, 4, ChordQuality.MAJOR))
        assertEquals(ChordQuality.MAJOR, question.chordQuality)
        assertEquals(0, question.pitchClass)
    }

    @Test
    fun judgingAnEarQuestionNeedsOnlyTheFamily() {
        val spec = ChordSpec(Chord(7, ChordQuality.MINOR), 0, 3)
        val question = Question.EarChord(spec, emptyList())
        assertTrue(QuizRules.isCorrect(question, ChordQuality.MINOR))
        assertTrue(!QuizRules.isCorrect(question, ChordQuality.MAJOR))
    }

    @Test
    fun placementRolesFollowTheInversion() {
        // Do majeur, 1er renversement, cordes 3·2·1 (indices 3, 4, 5) : Mi, Sol, Do = tierce, quinte, fondamentale.
        val spec = ChordSpec(Chord(0, ChordQuality.MAJOR), inversion = 1, start = 3)
        assertEquals(1, QuizRules.placementRole(spec, 3))
        assertEquals(2, QuizRules.placementRole(spec, 4))
        assertEquals(0, QuizRules.placementRole(spec, 5))
        assertNull(QuizRules.placementRole(spec, 2))
        assertNull(QuizRules.placementRole(spec, 6))
    }

    @Test
    fun judgingAPlacement() {
        val spec = ChordSpec(Chord(0, ChordQuality.MAJOR), inversion = 1, start = 3)
        // La bonne forme : cases 9, 8, 8.
        val right = listOf(FretPosition(3, 9), FretPosition(4, 8), FretPosition(5, 8))
        assertEquals(listOf(true, true, true), QuizRules.judgePlacement(guitar, spec, right))
        // Une note juste dans une autre octave est acceptée (Mi aigu sur la corde de Sol n'existe pas, mais Do à vide + 12 oui).
        val octaveUp = listOf(FretPosition(3, 21), FretPosition(4, 20), FretPosition(5, 20))
        assertEquals(listOf(true, true, true), QuizRules.judgePlacement(guitar, spec, octaveUp))
        // Une erreur sur la corde de Si.
        val oneWrong = listOf(FretPosition(3, 9), FretPosition(4, 7), FretPosition(5, 8))
        assertEquals(listOf(true, false, true), QuizRules.judgePlacement(guitar, spec, oneWrong))
        // Une case hors du groupe de cordes est fausse.
        assertEquals(listOf(false), QuizRules.judgePlacement(guitar, spec, listOf(FretPosition(1, 3))))
    }

    @Test
    fun aPlacementIsCompleteWhenEveryStringHasANote() {
        val spec = ChordSpec(Chord(0, ChordQuality.MAJOR), 0, 3)
        assertTrue(!QuizRules.placementComplete(spec, emptyList()))
        assertTrue(!QuizRules.placementComplete(spec, listOf(FretPosition(3, 5), FretPosition(4, 5))))
        assertTrue(QuizRules.placementComplete(spec, listOf(FretPosition(3, 5), FretPosition(4, 5), FretPosition(5, 3))))
        // Deux cases sur la même corde ne font pas trois cordes.
        assertTrue(!QuizRules.placementComplete(spec, listOf(FretPosition(3, 5), FretPosition(3, 6), FretPosition(4, 5))))
    }

    @Test
    fun theCorrectionIsTheLowestShapeOfTheZone() {
        val spec = ChordSpec(Chord(0, ChordQuality.MAJOR), 1, 3)
        assertEquals(listOf(9, 8, 8), QuizRules.correctVoicing(guitar, zone, spec)!!.positions.map { it.fret })
        assertNull(QuizRules.correctVoicing(guitar, FretZone(1, 4), spec))
    }

    @Test
    fun chordAnswersCountForTheirFamilyNotForTheirRoot() {
        val stats = QuizStats().withAnswers(
            listOf(
                Answer(0, true, 1_000, ChordQuality.MAJOR),
                Answer(0, false, 3_000, ChordQuality.MAJOR),
                Answer(7, true, 500, ChordQuality.DIMINISHED),
                Answer(9, true, 800), // une réponse sur une note
            ),
            entry = null,
        )
        assertEquals(NoteStat(2, 1, 4_000), stats.qualities[ChordQuality.MAJOR.ordinal])
        assertEquals(NoteStat(1, 1, 500), stats.qualities[ChordQuality.DIMINISHED.ordinal])
        assertEquals(NoteStat(1, 1, 800), stats.notes[9])
        assertEquals(NoteStat(), stats.notes[0]) // la fondamentale d'un accord ne compte pas comme une réponse sur la note
        assertEquals(4, stats.answered)
        assertEquals(3, stats.correct)
    }

    @Test
    fun weightsAndWeakFamiliesFollowTheResults() {
        assertEquals(3.0, QuizStats().weight(ChordQuality.MINOR), 1e-9)
        val answers = buildList {
            repeat(10) { add(Answer(0, it < 4, 1_000, ChordQuality.AUGMENTED)) } // 40 %
            repeat(10) { add(Answer(0, true, 1_000, ChordQuality.MAJOR)) } // 100 %
            repeat(2) { add(Answer(0, false, 1_000, ChordQuality.MINOR)) } // 0 %, trop peu de réponses
        }
        val stats = QuizStats().withAnswers(answers, null)
        assertTrue(stats.weight(ChordQuality.AUGMENTED) > stats.weight(ChordQuality.MAJOR))
        assertEquals(listOf(ChordQuality.AUGMENTED), stats.weakestQualities())
        assertEquals(listOf(ChordQuality.MINOR, ChordQuality.AUGMENTED), stats.weakestQualities(minAttempts = 2))
        assertEquals(emptyList(), stats.weakest()) // aucune note n'a été interrogée
    }
}
