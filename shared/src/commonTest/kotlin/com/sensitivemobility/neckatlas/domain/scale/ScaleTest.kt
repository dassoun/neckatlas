package com.sensitivemobility.neckatlas.domain.scale

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleTest {
    private val Do = 0
    private val Re = 2
    private val Mi = 4
    private val Fa = 5
    private val Sol = 7
    private val La = 9
    private val Si = 11

    @Test
    fun cMajorIsTheWhiteKeys() {
        assertEquals(listOf(0, 2, 4, 5, 7, 9, 11), Scale(Do, ScaleType.MAJOR).pitchClasses)
    }

    @Test
    fun gMajorHasAnFSharp() {
        assertEquals(listOf(7, 9, 11, 0, 2, 4, 6), Scale(Sol, ScaleType.MAJOR).pitchClasses)
    }

    @Test
    fun theMinorPentatonicOfAIsTheMajorPentatonicOfC() {
        assertEquals(
            Scale(Do, ScaleType.MAJOR_PENTATONIC).pitchClasses.toSet(),
            Scale(La, ScaleType.MINOR_PENTATONIC).pitchClasses.toSet(),
        )
    }

    @Test
    fun modesAreTheSameNotesAsTheirParentMajor() {
        // Do majeur : Ré dorien, Mi phrygien, Fa lydien, Sol mixolydien, La éolien (mineur naturel), Si locrien.
        val parent = Scale(Do, ScaleType.MAJOR).pitchClasses.toSet()
        assertEquals(parent, Scale(Re, ScaleType.DORIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(Mi, ScaleType.PHRYGIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(Fa, ScaleType.LYDIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(Sol, ScaleType.MIXOLYDIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(La, ScaleType.NATURAL_MINOR).pitchClasses.toSet())
        assertEquals(parent, Scale(Si, ScaleType.LOCRIAN).pitchClasses.toSet())
    }

    @Test
    fun harmonicMinorRaisesTheSeventh() {
        // La mineur harmonique : la mineur naturel avec un Sol♯.
        assertEquals(listOf(9, 11, 0, 2, 4, 5, 8), Scale(La, ScaleType.HARMONIC_MINOR).pitchClasses)
    }

    @Test
    fun theBluesAddsTheBlueNoteToTheMinorPentatonic() {
        val blues = Scale(La, ScaleType.BLUES).pitchClasses.toSet()
        val pentatonic = Scale(La, ScaleType.MINOR_PENTATONIC).pitchClasses.toSet()
        assertEquals(setOf(3), blues - pentatonic) // le Mi♭ (♭5 de La)
        assertEquals(emptySet(), pentatonic - blues)
    }

    @Test
    fun everyTypeIsWellFormed() {
        for (type in ScaleType.entries) {
            assertEquals(type.intervals.size, type.degrees.size, type.name)
            assertEquals("1", type.degrees.first(), type.name)
            assertEquals(type.noteCount, Scale(Do, type).pitchClasses.toSet().size, "${type.name} : notes distinctes")
        }
    }

    @Test
    fun degreesAreFoundByPitchClass() {
        val scale = Scale(Sol, ScaleType.MAJOR)
        assertEquals(0, scale.degreeOf(Sol))
        assertEquals(6, scale.degreeOf(6)) // Fa♯, la septième
        assertEquals(null, scale.degreeOf(Fa)) // Fa naturel n'est pas dans Sol majeur
    }

    @Test
    fun allNotesAreOnlyScaleNotesAndCoverEveryOne() {
        val board = Fretboard(Instrument.GUITAR_6)
        val scale = Scale(Do, ScaleType.MAJOR)
        val notes = ScalePositions.allNotes(board, FretZone.DEFAULT, scale)
        assertTrue(notes.all { board.pitchAt(it.position).pitchClass in scale.pitchClasses })
        // Dans les 12 premières cases chaque classe de hauteur apparaît sur chaque corde : sept notes x 6 cordes, plus la 12e case.
        assertEquals(scale.pitchClasses.toSet(), notes.map { board.pitchAt(it.position).pitchClass }.toSet())
        assertEquals(0, notes.first { board.pitchAt(it.position).pitchClass == Do }.degree)
    }

    @Test
    fun aSevenNoteScaleHasSevenPositionsAndAPentatonicFive() {
        val board = Fretboard(Instrument.GUITAR_6)
        assertEquals(7, ScalePositions.find(board, Scale(Do, ScaleType.MAJOR)).size)
        assertEquals(5, ScalePositions.find(board, Scale(La, ScaleType.MINOR_PENTATONIC)).size)
        assertEquals(6, ScalePositions.find(board, Scale(La, ScaleType.BLUES)).size)
    }

    @Test
    fun positionNStartsOnDegreeNOfTheScaleOnTheLowStringAndIsFourFretsWide() {
        val board = Fretboard(Instrument.GUITAR_6)
        val scale = Scale(Do, ScaleType.MAJOR)
        val positions = ScalePositions.find(board, scale)
        positions.forEachIndexed { index, position ->
            assertEquals(index, scale.degreeOf(board.pitchAt(FretPosition(0, position.firstFret)).pitchClass))
        }
        for (position in positions) {
            assertEquals(ScalePositions.WINDOW_FRETS - 1, position.lastFret - position.firstFret)
            assertTrue(scale.degreeOf(board.pitchAt(FretPosition(0, position.firstFret)).pitchClass) != null)
            assertTrue(position.notes.all { it.position.fret in position.firstFret..position.lastFret })
        }
        // Mi grave : Do (8), Ré (10), Mi (0), Fa (1), Sol (3), La (5), Si (7).
        assertEquals(listOf(8, 10, 0, 1, 3, 5, 7), positions.map { it.firstFret })
    }

    @Test
    fun eachPositionHasTwoOrThreeNotesOnEveryStringForADiatonicScale() {
        val board = Fretboard(Instrument.GUITAR_6)
        for (root in 0..11) {
            for (position in ScalePositions.find(board, Scale(root, ScaleType.MAJOR))) {
                for (string in 0 until 6) {
                    val onString = position.notes.count { it.position.string == string }
                    assertTrue(onString in 2..3, "racine $root, cases ${position.firstFret}-${position.lastFret}, corde $string : $onString notes")
                }
            }
        }
    }

    @Test
    fun positionsTogetherCoverEveryScaleNoteOfTheFirstOctave() {
        val board = Fretboard(Instrument.GUITAR_6)
        val scale = Scale(Sol, ScaleType.MAJOR)
        val covered = ScalePositions.find(board, scale).flatMap { p -> p.notes.map { it.position } }.toSet()
        val expected = ScalePositions.allNotes(board, FretZone(0, 12), scale).map { it.position }.toSet()
        assertTrue(covered.containsAll(expected), "notes de la gamme sans position : ${expected - covered}")
    }

    @Test
    fun theScaleGoesUpOneOctaveFromTheLowestRoot() {
        val board = Fretboard(Instrument.GUITAR_6)
        val notes = ScalePositions.allNotes(board, FretZone.DEFAULT, Scale(Do, ScaleType.MAJOR))
        val midi = ScalePositions.ascending(board, notes).map { it.midi }
        // Do3 (48) à Do4 (60) : Do Ré Mi Fa Sol La Si Do.
        assertEquals(listOf(48, 50, 52, 53, 55, 57, 59, 60), midi)
    }

    @Test
    fun withoutARootTheScaleJustGoesUp() {
        val board = Fretboard(Instrument.GUITAR_6)
        val noRoot = ScalePositions.allNotes(board, FretZone(1, 2), Scale(Do, ScaleType.MAJOR)).filter { it.degree != 0 }
        val midi = ScalePositions.ascending(board, noRoot).map { it.midi }
        assertEquals(midi.sorted(), midi)
        assertEquals(midi.distinct(), midi)
        assertTrue(midi.isNotEmpty())
    }

    @Test
    fun scalesWorkOnABassToo() {
        val board = Fretboard(Instrument.BASS_4)
        val positions = ScalePositions.find(board, Scale(Do, ScaleType.MAJOR))
        assertEquals(7, positions.size)
        assertTrue(positions.all { p -> p.notes.all { it.position.string in 0..3 } })
    }
}
