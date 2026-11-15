package com.sensitivemobility.neckatlas.domain.chord

import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.Pitch
import kotlin.math.abs

/** Un barré : un doigt posé à plat sur la case [fret], de la corde [fromString] à la corde [toString] incluses (0 = la plus grave). */
data class Barre(val fret: Int, val fromString: Int, val toString: Int)

/**
 * Une position d'accord du dictionnaire : pour chaque corde, de la plus grave à la plus aiguë, la case jouée (0 : corde à
 * vide), ou `null` si la corde est étouffée.
 *
 * @param roles pour chaque corde jouée, le rang de sa note dans l'accord (0 = fondamentale…) ; `null` si étouffée.
 * @param fingers pour chaque corde appuyée, le doigt (1 = index … 4 = auriculaire) ; `null` pour une corde à vide ou étouffée.
 * @param barre le barré, s'il en faut un.
 */
class ChordPosition(
    val frets: List<Int?>,
    val roles: List<Int?>,
    val fingers: List<Int?>,
    val barre: Barre?,
    val pitches: List<Pitch>,
) {
    /** Les cordes jouées (à vide ou appuyées), de la plus grave à la plus aiguë. */
    val playedStrings: List<Int> = frets.indices.filter { frets[it] != null }

    val lowestString: Int get() = playedStrings.first()

    private val pressed: List<Int> = frets.filterNotNull().filter { it > 0 }

    /** La case la plus basse sur laquelle un doigt appuie, ou 0 si aucune corde n'est appuyée. */
    val lowestFret: Int get() = pressed.minOrNull() ?: 0

    /** La case la plus haute sur laquelle un doigt appuie, ou 0 si aucune corde n'est appuyée. */
    val highestFret: Int get() = pressed.maxOrNull() ?: 0

    val mutedCount: Int get() = frets.count { it == null }

    val openCount: Int get() = frets.count { it == 0 }

    /** Vrai si au moins une corde sonne à vide : c'est un accord « de première position », près du sillet. */
    val usesOpenStrings: Boolean get() = openCount > 0

    /** Le nombre de doigts nécessaires (le barré compte pour un). */
    val fingerCount: Int get() = fingers.filterNotNull().toSet().size

    /** La position écrite comme sur une tablature : « x 3 2 0 1 0 » (x : corde étouffée). */
    val tab: String get() = frets.joinToString(" ") { it?.toString() ?: "x" }

    /**
     * Le coût de la position : plus il est bas, plus elle est facile. Peu de doigts, peu de cordes étouffées, pas de barré,
     * proche du sillet, une main resserrée, et des cordes à vide qui sonnent sans effort.
     */
    val effort: Double
        get() = fingerCount * 1.0 + mutedCount * 1.3 + (if (barre != null) 1.0 else 0.0) +
            lowestFret * 0.18 + (highestFret - lowestFret) * 0.4 - openCount * 0.3
}

/**
 * Le dictionnaire de positions d'accords. Pas de tableau écrit à la main : on cherche, pour l'instrument donné, les façons de
 * jouer l'accord qui sont jouables, puis on garde les plus faciles, une par région du manche. Ça marche donc pour n'importe
 * quel instrument et accordage (guitare 6 ou 7 cordes, basse), et chaque position est juste par construction.
 *
 * Une position est jouable si : la fondamentale est la note la plus grave ; toutes les notes de l'accord y sont (la quinte
 * d'un accord de septième majeure, mineure ou de dominante peut manquer, comme au naturel) ; les cordes jouées se suivent
 * (on n'étouffe que les cordes graves ou aiguës de bout) ; la main tient dans [MAX_SPAN] + 1 cases ; il faut au plus
 * [MAX_FINGERS] doigts, avec un barré quand il y a plus de notes que de doigts.
 */
object ChordPositions {
    /** Écart maximal entre la plus basse et la plus haute case appuyée : quatre cases de la main, de l'index à l'auriculaire. */
    const val MAX_SPAN = 3

    const val MAX_FINGERS = 4

    /** On cherche jusqu'à la onzième case : à partir de la douzième, ce sont les mêmes formes, un octave plus haut. */
    const val MAX_FIRST_FRET = 11

    /** Les cordes à vide ne servent que près du sillet : au-delà de cette case, la main ne les laisse plus sonner proprement. */
    const val OPEN_MAX_FRET = 4

    const val DEFAULT_LIMIT = 8

