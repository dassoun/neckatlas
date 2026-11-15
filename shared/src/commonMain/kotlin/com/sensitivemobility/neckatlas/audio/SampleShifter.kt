package com.sensitivemobility.neckatlas.audio

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Accorde un échantillon sur une autre note : on le lit plus vite pour monter, plus lentement pour descendre, comme un
 * disque dont on change la vitesse. La note change aussi de durée (une note plus aiguë s'éteint plus vite), ce qui est
 * ce qu'on entend sur une vraie corde plus courte ; les écarts restent petits tant que l'échantillon est proche de la note.
 *
 * Les prises de son tiennent presque indéfiniment (guitare enregistrée en direct et saturée, basse aux doigts : à peine
 * 6 à 17 dB de moins après 2,5 s). Jouées telles quelles, elles sonnent comme des notes tenues. On leur ajoute donc une
 * décroissance progressive ([DECAY_DB]) qui les éteint sur la durée de la note, comme une corde que l'on étouffe.
 *
 * Entre deux échantillons voisins on interpole avec une courbe cubique (Catmull-Rom). Il n'y a pas de filtre avant de
 * lire plus vite : les échantillons sont des instruments sombres et enregistrés à 22 kHz, il n'y a presque rien au-dessus de
 * 8 kHz qui puisse se replier.
 */
internal object SampleShifter {
    /**
     * Baisse de niveau ajoutée entre le début et la fin de la note, en dB. Se règle à l'oreille : plus c'est grand, plus la
     * note s'éteint vite ; 0 laisse l'échantillon tel qu'il a été enregistré.
     */
    const val DECAY_DB = 30.0

    /**
     * @param data échantillon mono 16 bits enregistré à [sourceRate] Hz.
     * @param semitones écart voulu : positif pour monter, négatif pour descendre.
     * @param maxSeconds durée maximale du son rendu ; il peut être plus court si l'échantillon finit avant.
     * @param decayDb décroissance ajoutée, répartie sur la durée réelle du son rendu (voir [DECAY_DB]).
     * @return le son à [SAMPLE_RATE], entré en 1 ms et éteint en fondu sur 0,35 s (ou la moitié du son s'il est plus court).
     */
    fun shift(
        data: ShortArray,
        sourceRate: Int,
        semitones: Int,
        maxSeconds: Double = NOTE_SECONDS,
        decayDb: Double = DECAY_DB,
    ): ShortArray {
        // Pas de lecture : combien d'échantillons de l'enregistrement on avance à chaque échantillon rendu.
        val stride = 2.0.pow(semitones / 12.0) * sourceRate / SAMPLE_RATE
        // La courbe cubique lit un échantillon avant et deux après la position courante.
        val readable = ((data.size - 3) / stride).toInt()
        val count = minOf(readable, (maxSeconds * SAMPLE_RATE).toInt())
        require(count > 0) { "Échantillon trop court (${data.size} échantillons) pour être accordé" }

        val fadeIn = (SAMPLE_RATE * 0.001).toInt().coerceAtLeast(1)
        val fadeOut = minOf((SAMPLE_RATE * 0.35).toInt(), count / 2).coerceAtLeast(1)
        // Décroissance exponentielle (donc régulière en dB) qui atteint -decayDb au dernier échantillon.
        val decayPerSample = decayDb / 20.0 * ln(10.0) / count
        return ShortArray(count) { i ->
            val position = i * stride
            val k = position.toInt()
            val f = (position - k).toFloat()
            val y0 = data[if (k > 0) k - 1 else 0].toFloat()
            val y1 = data[k].toFloat()
            val y2 = data[k + 1].toFloat()
            val y3 = data[k + 2].toFloat()
            var v = y1 + 0.5f * f * (y2 - y0 + f * (2 * y0 - 5 * y1 + 4 * y2 - y3 + f * (3 * (y1 - y2) + y3 - y0)))
            v *= exp(-decayPerSample * i).toFloat()
            if (i < fadeIn) v *= i / fadeIn.toFloat()
            val untilEnd = count - 1 - i
            if (untilEnd < fadeOut) v *= untilEnd / fadeOut.toFloat()
            v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
