package com.sensitivemobility.neckatlas.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SampleShifterTest {
    private val sourceRate = 22_050

    private fun source(hz: Double, seconds: Double = 3.0) = sine(hz, sourceRate, seconds)

    /** Accorde une sinusoïde sans décroissance ajoutée : ces tests ne regardent que la hauteur. */
    private fun shifted(hz: Double, semitones: Int, seconds: Double = 3.0, decayDb: Double = 0.0) =
        SampleShifter.shift(source(hz, seconds), sourceRate, semitones, decayDb = decayDb)

    private fun assertPitch(sound: ShortArray, expectedHz: Double, toleranceCents: Double = 2.0) {
        val measured = estimateFrequency(sound, expectedHz, minWindow = 16_384)
        assertTrue(abs(cents(measured, expectedHz)) < toleranceCents, "attendu $expectedHz Hz, mesuré $measured Hz")
    }

    /** Niveau efficace du son entre deux instants, en dB (0 dB = pleine échelle). */
    private fun levelDb(sound: ShortArray, fromSeconds: Double, toSeconds: Double): Double {
        val from = (fromSeconds * SAMPLE_RATE).toInt()
        val to = (toSeconds * SAMPLE_RATE).toInt()
        var sum = 0.0
        for (i in from until to) sum += (sound[i] / 32768.0) * (sound[i] / 32768.0)
        return 10 * log10(sum / (to - from))
    }

    @Test
    fun keepsThePitchWhenNotShifted() {
        val sound = shifted(220.0, 0)
        assertPitch(sound, 220.0)
        assertEquals((NOTE_SECONDS * SAMPLE_RATE).toInt(), sound.size)
    }

    @Test
    fun anOctaveUpDoublesTheFrequency() {
        assertPitch(shifted(220.0, 12), 440.0)
    }

    @Test
    fun goingDownLowersTheFrequency() {
        assertPitch(shifted(220.0, -5), 220.0 * exp(-5 * ln(2.0) / 12))
    }

    @Test
    fun everySemitoneInTheUsefulRangeIsInTune() {
        for (semitones in -14..28) {
            val expected = 262.0 * exp(semitones * ln(2.0) / 12)
            // Une source assez longue : les notes très aiguës sont lues vite et consomment beaucoup d'échantillons.
            assertPitch(shifted(262.0, semitones, seconds = 6.0), expected)
        }
    }

    @Test
    fun pitchIsStillRightWithTheDefaultDecay() {
        for (semitones in listOf(-2, 0, 1, 7)) {
            val expected = 262.0 * exp(semitones * ln(2.0) / 12)
            assertPitch(shifted(262.0, semitones, decayDb = SampleShifter.DECAY_DB), expected, toleranceCents = 3.0)
        }
    }

    @Test
    fun theSoundEndsWhenTheSampleEnds() {
        val oneSecond = source(220.0, seconds = 1.0)
        assertTrue(SampleShifter.shift(oneSecond, sourceRate, 0).size in 44_000..44_100)
        // Une octave plus haut, l'enregistrement est lu deux fois plus vite : il dure deux fois moins longtemps.
        assertTrue(SampleShifter.shift(oneSecond, sourceRate, 12).size in 22_000..22_100)
    }

    @Test
    fun entersAndLeavesInSilenceAndKeepsItsLevel() {
        val input = source(220.0)
        val sound = SampleShifter.shift(input, sourceRate, 3)
        assertEquals(0, sound.first().toInt())
        assertEquals(0, sound.last().toInt())
        val inputPeak = input.maxOf { abs(it.toInt()) }
        val peak = sound.maxOf { abs(it.toInt()) }
        assertTrue(peak in (inputPeak * 0.97).toInt()..(inputPeak * 1.03).toInt(), "crête $peak pour $inputPeak")
    }

    @Test
    fun addsTheRequestedDecay() {
        val input = source(220.0, seconds = 3.0) // niveau constant : tout ce qu'on mesure vient de la décroissance ajoutée
        val sound = SampleShifter.shift(input, sourceRate, 0, maxSeconds = 1.5, decayDb = 30.0)
        // Entre le milieu de la fenêtre 0,3-0,4 s et celle de 1,0-1,1 s : 0,7 s sur 1,5 s, soit 14 dB sur 30.
        val drop = levelDb(sound, 0.3, 0.4) - levelDb(sound, 1.0, 1.1)
        assertTrue(drop in 13.0..15.0, "baisse de $drop dB, attendu environ 14")
    }

    @Test
    fun withoutDecayTheLevelStaysConstant() {
        val sound = SampleShifter.shift(source(220.0), sourceRate, 0, maxSeconds = 1.5, decayDb = 0.0)
        val drop = levelDb(sound, 0.3, 0.4) - levelDb(sound, 1.0, 1.1)
        assertTrue(abs(drop) < 0.3, "baisse de $drop dB, attendu aucune")
    }

    @Test
    fun theDecayIsSpreadOverTheNoteWhateverItsLength() {
        // Une note qui s'arrête tôt (échantillon court) s'éteint quand même de la même quantité sur sa durée.
        val shortSample = source(220.0, seconds = 0.8)
        val sound = SampleShifter.shift(shortSample, sourceRate, 0, decayDb = 30.0)
        val length = sound.size / SAMPLE_RATE.toDouble()
        val drop = levelDb(sound, 0.05 * length, 0.15 * length) - levelDb(sound, 0.35 * length, 0.45 * length)
        // Les milieux des fenêtres sont à 10 % et 40 % de la note : 30 % de la durée, soit 9 dB. Les deux fenêtres sont avant
        // le fondu de fin, qui prend ici 0,35 s sur 0,8 s et commence donc à 56 %.
        assertTrue(drop in 8.0..10.0, "baisse de $drop dB, attendu environ 9")
    }

    @Test
    fun aVeryShortSampleIsRejected() {
        assertFailsWith<IllegalArgumentException> { SampleShifter.shift(ShortArray(3), sourceRate, 0) }
    }
}
