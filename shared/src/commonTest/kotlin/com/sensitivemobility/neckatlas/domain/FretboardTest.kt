package com.sensitivemobility.neckatlas.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FretboardTest {
    private val la = 9

    @Test
    fun pitchBasics() {
        assertEquals(9, Pitch(69).pitchClass)
        assertEquals(4, Pitch(69).octave)
        assertEquals(4, Pitch(60).octave)
        assertTrue(abs(Pitch(69).frequencyHz - 440.0) < 1e-9)
        assertTrue(abs(Pitch(57).frequencyHz - 220.0) < 1e-9)
    }

    @Test
    fun standardGuitarTuning() {
        val classes = Instrument.GUITAR_6.openStrings.map { it.pitchClass }
        // Mi La Ré Sol Si Mi, de la grave à l'aiguë.
        assertEquals(listOf(4, 9, 2, 7, 11, 4), classes)
        assertEquals(40, Instrument.GUITAR_6.openStrings.first().midi)
    }

    @Test
    fun stringCounts() {
        assertEquals(6, Instrument.GUITAR_6.stringCount)
        assertEquals(7, Instrument.GUITAR_7.stringCount)
        assertEquals(4, Instrument.BASS_4.stringCount)
        assertEquals(5, Instrument.BASS_5.stringCount)
    }

    @Test
    fun sevenStringAndFiveStringAddALowB() {
        assertEquals(11, Instrument.GUITAR_7.openStrings.first().pitchClass)
        assertEquals(11, Instrument.BASS_5.openStrings.first().pitchClass)
        // Le reste de la guitare 7 est la guitare 6 ; le reste de la basse 5 est la basse 4.
        assertEquals(Instrument.GUITAR_6.openStrings, Instrument.GUITAR_7.openStrings.drop(1))
        assertEquals(Instrument.BASS_4.openStrings, Instrument.BASS_5.openStrings.drop(1))
    }

    @Test
    fun bassIsOneOctaveBelowTheGuitarStrings() {
        assertEquals(
            Instrument.GUITAR_6.openStrings.take(4).map { it.midi - 12 },
            Instrument.BASS_4.openStrings.map { it.midi },
        )
    }

    @Test
    fun stringNumbersStartFromTheHighString() {
        assertEquals(1, Instrument.GUITAR_6.stringNumber(5))
        assertEquals(6, Instrument.GUITAR_6.stringNumber(0))
        assertEquals(7, Instrument.GUITAR_7.stringNumber(0))
    }

    @Test
    fun pitchAtAddsFretsToTheOpenString() {
        val board = Fretboard(Instrument.GUITAR_6)
        assertEquals(40, board.pitchAt(FretPosition(0, 0)).midi)
        assertEquals(45, board.pitchAt(FretPosition(0, 5)).midi) // Mi grave, case 5 = La
        assertEquals(76, board.pitchAt(FretPosition(5, 12)).midi) // Mi aigu, case 12 = Mi une octave plus haut
    }

    @Test
    fun adjacentStringsAreAFourthApartExceptGB() {
        val strings = Instrument.GUITAR_6.openStrings.map { it.midi }
        assertEquals(listOf(5, 5, 5, 4, 5), strings.zipWithNext { a, b -> b - a })
    }

    @Test
    fun positionsOfLaOnTheFirstTwelveFrets() {
        val board = Fretboard(Instrument.GUITAR_6)
        val positions = board.positionsOf(la, FretZone.firstFrets(12))
        assertEquals(
            setOf(
                FretPosition(0, 5),
                FretPosition(1, 0), FretPosition(1, 12),
                FretPosition(2, 7),
                FretPosition(3, 2),
                FretPosition(4, 10),
                FretPosition(5, 5),
            ),
            positions.toSet(),
        )
        assertEquals(positions.size, positions.toSet().size)
    }

    @Test
    fun positionsOfLaInAZoneFrom8To12() {
        val board = Fretboard(Instrument.GUITAR_6)
        val positions = board.positionsOf(la, FretZone(8, 12))
        assertEquals(setOf(FretPosition(1, 12), FretPosition(4, 10)), positions.toSet())
    }

    @Test
    fun everyNoteAppearsOnEveryStringWithinTwelveFrets() {
        for (instrument in Instrument.entries) {
            val board = Fretboard(instrument)
            for (pitchClass in 0..11) {
                val strings = board.positionsOf(pitchClass, FretZone.firstFrets(11)).map { it.string }.toSet()
                assertEquals((0 until instrument.stringCount).toSet(), strings, "$instrument classe $pitchClass")
            }
        }
    }

    @Test
    fun zoneValidation() {
        assertTrue(FretZone(0, 12).includesOpenStrings)
        assertTrue(!FretZone(8, 12).includesOpenStrings)
        assertTrue(9 in FretZone(8, 12))
        assertTrue(7 !in FretZone(8, 12))
        assertEquals(listOf(8, 9, 10), FretZone(8, 10).frets.toList())
        assertFailsWith<IllegalArgumentException> { FretZone(5, 4) }
        assertFailsWith<IllegalArgumentException> { FretZone(-1, 4) }
        assertFailsWith<IllegalArgumentException> { FretZone(0, 25) }
        assertFailsWith<IllegalArgumentException> { FretZone(0, 0) }
        assertEquals(FretZone(0, 24), FretZone.firstFrets(24))
    }
}
