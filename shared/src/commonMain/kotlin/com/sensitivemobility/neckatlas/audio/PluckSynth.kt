package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Timbre
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

/** Fréquence d'échantillonnage de tous les sons de l'appli : mono, 16 bits. */
const val SAMPLE_RATE = 44_100

/** Durée d'une note jouée par l'appli, en secondes. Une note plus longue sonne comme une note tenue, pas comme une corde. */
const val NOTE_SECONDS = 1.5

/**
 * Synthèse d'une corde pincée par l'algorithme de Karplus-Strong : une rafale de bruit tourne dans une ligne à retard dont
 * la longueur donne la hauteur ; à chaque tour elle passe dans un filtre qui l'amortit et adoucit les aigus, comme une
 * vraie corde. Tout est calculé ici, sans fichier son : rien à embarquer, et la note est exacte pour n'importe quelle case.
 *
 * L'appli joue désormais de vraies prises de son ([SampleBank]) ; ce synthétiseur reste le repli quand elles sont
 * introuvables ou illisibles, et sert de référence de justesse dans les tests.
 *
 * La ligne à retard est un nombre entier d'échantillons : pour accorder juste on complète avec un filtre passe-tout qui
 * ajoute la fraction manquante (sans lui, une note aiguë serait fausse de plusieurs dizaines de centièmes de ton).
 */
object PluckSynth {
    private class Voicing(
        /** 0 à 1 : plus c'est grand, plus la rafale de départ est brillante. */
        val brightness: Float,
        /** Position du médiator le long de la corde (0 à 1) : change la couleur du son. */
        val pickPosition: Float,
        /** Temps, en secondes, que mettrait la note pour perdre 60 dB si on la laissait sonner. */
        val sustainSeconds: Double,
    )

    // La basse est plus ronde au départ et dure plus longtemps ; les haut-parleurs de téléphone ne rendent pas les notes les
    // plus graves, mais leurs harmoniques suffisent à faire entendre la hauteur.
    private fun voicing(timbre: Timbre) = when (timbre) {
        Timbre.GUITAR -> Voicing(brightness = 0.55f, pickPosition = 0.14f, sustainSeconds = 4.0)
        Timbre.BASS -> Voicing(brightness = 0.40f, pickPosition = 0.22f, sustainSeconds = 5.0)
    }

    /**
     * Calcule une note.
     *
     * @param frequencyHz hauteur de la note, entre 20 et 2 000 Hz.
     * @param durationSeconds durée du son ; la fin s'éteint en fondu.
     * @param seed graine du bruit de départ : la même graine donne toujours le même son.
     * @return des échantillons 16 bits mono à [SAMPLE_RATE].
     */
    fun render(frequencyHz: Double, timbre: Timbre, durationSeconds: Double = NOTE_SECONDS, seed: Int = 7): ShortArray {
        require(frequencyHz in 20.0..2000.0) { "Fréquence hors limites : $frequencyHz" }
        val voicing = voicing(timbre)
        val total = (durationSeconds * SAMPLE_RATE).toInt()

        // Retard total de la boucle = période. Le filtre moyenneur en prend une demi-échantillon, le reste se partage entre
        // la ligne (entier) et le passe-tout (fraction entre 0,5 et 1,5, plage où il est le plus précis).
        val period = SAMPLE_RATE / frequencyHz
        val remaining = period - 0.5
        val length = floor(remaining - 0.5).toInt()
        val fraction = remaining - length
        val allpass = ((1.0 - fraction) / (1.0 + fraction)).toFloat()

        val line = excitation(length, voicing, Random(seed))

        // Gain de boucle : après `sustainSeconds` la note a perdu 60 dB.
        val loopGain = 10.0.pow(-3.0 / (frequencyHz * voicing.sustainSeconds)).toFloat()

        val out = FloatArray(total)
        var position = 0
        var previous = 0f
        var allpassIn = 0f
        var allpassOut = 0f
        for (k in 0 until total) {
            val sample = line[position]
            out[k] = sample
            val smoothed = loopGain * 0.5f * (sample + previous)
            previous = sample
            val tuned = allpass * smoothed + allpassIn - allpass * allpassOut
            allpassIn = smoothed
            allpassOut = tuned
            line[position] = tuned
            position = if (position + 1 == length) 0 else position + 1
        }
        return toPcm(out)
    }

    /** La rafale de départ : du bruit adouci, avec l'effet de peigne du point de pincement, sans composante continue. */
    private fun excitation(length: Int, voicing: Voicing, random: Random): FloatArray {
        val line = FloatArray(length)
        var smooth = 0f
        for (i in 0 until length) {
            smooth += voicing.brightness * ((random.nextFloat() * 2f - 1f) - smooth)
            line[i] = smooth
        }
        val pick = (length * voicing.pickPosition).toInt().coerceAtLeast(1)
        for (i in length - 1 downTo pick) line[i] -= line[i - pick]
        val mean = line.sum() / length
        for (i in 0 until length) line[i] -= mean
        return line
    }

    /** Met le son à un niveau constant, fait entrer et sortir la note en douceur, et la convertit en 16 bits. */
    private fun toPcm(signal: FloatArray): ShortArray {
        var peak = 0f
        for (v in signal) peak = maxOf(peak, abs(v))
        val scale = if (peak > 0f) TARGET_PEAK / peak else 0f
        val fadeIn = (SAMPLE_RATE * 0.001).toInt().coerceAtLeast(1)
        val fadeOut = minOf((SAMPLE_RATE * 0.35).toInt(), signal.size / 2).coerceAtLeast(1)
        return ShortArray(signal.size) { k ->
            var gain = scale
            if (k < fadeIn) gain *= k / fadeIn.toFloat()
            val untilEnd = signal.size - 1 - k
            if (untilEnd < fadeOut) gain *= untilEnd / fadeOut.toFloat()
            (signal[k] * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private const val TARGET_PEAK = 0.9f
}
