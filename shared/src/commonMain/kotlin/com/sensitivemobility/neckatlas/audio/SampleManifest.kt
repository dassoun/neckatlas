package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Timbre
import kotlin.math.abs

/**
 * L'inventaire des échantillons embarqués : `files/samples/manifest.txt`, écrit par `tools/make_samples.py`. Il dit à quelle
 * fréquence les échantillons sont enregistrés et quelles notes existent, pour que le code n'ait pas à les deviner.
 *
 * Format, une ligne par information (les lignes vides et celles qui commencent par `#` sont ignorées) :
 * ```
 * rate 22050
 * guitar 37 3      instrument, numéro MIDI de la note enregistrée, nombre de variantes
 * ```
 */
internal class SampleManifest private constructor(
    /** Fréquence d'échantillonnage des fichiers, en Hz. */
    val sampleRate: Int,
    private val notes: Map<Timbre, List<Note>>,
) {
    /** Une note enregistrée : son numéro MIDI, et le nombre de prises différentes de cette même note. */
    class Note(val midi: Int, val variants: Int)

    /** La note enregistrée la plus proche de [midi] pour ce timbre (la plus grave si deux sont à égale distance). */
    fun nearest(timbre: Timbre, midi: Int): Note? =
        notes[timbre]?.minWithOrNull(compareBy({ abs(it.midi - midi) }, { it.midi }))

    companion object {
        /** Lit l'inventaire. Lève [IllegalArgumentException] avec le numéro de ligne si une ligne est incompréhensible. */
        fun parse(text: String): SampleManifest {
            var rate = 0
            val notes = HashMap<Timbre, MutableList<Note>>()
            text.lines().forEachIndexed { index, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                val words = line.split(Regex("\\s+"))
                val where = "Inventaire des échantillons, ligne ${index + 1} : « $line »"
                if (words[0] == "rate") {
                    rate = words.getOrNull(1)?.toIntOrNull()?.takeIf { words.size == 2 && it > 0 }
                        ?: throw IllegalArgumentException("$where, fréquence illisible")
                } else {
                    val timbre = Timbre.entries.firstOrNull { it.name.equals(words[0], ignoreCase = true) }
                        ?: throw IllegalArgumentException("$where, instrument inconnu")
                    val midi = words.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..127 }
                    val variants = words.getOrNull(2)?.toIntOrNull()?.takeIf { it >= 1 }
                    if (words.size != 3 || midi == null || variants == null) {
                        throw IllegalArgumentException("$where, attendu : instrument, note MIDI (0 à 127), variantes (1 ou plus)")
                    }
                    notes.getOrPut(timbre) { ArrayList() }.add(Note(midi, variants))
                }
            }
            require(rate > 0) { "Inventaire des échantillons : fréquence (rate) absente" }
            require(notes.isNotEmpty()) { "Inventaire des échantillons : aucune note" }
            return SampleManifest(rate, notes)
        }
    }
}