    /** Les rôles (0 = fondamentale…) dont une position peut se passer : la quinte des accords de septième courants. */
    private fun optionalRoles(quality: ChordQuality): Set<Int> = when (quality) {
        ChordQuality.DOMINANT_7, ChordQuality.MAJOR_7, ChordQuality.MINOR_7 -> setOf(2)
        else -> emptySet()
    }

    /**
     * Les meilleures positions de l'accord sur l'instrument, de la plus proche du sillet à la plus éloignée, au plus [limit].
     * Deux positions qui partent de la même corde et de la même région du manche sont des variantes l'une de l'autre : on ne
     * garde que la plus facile.
     */
    fun find(instrument: Instrument, chord: Chord, limit: Int = DEFAULT_LIMIT): List<ChordPosition> {
        val all = candidates(instrument, chord)
        // Une position dont une autre ne diffère que par des cordes à vide ou barrées en plus n'est qu'une version réduite.
        val full = all.filter { small -> all.none { big -> isFuller(big, small) } }
        val ranked = full.sortedWith(compareBy({ it.effort }, { it.lowestFret }, { it.lowestString }))
        val chosen = mutableListOf<ChordPosition>()
        for (candidate in ranked) {
            if (chosen.size == limit) break
            if (chosen.none { sameRegion(it, candidate) }) chosen += candidate
        }
        // Les accords de première position d'abord, puis les formes mobiles en remontant le manche.
        return chosen.sortedWith(compareBy({ if (it.usesOpenStrings) 0 else it.lowestFret }, { it.effort }))
    }

    /**
     * [big] joue les mêmes cases que [small] sur ses cordes, avec des cordes en plus qui ne demandent aucun doigt de plus et
     * sans barré que [small] n'aurait pas (sinon la version plus légère reste une variante plus facile, à garder).
     */
    private fun isFuller(big: ChordPosition, small: ChordPosition): Boolean =
        big.playedStrings.size > small.playedStrings.size &&
            big.fingerCount <= small.fingerCount &&
            (big.barre == null || small.barre != null) &&
            small.playedStrings.all { big.frets[it] == small.frets[it] }

    private fun sameRegion(a: ChordPosition, b: ChordPosition): Boolean =
        a.usesOpenStrings == b.usesOpenStrings && a.lowestString == b.lowestString && abs(a.lowestFret - b.lowestFret) <= 1

    /** Toutes les positions jouables de l'accord, sans choix ni ordre (dédoublonnées). */
    internal fun candidates(instrument: Instrument, chord: Chord): List<ChordPosition> {
        val stringCount = instrument.stringCount
        val openPitchClasses = instrument.openStrings.map { it.pitchClass }
        val roleOf = chord.pitchClasses.withIndex().associate { (role, pc) -> pc to role }
        val required = (0 until chord.quality.toneCount).toSet() - optionalRoles(chord.quality)
        val minPlayed = maxOf(required.size, if (stringCount >= 6) 4 else 3).coerceAtMost(stringCount)

        val seen = HashSet<List<Int?>>()
        val found = mutableListOf<ChordPosition>()

        // Une fenêtre de quatre cases, [first, first + MAX_SPAN] : toutes les cases appuyées y tiennent. Les cordes à vide sont
        // permises avec n'importe quelle fenêtre ; les positions identiques d'une fenêtre à l'autre ne sont gardées qu'une fois.
        for (first in 1..MAX_FIRST_FRET) {
            val chosen = arrayOfNulls<Int>(stringCount)

            // `phase` : 0 = on n'a pas encore joué de corde, 1 = on joue, 2 = on a étouffé après avoir joué (plus rien ne se joue).
            fun extend(string: Int, phase: Int) {
                if (string == stringCount) {
                    val frets = chosen.toList()
                    val played = frets.count { it != null }
                    if (played < minPlayed) return
                    val lowest = frets.indexOfFirst { it != null }
                    if ((openPitchClasses[lowest] + frets[lowest]!!) % 12 != chord.pitchClasses[0]) return
                    val roles = frets.mapIndexed { s, fret -> fret?.let { roleOf[(openPitchClasses[s] + it) % 12] } }
                    if (!roles.filterNotNull().toSet().containsAll(required)) return
                    if (!seen.add(frets)) return
                    val highest = frets.filterNotNull().max()
                    val lowestPressed = frets.filterNotNull().filter { it > 0 }.minOrNull() ?: 0
                    if (lowestPressed > MAX_FIRST_FRET) return
                    if (frets.any { it == 0 } && highest > OPEN_MAX_FRET) return
                    val fingering = fingering(frets) ?: return
                    val pitches = frets.mapIndexedNotNull { s, fret -> fret?.let { instrument.openStrings[s] + it } }
                    found += ChordPosition(frets, roles, fingering.first, fingering.second, pitches)
                    return
                }
                // Étouffée : permis tant qu'on n'a rien joué (cordes graves), ou pour finir (cordes aiguës).
                chosen[string] = null
                extend(string + 1, if (phase == 1) 2 else phase)
                if (phase == 2) return
                val open = openPitchClasses[string]
                if (open in roleOf.keys) {
                    chosen[string] = 0
                    extend(string + 1, 1)
                }
                for (fret in first..first + MAX_SPAN) {
                    if ((open + fret) % 12 in roleOf.keys) {
                        chosen[string] = fret
                        extend(string + 1, 1)
                    }
                }
                chosen[string] = null
            }
            extend(0, 0)
        }
        return found
    }

