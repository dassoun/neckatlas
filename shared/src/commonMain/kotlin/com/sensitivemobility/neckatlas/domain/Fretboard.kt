package com.sensitivemobility.neckatlas.domain

/** Un emplacement sur le manche : [string] est l'indice de la corde (0 = la plus grave), [fret] la case (0 = corde à vide). */
data class FretPosition(val string: Int, val fret: Int)

/**
 * La zone du manche sur laquelle l'utilisateur travaille, de la case [first] à la case [last] incluses. La case 0 est la
 * corde à vide : elle n'est dans la zone que si celle-ci commence à 0.
 */
data class FretZone(val first: Int, val last: Int) {
    init {
        require(first in 0..MAX_FRET && last in 1..MAX_FRET && first <= last) { "Zone invalide : $first à $last" }
    }

    val frets: IntRange get() = first..last

    val includesOpenStrings: Boolean get() = first == 0

    operator fun contains(fret: Int): Boolean = fret in first..last

    companion object {
        const val MAX_FRET = 24

        /** Les [count] premières cases, avec les cordes à vide. */
        fun firstFrets(count: Int): FretZone = FretZone(0, count)

        val DEFAULT = firstFrets(12)
    }
}

class Fretboard(val instrument: Instrument) {
    fun pitchAt(position: FretPosition): Pitch = instrument.openStrings[position.string] + position.fret

    /** Tous les endroits de la zone où l'on joue cette note (dans n'importe quelle octave), cordes graves d'abord. */
    fun positionsOf(pitchClass: Int, zone: FretZone): List<FretPosition> = buildList {
        for (string in 0 until instrument.stringCount) {
            for (fret in zone.frets) {
                val position = FretPosition(string, fret)
                if (pitchAt(position).pitchClass == pitchClass) add(position)
            }
        }
    }
}
