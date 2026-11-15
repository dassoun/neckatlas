package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Pitch
import com.sensitivemobility.neckatlas.domain.Timbre
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SampleBankTest {
    private val manifest = "rate 22050\nguitar 57 2\nguitar 60 3\nbass 28 1\n"

    /** Un « dossier de ressources » en mémoire, qui note ce qu'on lui demande. */
    private class FakeLoader(private val files: Map<String, ByteArray>) : SampleLoader {
        val requests = ArrayList<String>()

        override suspend fun read(path: String): ByteArray {
            requests += path
            return files[path] ?: throw IllegalStateException("fichier absent : $path")
        }
    }

    private fun bytes(samples: ShortArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        for ((i, s) in samples.withIndex()) {
            out[2 * i] = s.toInt().toByte()
            out[2 * i + 1] = (s.toInt() shr 8).toByte()
        }
        return out
    }

    /**
     * Les fichiers de l'inventaire ci-dessus : chaque échantillon est une sinusoïde à la hauteur exacte de sa note. Les
     * variantes d'une même note ne se distinguent que par leur force, pour qu'on puisse les reconnaître.
     */
    private fun files(manifestText: String = manifest): Map<String, ByteArray> {
        val files = hashMapOf("files/samples/manifest.txt" to manifestText.encodeToByteArray())
        for ((name, midi, variants) in listOf(Triple("guitar", 57, 2), Triple("guitar", 60, 3), Triple("bass", 28, 1))) {
            for (v in 1..variants) {
                files["files/samples/${name}_${midi}_$v.pcm"] = bytes(sine(Pitch(midi).frequencyHz, 22050, 3.0, amplitude = 0.2 + 0.1 * v))
            }
        }
        return files
    }

    private fun assertInTune(sound: ShortArray, pitch: Pitch, toleranceCents: Double = 2.0) {
        val measured = estimateFrequency(sound, pitch.frequencyHz, minWindow = 16_384)
        assertTrue(abs(cents(measured, pitch.frequencyHz)) < toleranceCents, "midi ${pitch.midi} : mesuré $measured Hz")
    }

    @Test
    fun picksTheNearestRecordedNoteAndTunesIt() {
        val loader = FakeLoader(files())
        val bank = SampleBank(loader)
        val sound = runNow { bank.render(Pitch(59), Timbre.GUITAR) }
        assertEquals(listOf("files/samples/manifest.txt", "files/samples/guitar_60_1.pcm"), loader.requests)
        assertInTune(sound, Pitch(59))
        assertInTune(runNow { bank.render(Pitch(55), Timbre.GUITAR) }, Pitch(55)) // 2 demi-tons sous l'échantillon 57
        assertInTune(runNow { bank.render(Pitch(30), Timbre.BASS) }, Pitch(30)) // 2 demi-tons au-dessus de l'échantillon 28
    }

    @Test
    fun playsTheVariantsOfANoteInTurn() {
        val loader = FakeLoader(files())
        val bank = SampleBank(loader)
        repeat(4) { runNow { bank.render(Pitch(60), Timbre.GUITAR) } }
        // Variantes 1, 2, 3 puis retour à la 1, déjà en mémoire.
        assertEquals(
            listOf("manifest.txt", "guitar_60_1.pcm", "guitar_60_2.pcm", "guitar_60_3.pcm").map { "files/samples/$it" },
            loader.requests,
        )
    }

    @Test
    fun variantsAreCountedNoteByNote() {
        val loader = FakeLoader(files())
        val bank = SampleBank(loader)
        runNow { bank.render(Pitch(57), Timbre.GUITAR) }
        runNow { bank.render(Pitch(60), Timbre.GUITAR) }
        runNow { bank.render(Pitch(57), Timbre.GUITAR) }
        assertEquals(
            listOf("manifest.txt", "guitar_57_1.pcm", "guitar_60_1.pcm", "guitar_57_2.pcm").map { "files/samples/$it" },
            loader.requests,
        )
    }

    @Test
    fun readsEachFileOnlyOnce() {
        val loader = FakeLoader(files())
        val bank = SampleBank(loader)
        repeat(10) { runNow { bank.render(Pitch(60), Timbre.GUITAR) } }
        assertEquals(1 + 3, loader.requests.size)
    }

    @Test
    fun variantsSoundDifferentAndTheCycleComesBackToTheFirst() {
        val bank = SampleBank(FakeLoader(files()))
        val first = runNow { bank.render(Pitch(60), Timbre.GUITAR) }
        val second = runNow { bank.render(Pitch(60), Timbre.GUITAR) }
        runNow { bank.render(Pitch(60), Timbre.GUITAR) }
        assertTrue(!first.contentEquals(second), "deux variantes d'une même note doivent différer")
        // Au quatrième appel on revient à la première variante : même son que le premier.
        assertContentEquals(first, runNow { bank.render(Pitch(60), Timbre.GUITAR) })
    }

    @Test
    fun fallsBackToTheSynthWhenSampleFilesAreMissing() {
        val onlyTheManifest = mapOf("files/samples/manifest.txt" to manifest.encodeToByteArray())
        val causes = ArrayList<Throwable>()
        val bank = SampleBank(FakeLoader(onlyTheManifest), onFallback = { causes += it })
        assertFailsWith<IllegalStateException> { runNow { bank.render(Pitch(60), Timbre.GUITAR) } }

        val sound = runNow { bank.voice(Pitch(60), Timbre.GUITAR) }
        assertEquals(1, causes.size)
        assertContentEquals(PluckSynth.render(Pitch(60).frequencyHz, Timbre.GUITAR), sound)
    }

    @Test
    fun fallsBackToTheSynthWhenTheInventoryIsMissingOrBroken() {
        val brokenResources = listOf(
            emptyMap<String, ByteArray>(),
            mapOf("files/samples/manifest.txt" to "n'importe quoi".encodeToByteArray()),
        )
        for (broken in brokenResources) {
            var fallbacks = 0
            val bank = SampleBank(FakeLoader(broken), onFallback = { fallbacks++ })
            val sound = runNow { bank.voice(Pitch(45), Timbre.BASS) }
            assertEquals(1, fallbacks)
            assertInTune(sound, Pitch(45), toleranceCents = 6.0)
        }
    }

    @Test
    fun fallsBackWhenTheInstrumentHasNoSamples() {
        var fallbacks = 0
        val bank = SampleBank(FakeLoader(files("rate 22050\nguitar 60 1\n")), onFallback = { fallbacks++ })
        assertEquals((NOTE_SECONDS * SAMPLE_RATE).toInt(), runNow { bank.voice(Pitch(40), Timbre.BASS) }.size)
        assertEquals(1, fallbacks)
    }

    @Test
    fun usesTheSamplesWithoutFallingBackWhenAllIsWell() {
        var fallbacks = 0
        val bank = SampleBank(FakeLoader(files()), onFallback = { fallbacks++ })
        runNow { bank.voice(Pitch(60), Timbre.GUITAR) }
        runNow { bank.voice(Pitch(28), Timbre.BASS) }
        assertEquals(0, fallbacks)
    }

    @Test
    fun rejectsACorruptSampleFile() {
        val corrupt = files() + ("files/samples/guitar_60_1.pcm" to ByteArray(7))
        assertFailsWith<IllegalArgumentException> { runNow { SampleBank(FakeLoader(corrupt)).render(Pitch(60), Timbre.GUITAR) } }
    }

    @Test
    fun aCancelledCoroutineIsNotTurnedIntoAFallback() {
        val loader = SampleLoader { throw CancellationException("annulé") }
        var fallbacks = 0
        val bank = SampleBank(loader, onFallback = { fallbacks++ })
        assertFailsWith<CancellationException> { runNow { bank.voice(Pitch(60), Timbre.GUITAR) } }
        assertEquals(0, fallbacks)
    }
}
