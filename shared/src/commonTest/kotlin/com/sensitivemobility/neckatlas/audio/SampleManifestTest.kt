package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Timbre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SampleManifestTest {
    private val text = """
        # commentaire
        rate 22050

        guitar 40 3
        guitar 45 2
        guitar 60 3
        bass 28 1
        bass 39 1
    """.trimIndent()

    @Test
    fun readsTheRateAndTheNotes() {
        val manifest = SampleManifest.parse(text)
        assertEquals(22050, manifest.sampleRate)
        assertEquals(3, manifest.nearest(Timbre.GUITAR, 40)?.variants)
        assertEquals(2, manifest.nearest(Timbre.GUITAR, 45)?.variants)
        assertEquals(1, manifest.nearest(Timbre.BASS, 28)?.variants)
    }

    @Test
    fun nearestIsTheClosestRecordedNote() {
        val manifest = SampleManifest.parse(text)
        assertEquals(45, manifest.nearest(Timbre.GUITAR, 44)?.midi)
        assertEquals(40, manifest.nearest(Timbre.GUITAR, 41)?.midi)
        assertEquals(45, manifest.nearest(Timbre.GUITAR, 50)?.midi) // 5 demi-tons de 45, 10 de 60
        assertEquals(60, manifest.nearest(Timbre.GUITAR, 100)?.midi)
        assertEquals(40, manifest.nearest(Timbre.GUITAR, 0)?.midi)
        assertEquals(28, manifest.nearest(Timbre.BASS, 23)?.midi)
    }

    @Test
    fun aTieGoesToTheLowerNote() {
        val manifest = SampleManifest.parse("rate 22050\nguitar 44 1\nguitar 40 1")
        assertEquals(40, manifest.nearest(Timbre.GUITAR, 42)?.midi)
    }

    @Test
    fun anInstrumentWithoutSamplesHasNoNearestNote() {
        val manifest = SampleManifest.parse("rate 22050\nguitar 40 1")
        assertNull(manifest.nearest(Timbre.BASS, 40))
    }

    @Test
    fun rejectsIncomprehensibleInventories() {
        val bad = listOf(
            "guitar 40 3", // pas de fréquence
            "rate 22050", // aucune note
            "rate abc\nguitar 40 1",
            "rate 0\nguitar 40 1",
            "rate 22050 1\nguitar 40 1",
            "rate 22050\nviolin 40 1",
            "rate 22050\nguitar 200 1",
            "rate 22050\nguitar 40 0",
            "rate 22050\nguitar 40",
            "rate 22050\nguitar 40 1 2",
        )
        for (text in bad) assertFailsWith<IllegalArgumentException>(text) { SampleManifest.parse(text) }
    }

    @Test
    fun errorMessageNamesTheLine() {
        val error = assertFailsWith<IllegalArgumentException> { SampleManifest.parse("rate 22050\n\nguitar quarante 1") }
        assertTrue("ligne 3" in error.message.orEmpty(), error.message)
    }
}
