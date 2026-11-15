package com.sensitivemobility.neckatlas.domain.scale

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Pitch

/**
 * Les gammes du dictionnaire. [intervals] : l'écart en demi-tons de chaque note à la fondamentale, en montant ; [degrees] :
 * le nom de chaque degré (1, ♭3, ♯4…), dans le même ordre. Les modes (dorien…) sont des gammes comme les autres : ce sont les
 * mêmes notes que la majeure, jouées à partir d'une autre.
 */
enum class ScaleType(val intervals: List<Int>, val degrees: List<String>) {
    MAJOR(listOf(0, 2, 4, 5, 7, 9, 11), listOf("1", "2", "3", "4", "5", "6", "7")),
    DORIAN(listOf(0, 2, 3, 5, 7, 9, 10), listOf("1", "2", "♭3", "4", "5", "6", "♭7")),
    PHRYGIAN(listOf(0, 1, 3, 5, 7, 8, 10), listOf("1", "♭2", "♭3", "4", "5", "♭6", "♭7")),
    LYDIAN(listOf(0, 2, 4, 6, 7, 9, 11), listOf("1", "2", "3", "♯4", "5", "6", "7")),
    MIXOLYDIAN(listOf(0, 2, 4, 5, 7, 9, 10), listOf("1", "2", "3", "4", "5", "6", "♭7")),
    NATURAL_MINOR(listOf(0, 2, 3, 5, 7, 8, 10), listOf("1", "2", "♭3", "4", "5", "♭6", "♭7")),
    LOCRIAN(listOf(0, 1, 3, 5, 6, 8, 10), listOf("1", "♭2", "♭3", "4", "♭5", "♭6", "♭7")),
    HARMONIC_MINOR(listOf(0, 2, 3, 5, 7, 8, 11), listOf("1", "2", "♭3", "4", "5", "♭6", "7")),
    MELODIC_MINOR(listOf(0, 2, 3, 5, 7, 9, 11), listOf("1", "2", "♭3", "4", "5", "6", "7")),
    MAJOR_PENTATONIC(listOf(0, 2, 4, 7, 9), listOf("1", "2", "3", "5", "6")),
    MINOR_PENTATONIC(listOf(0, 3, 5, 7, 10), listOf("1", "♭3", "4", "5", "♭7")),
    BLUES(listOf(0, 3, 5, 6, 7, 10), listOf("1", "♭3", "4", "♭5", "5", "♭7"));

    init {
        require(intervals.size == degrees.size) { "$name : ${intervals.size} intervalles pour ${degrees.size} degrés" }
        require(intervals.first() == 0 && intervals == intervals.sorted() && intervals.last() < 12) { "$name : intervalles invalides" }
    }

    val noteCount: Int get() = intervals.size
}

/** Une gamme : sa fondamentale (classe de hauteur, 0 = Do) et son type. */
data class Scale(val root: Int, val type: ScaleType) {
    init {
        require(root in 0..11) { "Fondamentale hors limites : $root" }
    }

    /** Les notes de la gamme (classes de hauteur), en montant à partir de la fondamentale. */
    val pitchClasses: List<Int> = type.intervals.map { (root + it) % 12 }

    /** Le degré (0 = fondamentale, 1 = seconde…) de cette note dans la gamme, ou null si elle n'en fait pas partie. */
    fun degreeOf(pitchClass: Int): Int? = pitchClasses.indexOf(pitchClass).takeIf { it >= 0 }
}

/** Une note de la gamme quelque part sur le manche, avec son degré (0 = la fondamentale). */
class ScaleNote(val position: FretPosition, val degree: Int)

/**
 * Une position de la gamme : toutes ses notes dans une fenêtre de [ScalePositions.WINDOW_FRETS] cases, de [firstFret] à
 * [lastFret] incluses. Un doigt par case, sans bouger la main : la façon classique d'apprendre une gamme sur le manche.
 */
class ScalePosition(val firstFret: Int, val lastFret: Int, val notes: List<ScaleNote>)

object ScalePositions {
    /** La fenêtre d'une position : quatre cases, de l'index à l'auriculaire. */
    const val WINDOW_FRETS = 4

    /** Toutes les notes de la gamme dans la zone, sur toutes les cordes, les cordes graves d'abord. */
    fun allNotes(board: Fretboard, zone: FretZone, scale: Scale): List<ScaleNote> = buildList {
        for (string in 0 until board.instrument.stringCount) {
            for (fret in zone.frets) {
                val position = FretPosition(string, fret)
                scale.degreeOf(board.pitchAt(position).pitchClass)?.let { add(ScaleNote(position, it)) }
            }
        }
    }

    /**
     * Les positions de la gamme sur un octave du manche (le dessin se répète à la douzième case). La position n commence sur
     * le n-ième degré de la gamme, joué sur la corde la plus grave (la 1re sur la fondamentale, la 2e sur le degré suivant…) :
     * une gamme de sept notes a sept positions, une pentatonique cinq. Elles ne dépendent pas de la zone de travail : c'est un dictionnaire, pas un exercice.
     */
    fun find(board: Fretboard, scale: Scale): List<ScalePosition> {
        val lowString = 0
        // Position 1 : la fondamentale sur la corde grave ; position 2 : le degré suivant ; etc. (pas l'ordre des cases).
        val starts = scale.pitchClasses.map { pc -> (0..11).first { fret -> board.pitchAt(FretPosition(lowString, fret)).pitchClass == pc } }
        return starts.map { first ->
            val last = first + WINDOW_FRETS - 1
            val notes = buildList {
                for (string in 0 until board.instrument.stringCount) {
                    for (fret in first..last) {
                        val position = FretPosition(string, fret)
                        scale.degreeOf(board.pitchAt(position).pitchClass)?.let { add(ScaleNote(position, it)) }
                    }
                }
            }
            ScalePosition(first, last, notes)
        }
    }

    /**
     * Les hauteurs à faire entendre pour jouer la gamme en montant : à partir de la plus grave fondamentale parmi [notes], jusqu'à
     * l'octave au-dessus, sans doublon. Sans fondamentale dans [notes], toutes les notes en montant.
     */
    fun ascending(board: Fretboard, notes: List<ScaleNote>): List<Pitch> {
        val pitches = notes.map { board.pitchAt(it.position) to it.degree }
            .distinctBy { it.first.midi }
            .sortedBy { it.first.midi }
        val rootMidi = pitches.firstOrNull { it.second == 0 }?.first?.midi ?: return pitches.map { it.first }
        return pitches.filter { it.first.midi in rootMidi..rootMidi + 12 }.map { it.first }
    }
}
