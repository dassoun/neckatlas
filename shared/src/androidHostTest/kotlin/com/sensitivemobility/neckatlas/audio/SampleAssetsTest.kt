package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.Pitch
import com.sensitivemobility.neckatlas.domain.Timbre
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Teste les vrais fichiers embarqués (`composeResources/files/samples`, produits par `tools/make_samples.py`), lus comme
 * l'appli les lit, avec la vraie [SampleBank]. S'ils sont absents, abîmés ou mal accordés, c'est ici qu'on le voit.
 */
class SampleAssetsTest {
    // Gradle lance les tests depuis le dossier du module (`shared/`).
    private val resources = File("src/commonMain/composeResources")

    private fun bank(): SampleBank {
        check(File(resources, "files/samples/manifest.txt").isFile) { "Échantillons introuvables dans ${resources.absolutePath}" }
        return SampleBank(SampleLoader { path -> File(resources, path).readBytes() })
    }

    @Test
    fun everyNoteOfEveryInstrumentIsAudibleAndInTune() {
        val problems = ArrayList<String>()
        val deviations = ArrayList<Pair<String, Double>>()
        var worst = 0.0
        for (timbre in Timbre.entries) {
            val bank = bank()
            val strings = Instrument.entries.filter { it.timbre == timbre }.flatMap { it.openStrings }
            // De la corde la plus grave à vide jusqu'à la case 24 de la corde la plus aiguë.
            for (midi in strings.minOf { it.midi }..strings.maxOf { it.midi } + 24) {
                val pitch = Pitch(midi)
                // Trois passages par note : toutes les variantes de la guitare, qui en a trois.
                repeat(3) {
                    val sound = runNow { bank.render(pitch, timbre) }
                    val peak = sound.maxOf { abs(it.toInt()) }
                    // Fenêtre large : sur une basse grave, la fenêtre par défaut ne contient que quatre périodes et fausse la mesure.
                    val deviation = cents(estimateFrequency(sound, pitch.frequencyHz, minWindow = 16_384), pitch.frequencyHz)
                    worst = max(worst, abs(deviation))
                    val where = "$timbre midi $midi"
                    deviations += where to deviation
                    if (peak < 16_000 || peak >= Short.MAX_VALUE) problems += "$where : crête $peak (trop faible ou saturée)"
                    if (sound.size < 0.9 * SAMPLE_RATE) problems += "$where : son trop court (${sound.size} échantillons)"
                    if (abs(deviation) > TOLERANCE_CENTS) problems += "$where : %.1f cents d'écart".format(deviation)
                    if (sound.first().toInt() != 0 || sound.last().toInt() != 0) problems += "$where : ne commence ou ne finit pas à zéro"
                }
            }
        }
        println("Écart de justesse le plus grand sur tous les échantillons : %.1f cents".format(worst))
        println("Les dix pires : " + deviations.sortedByDescending { abs(it.second) }.take(10).joinToString { "%s %+.1f".format(it.first, it.second) })
        println("Écart typique (médiane) : %.1f cents".format(deviations.map { abs(it.second) }.sorted()[deviations.size / 2]))
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    private companion object {
        /**
         * Mesuré : 6,0 cents au pire, 1,5 en médiane (4,3 et 1,3 avant que la décroissance ajoutée ne fausse un peu la mesure).
         * Une note fausse (mauvais échantillon, mauvais décalage) est à 100 cents ou plus.
         */
        const val TOLERANCE_CENTS = 8.0
    }
}
