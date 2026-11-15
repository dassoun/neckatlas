package com.sensitivemobility.neckatlas.domain.chord

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChordTest {
    private val guitar = Fretboard(Instrument.GUITAR_6)
    private val c = 0
    private val g = 7

    private fun frets(voicing: Voicing) = voicing.positions.map { it.fret }

    @Test
    fun theNotesOfEachQuality() {
        assertEquals(listOf(0, 4, 7), Chord(c, ChordQuality.MAJOR).pitchClasses)
        assertEquals(listOf(0, 3, 7), Chord(c, ChordQuality.MINOR).pitchClasses)
        assertEquals(listOf(0, 3, 6), Chord(c, ChordQuality.DIMINISHED).pitchClasses)
        assertEquals(listOf(0, 4, 8), Chord(c, ChordQuality.AUGMENTED).pitchClasses)
        assertEquals(listOf(0, 4, 7, 10), Chord(c, ChordQuality.DOMINANT_7).pitchClasses)
        assertEquals(listOf(0, 4, 7, 11), Chord(c, ChordQuality.MAJOR_7).pitchClasses)
        assertEquals(listOf(0, 3, 7, 10), Chord(c, ChordQuality.MINOR_7).pitchClasses)
        assertEquals(listOf(0, 3, 6, 10), Chord(c, ChordQuality.HALF_DIMINISHED).pitchClasses)
        assertEquals(listOf(0, 3, 6, 9), Chord(c, ChordQuality.DIMINISHED_7).pitchClasses)
    }

    @Test
    fun theChordWrapsAroundTheOctave() {
        // Sol majeur : Sol Si Ré. Si majeur : Si Ré♯ Fa♯.
        assertEquals(listOf(7, 11, 2), Chord(g, ChordQuality.MAJOR).pitchClasses)
        assertEquals(listOf(11, 3, 6), Chord(11, ChordQuality.MAJOR).pitchClasses)
    }

    @Test
    fun toneCountsAndSevenths() {
        assertEquals(4, ChordQuality.entries.count { !it.isSeventh })
        assertEquals(5, ChordQuality.entries.count { it.isSeventh })
        ChordQuality.entries.forEach { assertEquals(if (it.isSeventh) 4 else 3, it.toneCount, it.name) }
    }

    @Test
    fun roleLabelsNameTheIntervals() {
        assertEquals(listOf("R", "3", "5"), ChordQuality.MAJOR.roleLabels)
        assertEquals(listOf("R", "♭3", "5"), ChordQuality.MINOR.roleLabels)
        assertEquals(listOf("R", "♭3", "♭5"), ChordQuality.DIMINISHED.roleLabels)
        assertEquals(listOf("R", "3", "♯5"), ChordQuality.AUGMENTED.roleLabels)
        assertEquals(listOf("R", "3", "5", "♭7"), ChordQuality.DOMINANT_7.roleLabels)
        assertEquals(listOf("R", "3", "5", "7"), ChordQuality.MAJOR_7.roleLabels)
        assertEquals(listOf("R", "♭3", "♭5", "♭7"), ChordQuality.HALF_DIMINISHED.roleLabels)
        assertEquals(listOf("R", "♭3", "♭5", "♭♭7"), ChordQuality.DIMINISHED_7.roleLabels)
    }

    @Test
    fun roleOfFindsTheNotesOfTheChord() {
        val cMajor = Chord(c, ChordQuality.MAJOR)
        assertEquals(0, cMajor.roleOf(0))
        assertEquals(1, cMajor.roleOf(4))
        assertEquals(2, cMajor.roleOf(7))
        assertNull(cMajor.roleOf(5))
    }

    @Test
    fun rootPositionOfGMajorOnTheThreeHighStrings() {
        // Sol Si Ré sur les cordes 3, 2, 1 : la corde de Sol et celle de Si à la case 12, la corde de Mi aigu à la case 10.
        val voicings = Voicings.find(guitar, FretZone.firstFrets(12), Chord(g, ChordQuality.MAJOR), inversion = 0, startString = 3)
        assertEquals(listOf(listOf(12, 12, 10)), voicings.map(::frets))
        assertEquals(listOf(0, 1, 2), voicings.single().roles)
    }

    @Test
    fun firstInversionOfCMajorOnTheThreeHighStrings() {
        // Mi Sol Do : cases 9, 8, 8 (le classique « do sur mi », à la huitième position).
        val voicings = Voicings.find(guitar, FretZone.firstFrets(12), Chord(c, ChordQuality.MAJOR), inversion = 1, startString = 3)
        assertEquals(listOf(listOf(9, 8, 8)), voicings.map(::frets))
        assertEquals(listOf(1, 2, 0), voicings.single().roles)
    }

    @Test
    fun theZoneLimitsTheVoicings() {
        val chord = Chord(g, ChordQuality.MAJOR)
        // Sur 24 cases la même forme revient une octave plus haut ; hors de la zone, plus rien.
        assertEquals(listOf(listOf(12, 12, 10), listOf(24, 24, 22)), Voicings.find(guitar, FretZone.firstFrets(24), chord, 0, 3).map(::frets))
        assertEquals(emptyList(), Voicings.find(guitar, FretZone(1, 9), chord, 0, 3).map(::frets))
        assertEquals(emptyList(), Voicings.find(guitar, FretZone(13, 20), chord, 0, 3).map(::frets))
    }

    @Test
    fun everyVoicingIsClosedAscendingPlayableAndInTheZone() {
        val zone = FretZone.firstFrets(24)
        for (instrument in Instrument.entries) {
            val board = Fretboard(instrument)
            for (quality in ChordQuality.entries) {
                for (root in 0..11) {
                    val chord = Chord(root, quality)
                    for (inversion in 0 until quality.toneCount) {
                        for (start in Voicings.stringSets(instrument, quality.toneCount)) {
                            for (voicing in Voicings.find(board, zone, chord, inversion, start)) {
                                val label = "${instrument.name} ${quality.name} $root inv$inversion corde$start"
                                assertEquals(quality.toneCount, voicing.positions.size, label)
                                assertEquals(List(quality.toneCount) { start + it }, voicing.positions.map { it.string }, label)
                                // Les notes sont celles de l'accord, dans l'ordre du renversement.
                                assertEquals(
                                    voicing.roles.map { chord.pitchClasses[it] },
                                    voicing.pitches.map { it.pitchClass },
                                    label,
                                )
                                assertEquals(List(quality.toneCount) { (inversion + it) % quality.toneCount }, voicing.roles, label)
                                assertTrue(voicing.pitches.zipWithNext().all { (a, b) -> a.midi < b.midi }, "$label : monte")
                                assertTrue(voicing.pitches.last().midi - voicing.pitches.first().midi < 12, "$label : serré")
                                assertTrue(voicing.maxFret - voicing.minFret <= Voicings.MAX_FRET_SPAN, "$label : jouable")
                                assertTrue(voicing.positions.all { it.fret in zone }, "$label : dans la zone")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun voicingsAreSortedFromTheLowestFret() {
        val voicings = Voicings.find(guitar, FretZone.firstFrets(24), Chord(c, ChordQuality.MAJOR), 0, 3)
        assertEquals(voicings.sortedBy { it.minFret }.map(::frets), voicings.map(::frets))
    }

    @Test
    fun everyTriadHasAShapeSomewhereOnTwoOctavesOfTheGuitar() {
        // Sur 24 cases, toute triade, tout renversement, tout groupe de trois cordes voisines a au moins une position jouable.
        val zone = FretZone.firstFrets(24)
        for (quality in ChordQuality.entries.filter { !it.isSeventh }) {
            for (root in 0..11) {
                for (inversion in 0..2) {
                    for (start in Voicings.stringSets(Instrument.GUITAR_6, 3)) {
                        assertTrue(
                            Voicings.find(guitar, zone, Chord(root, quality), inversion, start).isNotEmpty(),
                            "${quality.name} $root inv$inversion corde$start",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun stringSetsOfEachInstrument() {
        assertEquals(listOf(0, 1, 2, 3), Voicings.stringSets(Instrument.GUITAR_6, 3))
        assertEquals(listOf(0, 1, 2), Voicings.stringSets(Instrument.GUITAR_6, 4))
        assertEquals(listOf(0, 1, 2, 3, 4), Voicings.stringSets(Instrument.GUITAR_7, 3))
        assertEquals(listOf(0, 1), Voicings.stringSets(Instrument.BASS_4, 3))
        assertEquals(listOf(0), Voicings.stringSets(Instrument.BASS_4, 4))
        assertEquals(listOf(0, 1), Voicings.stringSets(Instrument.BASS_5, 4))
    }

    @Test
    fun invalidArgumentsAreRejected() {
        val chord = Chord(c, ChordQuality.MAJOR)
        assertFailsWith<IllegalArgumentException> { Voicings.find(guitar, FretZone.DEFAULT, chord, 3, 0) }
        assertFailsWith<IllegalArgumentException> { Voicings.find(guitar, FretZone.DEFAULT, chord, 0, 4) }
        assertFailsWith<IllegalArgumentException> { Chord(12, ChordQuality.MAJOR) }
    }

    @Test
    fun theArpeggioMapHoldsEveryChordTone() {
        val zone = FretZone.firstFrets(12)
        val notes = Voicings.arpeggio(guitar, zone, Chord(c, ChordQuality.MAJOR))
        // Chaque note de l'accord, de la fondamentale à la quinte, apparaît au moins une fois par corde sur 12 cases… ou presque.
        assertTrue(notes.all { guitar.pitchAt(it.position).pitchClass in listOf(0, 4, 7) })
        assertTrue(notes.all { it.position.fret in zone })
        assertEquals(setOf(0, 1, 2), notes.map { it.role }.toSet())
        // Do majeur sur la corde de Sol : sol (0), do (5), mi (9), et le sol de l'octave suivante (12).
        assertEquals(listOf(0, 5, 9, 12), notes.filter { it.position.string == 3 }.map { it.position.fret })
        // Les rôles suivent les notes.
        assertEquals(0, notes.single { it.position == FretPosition(1, 3) }.role)
    }

    @Test
    fun anArpeggioInAFiveFretBoxHasTwoOrThreeNotesPerString() {
        val notes = Voicings.arpeggio(guitar, FretZone(5, 9), Chord(c, ChordQuality.MAJOR))
        for (string in 0..5) {
            val perString = notes.count { it.position.string == string }
            assertTrue(perString in 1..3, "corde $string : $perString notes")
        }
    }

    @Test
    fun arpeggioPitchesAreSortedAndUnique() {
        val zone = FretZone(5, 9)
        val notes = Voicings.arpeggio(guitar, zone, Chord(c, ChordQuality.MAJOR))
        val pitches = Voicings.ascendingPitches(guitar, notes)
        assertEquals(pitches.map { it.midi }.sorted(), pitches.map { it.midi })
        assertEquals(pitches.size, pitches.toSet().size)
        // Le même son joué sur deux cordes ne compte qu'une fois : la zone en contient donc moins que de notes.
        assertTrue(pitches.size <= notes.size)
        assertTrue(pitches.all { it.pitchClass in listOf(0, 4, 7) })
    }
}
