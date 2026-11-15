package com.sensitivemobility.neckatlas.domain.tuner

import com.sensitivemobility.neckatlas.domain.Instrument
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TunerTest {
    private val rate = 44_100
    private val frames = 8192

    /** Un son de [hz] fait de ses harmoniques ([amplitudes] : la 1re est la fondamentale), à la phase [phase]. */
    private fun tone(hz: Double, amplitudes: List<Double> = listOf(0.4), phase: Double = 0.3): FloatArray = FloatArray(frames) { i ->
        var v = 0.0
        amplitudes.forEachIndexed { h, a -> v += a * sin(2 * PI * hz * (h + 1) * i / rate + phase * (h + 1)) }
        v.toFloat()
    }

    private fun cents(a: Double, b: Double) = Tuning.cents(a, b)

    private fun detect(samples: FloatArray, instrument: Instrument): Double? {
        val range = Tuning.searchRange(instrument)
        return PitchDetector.detect(samples, rate, range.start, range.endInclusive)
    }

    @Test
    fun everyOpenStringOfEveryInstrumentIsFoundWithinOneCent() {
        for (instrument in Instrument.entries) {
            for (open in instrument.openStrings) {
                val hz = open.frequencyHz
                val found = assertNotNull(detect(tone(hz), instrument), "$instrument $hz Hz")
                assertTrue(abs(cents(found, hz)) < 1.0, "$instrument : attendu $hz, trouvé $found")
            }
        }
    }

    @Test
    fun aDetunedStringIsMeasuredAsDetuned() {
        val target = Instrument.GUITAR_6.openStrings[3].frequencyHz // Sol
        for (offset in listOf(-30.0, -12.0, -5.0, 7.0, 25.0)) {
            val hz = target * 2.0.pow(offset / 1200.0)
            val reading = Tuning.reading(Instrument.GUITAR_6, detect(tone(hz), Instrument.GUITAR_6)!!)
            assertEquals(3, reading.string)
            assertTrue(abs(reading.cents - offset) < 1.0, "décalage $offset : mesuré ${reading.cents}")
        }
    }

    @Test
    fun aBassStringWithAWeakFundamentalIsNotTakenForItsOctave() {
        // Mi grave de basse (41,2 Hz) : presque tout l'énergie est dans les harmoniques 2 et 3.
        val hz = Instrument.BASS_4.openStrings[0].frequencyHz
        val found = assertNotNull(detect(tone(hz, listOf(0.08, 0.35, 0.3, 0.12, 0.05)), Instrument.BASS_4))
        assertTrue(abs(cents(found, hz)) < 2.0, "attendu $hz, trouvé $found")
    }

    @Test
    fun aGuitarStringRichInHarmonicsKeepsItsFundamental() {
        val hz = Instrument.GUITAR_6.openStrings[1].frequencyHz // La 110
        val found = assertNotNull(detect(tone(hz, listOf(0.25, 0.3, 0.25, 0.15, 0.1, 0.05)), Instrument.GUITAR_6))
        assertTrue(abs(cents(found, hz)) < 2.0, "attendu $hz, trouvé $found")
    }

    @Test
    fun silenceAndNoiseGiveNothing() {
        assertNull(detect(FloatArray(frames), Instrument.GUITAR_6))
        assertNull(detect(FloatArray(frames) { 0.0005f }, Instrument.GUITAR_6))
        val random = Random(3)
        val noise = FloatArray(frames) { (random.nextFloat() - 0.5f) * 0.6f }
        assertNull(detect(noise, Instrument.GUITAR_6))
    }

    @Test
    fun aNoteWithBackgroundNoiseIsStillFound() {
        val hz = Instrument.GUITAR_6.openStrings[5].frequencyHz
        val random = Random(9)
        val samples = tone(hz, listOf(0.4, 0.15)).also { for (i in it.indices) it[i] += (random.nextFloat() - 0.5f) * 0.08f }
        val found = assertNotNull(detect(samples, Instrument.GUITAR_6))
        assertTrue(abs(cents(found, hz)) < 3.0)
    }

    @Test
    fun theNearestStringIsMeasuredInCentsNotInHertz() {
        val guitar = Instrument.GUITAR_6
        assertEquals(0, Tuning.nearestString(guitar, 82.0))
        assertEquals(5, Tuning.nearestString(guitar, 331.0))
        // 96 Hz est plus près de 110 (La, -250 cents) que de 82,4 (Mi, +265 cents) : en Hz l'écart au Mi serait le plus faible.
        assertEquals(1, Tuning.nearestString(guitar, 96.0))
    }

    @Test
    fun aChosenStringOverridesTheNearestOne() {
        val reading = Tuning.reading(Instrument.GUITAR_6, 112.0, target = 2) // vers le Ré (146,8)
        assertEquals(2, reading.string)
        assertTrue(reading.cents < -400)
        assertTrue(!reading.inTune)
        assertTrue(Tuning.reading(Instrument.GUITAR_6, 110.0).inTune)
    }

    @Test
    fun theSmootherTakesTheMedianAndForgetsAfterSilence() {
        val smoother = PitchSmoother(size = 5, maxMisses = 3)
        assertNull(smoother.add(null))
        smoother.add(110.0)
        smoother.add(110.2)
        // Une mesure aberrante (harmonique) ne déplace pas la médiane.
        val median = smoother.add(220.0)
        assertEquals(110.2, median!!, 0.01)
        smoother.add(110.1)
        assertEquals(110.1, smoother.add(110.1)!!, 0.2)
        smoother.add(null)
        smoother.add(null)
        assertNull(smoother.add(null))
    }

    @Test
    fun aNewNoteTakesOverAfterAFewMeasurements() {
        val smoother = PitchSmoother()
        repeat(5) { smoother.add(110.0) }
        assertEquals(110.0, smoother.add(146.8)!!, 0.01) // une mesure isolée ne change rien
        smoother.add(146.8)
        assertEquals(146.8, smoother.add(146.8)!!, 0.01)
    }

    @Test
    fun theReferencePitchMovesTheTargets() {
        // Une guitare accordée sur un La à 442 Hz est juste pour un accordeur à 442, trop haute de ~8 cents pour un accordeur à 440.
        val a = Instrument.GUITAR_6.openStrings[1]
        val hz = Tuning.frequencyOf(a, 442.0) // 110,5 Hz
        assertTrue(Tuning.reading(Instrument.GUITAR_6, hz, referenceHz = 442.0).inTune)
        val at440 = Tuning.reading(Instrument.GUITAR_6, hz, referenceHz = 440.0)
        assertTrue(!at440.inTune && at440.cents > 7.0 && at440.cents < 9.0, "${at440.cents}")
    }

    @Test
    fun aDyingNoteIsStillFound() {
        // La note s'éteint : l'amplitude passe de 0,3 à 0,004 sur la fenêtre, avec un peu de bruit.
        val hz = Instrument.GUITAR_6.openStrings[2].frequencyHz
        val random = Random(4)
        val samples = FloatArray(frames) { i ->
            val envelope = 0.3 * kotlin.math.exp(-4.5 * i / frames)
            (envelope * (sin(2 * PI * hz * i / rate) + 0.4 * sin(4 * PI * hz * i / rate)) + (random.nextFloat() - 0.5f) * 0.002).toFloat()
        }
        val found = assertNotNull(detect(samples, Instrument.GUITAR_6))
        assertTrue(abs(cents(found, hz)) < 3.0)
    }

    @Test
    fun aVeryQuietNoteIsStillFound() {
        // Une électrique non amplifiée au micro d'un téléphone : une valeur efficace de l'ordre de 0,001, dans un peu de bruit.
        val hz = Instrument.GUITAR_6.openStrings[0].frequencyHz
        val random = Random(8)
        val samples = tone(hz, listOf(0.0015, 0.001)).also { for (i in it.indices) it[i] += (random.nextFloat() - 0.5f) * 0.0004f }
        assertTrue(PitchDetector.rms(samples) < 0.002f)
        val found = assertNotNull(detect(samples, Instrument.GUITAR_6))
        assertTrue(abs(cents(found, hz)) < 2.0)
    }
}
