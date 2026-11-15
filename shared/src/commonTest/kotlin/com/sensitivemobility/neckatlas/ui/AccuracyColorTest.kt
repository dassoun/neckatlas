package com.sensitivemobility.neckatlas.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Les tranches de réussite (vert, turquoise, ambre, rouge) sont documentées dans le README et lues par la carte de chaleur, les
 * cases de statistiques et les anneaux de score : leurs seuils ne doivent pas bouger sans qu'on le veuille.
 */
class AccuracyColorTest {
    private val accents = AccentColors(Color(0xFF00AA99), Color.Black, Color(0xFF0066CC), Color.White)

    private fun colorOf(accuracy: Double?, dark: Boolean = false): Color? {
        val scheme = if (dark) darkColorScheme() else lightColorScheme()
        return accuracyColor(scheme, accents, accuracy)
    }

    @Test
    fun neverAskedHasNoColour() {
        assertNull(colorOf(null))
    }

    @Test
    fun ninetyPercentAndAboveIsGreen() {
        assertEquals(CorrectColor, colorOf(1.0))
        assertEquals(CorrectColor, colorOf(0.9))
    }

    @Test
    fun seventyToEightyNineIsTheTeal() {
        assertEquals(accents.teal, colorOf(0.89))
        assertEquals(accents.teal, colorOf(0.7))
    }

    @Test
    fun fiftyToSixtyNineIsAmber() {
        assertEquals(lightColorScheme().tertiary, colorOf(0.69))
        assertEquals(lightColorScheme().tertiary, colorOf(0.5))
    }

    @Test
    fun belowFiftyIsRed() {
        assertEquals(lightColorScheme().error, colorOf(0.49))
        assertEquals(lightColorScheme().error, colorOf(0.0))
    }

    @Test
    fun theTealDoesNotFollowTheBrandColour() {
        // C'est la raison d'être d'AccentColors : le thème peut changer de couleur de marque sans toucher à cette tranche.
        assertEquals(colorOf(0.8, dark = false), colorOf(0.8, dark = true))
    }
}
