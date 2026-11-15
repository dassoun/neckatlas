package com.sensitivemobility.neckatlas.domain.chord

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.Pitch

/**
 * Les familles d'accords : quatre triades (trois notes) et cinq accords de septième (quatre notes). [intervals] : l'écart,
 * en demi-tons, de chaque note à la fondamentale, dans l'ordre fondamentale, tierce, quinte, septième.
 */
enum class ChordQuality(val symbol: String, val intervals: List<Int>) {
    MAJOR("Maj", listOf(0, 4, 7)),
    MINOR("min", listOf(0, 3, 7)),
    DIMINISHED("dim", listOf(0, 3, 6)),
    AUGMENTED("aug", listOf(0, 4, 8)),
    DOMINANT_7("7", listOf(0, 4, 7, 10)),
    MAJOR_7("Maj7", listOf(0, 4, 7, 11)),
    MINOR_7("m7", listOf(0, 3, 7, 10)),
    HALF_DIMINISHED("m7♭5", listOf(0, 3, 6, 10)),
    DIMINISHED_7("dim7", listOf(0, 3, 6, 9));

    val toneCount: Int get() = intervals.size

    val isSeventh: Boolean get() = toneCount == 4

    /** Le rôle de chaque note dans l'accord : R, ♭3, 3, ♭5, 5, ♯5, ♭♭7, ♭7, 7. */
    val roleLabels: List<String> = intervals.mapIndexed { degree, semitones ->
        when (degree) {
            0 -> "R"
            1 -> if (semitones == 3) "♭3" else "3"
            2 -> when (semitones) {
                6 -> "♭5"
                8 -> "♯5"
                else -> "5"
            }
            else -> when (semitones) {
                9 -> "♭♭7"
                10 -> "♭7"
                else -> "7"
            }
        }
    }
}

/** Un accord : sa fondamentale (classe de hauteur, 0 = Do) et sa famille. */
data class Chord(val root: Int, val quality: ChordQuality) {
    init {
        require(root in 0..11) { "Fondamentale hors limites : $root" }
    }

    /** Les notes de l'accord (classes de hauteur), dans l'ordre fondamentale, tierce, quinte, septième. */
    val pitchClasses: List<Int> = quality.intervals.map { (root + it) % 12 }

    /** Le rang (0 = fondamentale, 1 = tierce…) de cette note dans l'accord, ou null si elle n'en fait pas partie. */
    fun roleOf(pitchClass: Int): Int? = pitchClasses.indexOf(pitchClass).takeIf { it >= 0 }
}

/**
 * Une façon de jouer un accord : une note par corde sur des cordes voisines, la plus grave en premier. [roles] donne, pour
 * chaque corde, le rang de sa note dans l'accord (0 = fondamentale…).
 */
class Voicing(val positions: List<FretPosition>, val roles: List<Int>, val pitches: List<Pitch>) {
    val minFret: Int = positions.minOf { it.fret }
    val maxFret: Int = positions.maxOf { it.fret }
}

/** Une note d'accord quelque part sur le manche, avec son rôle. */
class ChordNote(val position: FretPosition, val role: Int)

object Voicings {
    /** Écart maximal entre la case la plus basse et la plus haute d'une position (la corde à vide compte comme case 0) : 5 cases de la main. */
    const val MAX_FRET_SPAN = 4

    /** Les cordes de départ possibles pour un accord de [toneCount] notes sur cordes voisines (0 = la plus grave). */
    fun stringSets(instrument: Instrument, toneCount: Int): List<Int> = (0..instrument.stringCount - toneCount).toList()

    /**
     * Toutes les positions de l'accord dans la zone, avec le renversement [inversion] (0 = la fondamentale à la basse, 1 =
     * la tierce, 2 = la quinte, 3 = la septième), sur les cordes voisines à partir de [startString]. Une position est
     * « serrée » : les notes montent de corde en corde et tiennent dans moins d'une octave, comme dans un voicing de
     * triade classique ; et elle est jouable : au plus 5 cases de la main. Triées de la plus basse à la plus haute.
     */
    fun find(board: Fretboard, zone: FretZone, chord: Chord, inversion: Int, startString: Int): List<Voicing> {
        val count = chord.quality.toneCount
        require(inversion in 0 until count) { "Renversement hors limites : $inversion" }
        require(startString in 0..board.instrument.stringCount - count) { "Corde de départ hors limites : $startString" }

        // Du grave à l'aigu, les rôles se suivent en tournant : au 1er renversement d'une triade, tierce, quinte, fondamentale.
        val roles = List(count) { (inversion + it) % count }
        val candidates = roles.mapIndexed { index, role ->
            zone.frets.filter { fret -> board.pitchAt(FretPosition(startString + index, fret)).pitchClass == chord.pitchClasses[role] }
        }

        val found = mutableListOf<Voicing>()
        fun extend(chosen: List<Int>) {
            if (chosen.size == count) {
                val positions = chosen.mapIndexed { index, fret -> FretPosition(startString + index, fret) }
                val pitches = positions.map(board::pitchAt)
                val ascending = pitches.zipWithNext().all { (low, high) -> low.midi < high.midi }
                val closed = pitches.last().midi - pitches.first().midi < 12
                val playable = chosen.max() - chosen.min() <= MAX_FRET_SPAN
                if (ascending && closed && playable) found += Voicing(positions, roles, pitches)
                return
            }
            for (fret in candidates[chosen.size]) extend(chosen + fret)
        }
        extend(emptyList())
        return found.sortedWith(compareBy({ it.minFret }, { it.maxFret }))
    }

    /** Toutes les notes de l'accord dans la zone, sur toutes les cordes : la carte d'un arpège. */
    fun arpeggio(board: Fretboard, zone: FretZone, chord: Chord): List<ChordNote> = buildList {
        for (string in 0 until board.instrument.stringCount) {
            for (fret in zone.frets) {
                val position = FretPosition(string, fret)
                chord.roleOf(board.pitchAt(position).pitchClass)?.let { add(ChordNote(position, it)) }
            }
        }
    }

    /** Les hauteurs d'un arpège, de la plus grave à la plus aiguë, chaque hauteur une seule fois. */
    fun ascendingPitches(board: Fretboard, notes: List<ChordNote>): List<Pitch> =
        notes.map { board.pitchAt(it.position) }.distinct().sortedBy { it.midi }
}
