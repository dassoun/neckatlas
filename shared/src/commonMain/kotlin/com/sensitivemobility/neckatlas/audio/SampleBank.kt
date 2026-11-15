package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Pitch
import com.sensitivemobility.neckatlas.domain.Timbre
import com.sensitivemobility.neckatlas.shared.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/** Lit un fichier des ressources de l'appli (`files/samples/...`). Sur le téléphone : [Res.readBytes]. */
internal fun interface SampleLoader {
    suspend fun read(path: String): ByteArray
}

/**
 * Les notes de guitare et de basse, à partir de vraies prises de son (voir `tools/make_samples.py`).
 *
 * Pour une note demandée on prend l'échantillon enregistré le plus proche et on l'accorde ([SampleShifter]). Une même note
 * a plusieurs variantes, jouées à tour de rôle pour qu'elle ne sonne pas à l'identique deux fois de suite. Les fichiers sont
 * lus à la première note qui en a besoin, puis gardés en mémoire : rien n'est chargé tant qu'on ne joue pas.
 *
 * Peut être appelée depuis plusieurs coroutines à la fois (plusieurs notes qui partent ensemble).
 */
internal class SampleBank(
    private val loader: SampleLoader,
    /** Prévenue quand [voice] doit se rabattre sur le synthétiseur, avec la cause. */
    private val onFallback: (Throwable) -> Unit = {},
) {
    private class Pick(val data: ShortArray, val sampleRate: Int, val semitones: Int)

    private val lock = Mutex()
    private var manifest: SampleManifest? = null
    private val decoded = HashMap<String, ShortArray>()
    private val played = HashMap<String, Int>()

    /** Le son de la note, accordé sur [pitch]. Lève une exception si les fichiers manquent ou sont abîmés. */
    suspend fun render(pitch: Pitch, timbre: Timbre): ShortArray {
        val pick = lock.withLock {
            val manifest = manifest ?: SampleManifest.parse(loader.read(MANIFEST_PATH).decodeToString()).also { manifest = it }
            val note = manifest.nearest(timbre, pitch.midi) ?: error("Aucun échantillon de $timbre dans l'inventaire")
            val name = "${timbre.name.lowercase()}_${note.midi}"
            val count = played.getOrElse(name) { 0 }
            played[name] = count + 1
            val path = "$SAMPLES_DIR/${name}_${count % note.variants + 1}.pcm"
            val data = decoded.getOrPut(path) { decode(loader.read(path)) }
            Pick(data, manifest.sampleRate, pitch.midi - note.midi)
        }
        return SampleShifter.shift(pick.data, pick.sampleRate, pick.semitones)
    }

    /**
     * Le son à jouer : [render], ou à défaut la corde synthétisée ([PluckSynth]) si une ressource manque ou est illisible.
     * Mieux vaut une note moins réaliste qu'une note muette.
     */
    suspend fun voice(pitch: Pitch, timbre: Timbre): ShortArray = try {
        render(pitch, timbre)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFallback(e)
        PluckSynth.render(pitch.frequencyHz, timbre)
    }

    private fun decode(bytes: ByteArray): ShortArray {
        require(bytes.size % 2 == 0) { "Échantillon de taille impaire (${bytes.size} octets)" }
        return ShortArray(bytes.size / 2) { i ->
            ((bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)).toShort()
        }
    }

    private companion object {
        const val SAMPLES_DIR = "files/samples"
        const val MANIFEST_PATH = "$SAMPLES_DIR/manifest.txt"
    }
}

/** La banque de l'appli : les échantillons embarqués dans les ressources. Ne lit rien avant la première note. */
internal fun createSampleBank(): SampleBank = SampleBank(
    loader = { path -> Res.readBytes(path) },
    onFallback = { println("NeckAtlas : échantillons indisponibles, repli sur le synthétiseur (${it::class.simpleName} : ${it.message})") },
)
