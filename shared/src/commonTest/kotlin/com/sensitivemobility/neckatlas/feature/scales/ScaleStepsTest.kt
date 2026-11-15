package com.sensitivemobility.neckatlas.feature.scales

import com.sensitivemobility.neckatlas.domain.scale.ScaleType
import kotlin.test.Test
import kotlin.test.assertEquals

class ScaleStepsTest {
    @Test
    fun steppingMovesToTheNextScaleInTheListAndWrapsAround() {
        assertEquals(ScaleType.DORIAN, stepScaleType(ScaleType.MAJOR, 1))
        assertEquals(ScaleType.MAJOR, stepScaleType(ScaleType.DORIAN, -1))
        assertEquals(ScaleType.MAJOR, stepScaleType(ScaleType.BLUES, 1))
        assertEquals(ScaleType.BLUES, stepScaleType(ScaleType.MAJOR, -1))
    }

    @Test
    fun aFullTurnComesBackToTheSameScale() {
        for (type in ScaleType.entries) {
            assertEquals(type, stepScaleType(type, ScaleType.entries.size))
            assertEquals(type, stepScaleType(type, -ScaleType.entries.size))
        }
    }

    @Test
    fun aPositionChoiceStaysInRangeWhenTheScaleHasFewerPositions() {
        assertEquals(-1, clampPosition(-1, 7)) // toute la zone
        assertEquals(3, clampPosition(3, 7))
        assertEquals(4, clampPosition(6, 5)) // on passe d'une gamme de sept notes à une pentatonique
        assertEquals(-1, clampPosition(2, 0))
    }
}
