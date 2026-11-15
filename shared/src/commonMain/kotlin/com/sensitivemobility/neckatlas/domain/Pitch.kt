package com.sensitivemobility.neckatlas.domain

import kotlin.math.pow

/** Une hauteur de note, en numéro MIDI : 60 = Do central (Do4), 69 = La4 (440 Hz). */
data class Pitch(val midi: Int) : Comparable<Pitch> {
    init {
        require(midi in 0..127) { "Hauteur MIDI hors limites : $midi" }
    }

    /** Classe de hauteur, de 0 (Do) à 11 (Si), sans tenir compte de l'octave. */
    val pitchClass: Int get() = midi % 12

    /** Octave en notation scientifique : Do4 = 60. */
    val octave: Int get() = midi / 12 - 1

    val frequencyHz: Double get() = 440.0 * 2.0.pow((midi - 69) / 12.0)

    operator fun plus(semitones: Int): Pitch = Pitch(midi + semitones)

    override fun compareTo(other: Pitch): Int = midi.compareTo(other.midi)
}

/** Façon de nommer les notes : Do Ré Mi (solfège) ou C D E (lettres). */
enum class NoteNotation { SOLFEGE, LETTERS }

/** Écriture des altérations : dièses, bémols, ou les deux enharmoniques côte à côte (Do♯/Ré♭). */
enum class AccidentalStyle { BOTH, SHARPS, FLATS }

object NoteNames {
    private val solfege = listOf("Do", "Ré", "Mi", "Fa", "Sol", "La", "Si")
    private val letters = listOf("C", "D", "E", "F", "G", "A", "B")

    // Indexés par classe de hauteur (0 = Do … 11 = Si). Chaque valeur est l'indice de la note naturelle (Do = 0 … Si = 6),
    // ou -1 si la classe n'est pas concernée. Un dièse se lit sur la note en dessous, un bémol sur la note au-dessus.
    private val natural = intArrayOf(0, -1, 1, -1, 2, 3, -1, 4, -1, 5, -1, 6)
    private val sharpOf = intArrayOf(-1, 0, -1, 1, -1, -1, 3, -1, 4, -1, 5, -1)
    private val flatOf = intArrayOf(-1, 1, -1, 2, -1, -1, 4, -1, 5, -1, 6, -1)

    private fun base(index: Int, notation: NoteNotation): String =
        if (notation == NoteNotation.SOLFEGE) solfege[index] else letters[index]

    /** Les écritures possibles d'une classe de hauteur : une pour une note naturelle, deux (dièse puis bémol) sinon. */
    fun spellings(pitchClass: Int, notation: NoteNotation): List<String> {
        require(pitchClass in 0..11) { "Classe de hauteur hors limites : $pitchClass" }
        val n = natural[pitchClass]
        if (n >= 0) return listOf(base(n, notation))
        return listOf(base(sharpOf[pitchClass], notation) + "♯", base(flatOf[pitchClass], notation) + "♭")
    }

    /** Nom d'une note sans octave, selon la notation et le style d'altérations choisis : « La », « Do♯/Ré♭ »… */
    fun label(pitchClass: Int, notation: NoteNotation, style: AccidentalStyle): String =
        pick(spellings(pitchClass, notation), style) { it }

    /** Nom d'une note avec son octave : « La2 », « Do♯3/Ré♭3 »… */
    fun label(pitch: Pitch, notation: NoteNotation, style: AccidentalStyle): String =
        pick(spellings(pitch.pitchClass, notation), style) { it + pitch.octave }

    private fun pick(spellings: List<String>, style: AccidentalStyle, decorate: (String) -> String): String {
        if (spellings.size == 1) return decorate(spellings[0])
        return when (style) {
            AccidentalStyle.SHARPS -> decorate(spellings[0])
            AccidentalStyle.FLATS -> decorate(spellings[1])
            AccidentalStyle.BOTH -> spellings.joinToString("/", transform = decorate)
        }
    }
}