    /**
     * Les doigts d'une position, ou `null` si elle ne se joue pas à une main. Chaque corde appuyée prend un doigt (le plus
     * bas en case d'abord, à case égale la plus grave d'abord) ; s'il en faut plus de [MAX_FINGERS], un barré à plat sur la
     * case la plus basse en remplace plusieurs, à condition qu'aucune corde à vide ni étouffée ne se trouve sous lui.
     */
    internal fun fingering(frets: List<Int?>): Pair<List<Int?>, Barre?>? {
        val pressed = frets.mapIndexedNotNull { string, fret -> if (fret != null && fret > 0) string to fret else null }
        if (pressed.isEmpty()) return frets.map { null } to null

        fun byFretThenString(list: List<Pair<Int, Int>>) = list.sortedWith(compareBy({ it.second }, { it.first }))

        val lowest = pressed.minOf { it.second }
        val atLowest = pressed.filter { it.second == lowest }.map { it.first }

        if (pressed.size <= MAX_FINGERS) {
            val fingers = arrayOfNulls<Int>(frets.size)
            // Deux cordes voisines ou plus à la case la plus basse, avec d'autres cases à jouer au-dessus (le Fa « xx3211 ») :
            // l'index les tient à plat, un petit barré, et les autres doigts prennent la suite (3 2 1 1 plutôt que 4 3 1 2).
            val contiguous = atLowest.size >= 2 && atLowest.sorted().zipWithNext().all { (a, b) -> b == a + 1 }
            if (contiguous && pressed.any { it.second > lowest }) {
                for (string in atLowest) fingers[string] = 1
                byFretThenString(pressed.filter { it.second > lowest }).forEachIndexed { index, (string, _) -> fingers[string] = index + 2 }
                return fingers.toList() to Barre(lowest, atLowest.min(), atLowest.max())
            }
            byFretThenString(pressed).forEachIndexed { index, (string, _) -> fingers[string] = index + 1 }
            return fingers.toList() to null
        }

        if (atLowest.size < 2) return null
        val from = atLowest.min()
        val to = atLowest.max()
        // Le barré appuie toutes les cordes de `from` à `to` : aucune ne peut être à vide, étouffée, ni en deçà de sa case.
        for (string in from..to) {
            val fret = frets[string]
            if (fret == null || fret < lowest) return null
        }
        val others = pressed.filter { (string, fret) -> !(string in from..to && fret == lowest) }
        if (1 + others.size > MAX_FINGERS) return null
        val fingers = arrayOfNulls<Int>(frets.size)
        for (string in from..to) if (frets[string] == lowest) fingers[string] = 1
        byFretThenString(others).forEachIndexed { index, (string, _) -> fingers[string] = index + 2 }
        return fingers.toList() to Barre(lowest, from, to)
    }

    /**
     * La zone du manche qui montre une position. Près du sillet : du sillet (pour voir les cordes à vide et étouffées sur sa
     * rangée) à une case après la dernière. Plus haut : seulement les cases de la main, comme un diagramme d'accord qui
     * indique « 8 » à côté de sa première case.
     */
    fun zoneOf(position: ChordPosition): FretZone =
        if (position.usesOpenStrings || position.lowestFret <= NEAR_NUT_FRET) {
            FretZone(0, maxOf(MIN_ROWS, position.highestFret + 1).coerceAtMost(FretZone.MAX_FRET))
        } else {
            FretZone(position.lowestFret, maxOf(position.highestFret, position.lowestFret + MAX_SPAN).coerceAtMost(FretZone.MAX_FRET))
        }

    private const val MIN_ROWS = 5
    private const val NEAR_NUT_FRET = 2
}
