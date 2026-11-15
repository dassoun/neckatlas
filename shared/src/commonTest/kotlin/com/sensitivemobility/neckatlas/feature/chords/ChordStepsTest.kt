package com.sensitivemobility.neckatlas.feature.chords

import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import kotlin.test.Test
import kotlin.test.assertEquals

class ChordStepsTest {
    @Test
    fun rootStepsAroundTheOctave() {
        assertEquals(1, stepRoot(0, 1))
        assertEquals(11, stepRoot(0, -1)) // avant Do vient Si
        assertEquals(0, stepRoot(11, 1)) // après Si vient Do
        assertEquals(5, stepRoot(5, 12))
        assertEquals(0, stepRoot(3, -15))
    }

    @Test
    fun everyRootIsReachableBySteppingForward() {
        val visited = generateSequence(0) { stepRoot(it, 1) }.take(12).toSet()
        assertEquals((0..11).toSet(), visited)
    }

    @Test
    fun qualityStepsAroundTheList() {
        val all = ChordQuality.entries
        assertEquals(all[1], stepQuality(all.first(), 1))
        assertEquals(all.last(), stepQuality(all.first(), -1))
        assertEquals(all.first(), stepQuality(all.last(), 1))
    }

    @Test
    fun everyQualityIsReachableAndStepsAreReversible() {
        val all = ChordQuality.entries
        val visited = generateSequence(all.first()) { stepQuality(it, 1) }.take(all.size).toSet()
        assertEquals(all.toSet(), visited)
        for (quality in all) assertEquals(quality, stepQuality(stepQuality(quality, 1), -1))
    }
}
