package com.sensitivemobility.neckatlas.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NoteNamesTest {
    @Test
    fun naturalNotesIgnoreAccidentalStyle() {
        for (style in AccidentalStyle.entries) {
            assertEquals("Do", NoteNames.label(0, NoteNotation.SOLFEGE, style))
            assertEquals("Sol", NoteNames.label(7, NoteNotation.SOLFEGE, style))
            assertEquals("G", NoteNames.label(7, NoteNotation.LETTERS, style))
        }
    }

    @Test
    fun solfegeNamesOfAllNaturals() {
        val naturals = listOf(0 to "Do", 2 to "Ré", 4 to "Mi", 5 to "Fa", 7 to "Sol", 9 to "La", 11 to "Si")
        for ((pitchClass, name) in naturals) {
            assertEquals(name, NoteNames.label(pitchClass, NoteNotation.SOLFEGE, AccidentalStyle.BOTH))
        }
    }

    @Test
    fun accidentalsFollowTheStyle() {
        assertEquals("Do♯", NoteNames.label(1, NoteNotation.SOLFEGE, AccidentalStyle.SHARPS))
        assertEquals("Ré♭", NoteNames.label(1, NoteNotation.SOLFEGE, AccidentalStyle.FLATS))
        assertEquals("Do♯/Ré♭", NoteNames.label(1, NoteNotation.SOLFEGE, AccidentalStyle.BOTH))
        assertEquals("F♯/G♭", NoteNames.label(6, NoteNotation.LETTERS, AccidentalStyle.BOTH))
        assertEquals("A♯", NoteNames.label(10, NoteNotation.LETTERS, AccidentalStyle.SHARPS))
        assertEquals("Si♭", NoteNames.label(10, NoteNotation.SOLFEGE, AccidentalStyle.FLATS))
    }

    @Test
    fun everyPitchClassHasASpelling() {
        for (pitchClass in 0..11) {
            val spellings = NoteNames.spellings(pitchClass, NoteNotation.SOLFEGE)
            assertEquals(true, spellings.size in 1..2, "classe $pitchClass")
        }
    }

    @Test
    fun labelWithOctave() {
        assertEquals("Mi2", NoteNames.label(Pitch(40), NoteNotation.SOLFEGE, AccidentalStyle.BOTH))
        assertEquals("La4", NoteNames.label(Pitch(69), NoteNotation.SOLFEGE, AccidentalStyle.BOTH))
        assertEquals("C♯3/D♭3", NoteNames.label(Pitch(49), NoteNotation.LETTERS, AccidentalStyle.BOTH))
    }

    @Test
    fun rejectsOutOfRangePitchClass() {
        assertFailsWith<IllegalArgumentException> { NoteNames.label(12, NoteNotation.SOLFEGE, AccidentalStyle.BOTH) }
    }
}
