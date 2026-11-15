package com.sensitivemobility.neckatlas.ui

import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Teste la police embarquée (Sora, `composeResources/font/sora.ttf`) : un fichier TrueType intact, à graisse variable, et le
 * texte de sa licence qui l'accompagne (la licence SIL OFL exige que sa copie suive la police).
 */
class FontAssetsTest {
    // Gradle lance les tests depuis le dossier du module (`shared/`).
    private val resources = File("src/commonMain/composeResources")
    private val font = File(resources, "font/sora.ttf")

    private fun tables(data: ByteArray): Map<String, Pair<Int, Int>> {
        val buffer = ByteBuffer.wrap(data)
        val count = buffer.getShort(4).toInt() and 0xFFFF
        return (0 until count).associate { i ->
            val at = 12 + 16 * i
            val tag = String(data, at, 4, Charsets.US_ASCII)
            tag to (buffer.getInt(at + 8) to buffer.getInt(at + 12))
        }
    }

    @Test
    fun theFontIsAnIntactTrueTypeFile() {
        assertTrue(font.isFile, "police introuvable : ${font.absolutePath}")
        val data = font.readBytes()
        assertEquals(0x00010000, ByteBuffer.wrap(data).getInt(0), "signature TrueType attendue")
        val tables = tables(data)
        for (required in listOf("cmap", "glyf", "head", "hhea", "hmtx", "maxp", "name", "OS/2")) {
            assertTrue(required in tables, "table $required absente")
        }
        // Chaque table doit tenir dans le fichier : un téléchargement tronqué le trahit.
        for ((tag, place) in tables) assertTrue(place.first + place.second <= data.size, "table $tag déborde du fichier")
    }

    @Test
    fun theFontHasAVariableWeightAxis() {
        // Sans cet axe, toutes les graisses demandées (moyen, demi-gras, gras) s'afficheraient en graisse normale.
        val data = font.readBytes()
        val fvar = tables(data).getValue("fvar").first
        val buffer = ByteBuffer.wrap(data)
        val axesOffset = buffer.getShort(fvar + 4).toInt() and 0xFFFF
        val axisCount = buffer.getShort(fvar + 8).toInt() and 0xFFFF
        val axisSize = buffer.getShort(fvar + 10).toInt() and 0xFFFF
        val axes = (0 until axisCount).associate { i ->
            val at = fvar + axesOffset + i * axisSize
            String(data, at, 4, Charsets.US_ASCII) to Triple(buffer.getInt(at + 4) / 65536f, buffer.getInt(at + 8) / 65536f, buffer.getInt(at + 12) / 65536f)
        }
        val weight = axes["wght"]
        assertTrue(weight != null, "axe de graisse (wght) absent : $axes")
        assertTrue(weight.first <= 400f && 700f <= weight.third, "axe wght trop étroit pour normal (400) à gras (700) : $weight")
    }

    @Test
    fun theLicenceTravelsWithTheFont() {
        val licence = File(resources, "files/licenses/Sora-OFL.txt")
        assertTrue(licence.isFile, "licence introuvable : ${licence.absolutePath}")
        val text = licence.readText()
        assertTrue("SIL Open Font License, Version 1.1" in text, "ce n'est pas la licence SIL OFL 1.1")
        assertTrue("The Sora Project Authors" in text, "l'avis de droits d'auteur est absent")
    }
}
