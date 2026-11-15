package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.Timbre
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PluckSynthTest {
    private fun rms(samples: ShortArray, fromSeconds: Double, toSeconds: Double): Double {
        val from = (fromSeconds * SAMPLE_RATE).toInt()
        val to = (toSeconds * SAMPLE_RATE).toInt()
        var sum = 0.0
        for (i in from until to) sum += (samples[i] / 32768.0) * (samples[i] / 32768.0)
        return sqrt(sum / (to - from))
    }

    @Test
    fun lengthMatchesDuration() {
        assertEquals((NOTE_SECONDS * SAMPLE_RATE).toInt(), PluckSynth.render(220.0, Timbre.GUITAR).size)
        assertEquals(SAMPLE_RATE, PluckSynth.render(220.0, Timbre.GUITAR, durationSeconds = 1.0).size)
    }

    @Test
    fun guitarNotesAreInTune() {
        // De la corde grave à vide jusqu'aux hautes notes de la guitare 7 cordes à 24 cases.
        for (hz in listOf(82.41, 110.0, 196.0, 329.63, 523.25, 659.26, 1046.5)) {
            val measured = estimateFrequency(PluckSynth.render(hz, Timbre.GUITAR), hz)
            assertTrue(abs(cents(measured, hz)) < 6.0, "$hz Hz mesuré à $measured Hz")
        }
    }

    @Test
    fun bassNotesAreInTune() {
        for (hz in listOf(30.87, 41.2, 55.0, 98.0)) {
            val measured = estimateFrequency(PluckSynth.render(hz, Timbre.BASS), hz)
            assertTrue(abs(cents(measured, hz)) < 6.0, "$hz Hz mesuré à $measured Hz")
        }
    }

    @Test
    fun everyStringOfEveryInstrumentCanBeRendered() {
        for (instrument in Instrument.entries) {
            for (fret in listOf(0, 12, 24)) {
                for (open in instrument.openStrings) {
                    val pitch = open + fret
                    val samples = PluckSynth.render(pitch.frequencyHz, instrument.timbre, durationSeconds = 0.5)
                    assertTrue(samples.any { it.toInt() != 0 }, "${instrument.name} midi ${pitch.midi}")
                }
            }
        }
    }

    @Test
    fun theNoteDiesAwayAndEndsInSilence() {
        val samples = PluckSynth.render(196.0, Timbre.GUITAR)
        assertTrue(rms(samples, 0.1, 0.3) > 5 * rms(samples, NOTE_SECONDS - 0.4, NOTE_SECONDS - 0.2), "la note doit s'éteindre")
        assertEquals(0, samples.last().toInt())
        assertEquals(0, samples.first().toInt())
    }

    @Test
    fun levelIsConsistentAndNeverClips() {
        for (hz in listOf(41.2, 196.0, 659.26)) {
            val peak = PluckSynth.render(hz, Timbre.GUITAR).maxOf { abs(it.toInt()) }
            assertTrue(peak in 28000..30000, "$hz Hz : crête $peak")
        }
    }

    @Test
    fun sameSeedGivesTheSameSound() {
        assertContentEquals(PluckSynth.render(220.0, Timbre.GUITAR, seed = 3), PluckSynth.render(220.0, Timbre.GUITAR, seed = 3))
        assertTrue(!PluckSynth.render(220.0, Timbre.GUITAR, seed = 3).contentEquals(PluckSynth.render(220.0, Timbre.GUITAR, seed = 4)))
    }

    @Test
    fun rejectsFrequenciesOutOfRange() {
        assertFailsWith<IllegalArgumentException> { PluckSynth.render(10.0, Timbre.GUITAR) }
        assertFailsWith<IllegalArgumentException> { PluckSynth.render(5000.0, Timbre.BASS) }
    }
}
