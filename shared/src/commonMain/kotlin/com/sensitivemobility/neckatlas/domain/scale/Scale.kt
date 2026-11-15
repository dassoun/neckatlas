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
enum class ScaleType(val intervals: List<Int>, val degrees: List<String>, val passing: Set<Int> = emptySet()) {
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
    BLUES(listOf(0, 3, 5, 6, 7, 10), listOf("1", "♭3", "4", "♭5", "5", "♭7"), passing = setOf(3));

    init {
        require(intervals.size == degrees.size) { "$name : ${intervals.size} intervalles pour ${degrees.size} degrés" }
        require(intervals.first() == 0 && intervals == intervals.sorted() && intervals.last() < 12) { "$name : intervalles invalides" }
    }

    val noteCount: Int get() = intervals.size

    /**
     * Les degrés (indices) qui structurent les positions : tous, sauf les notes de passage. Le blues est la pentatonique mineure
     * plus une note bleue (♭5) : ses positions sont celles de la pentatonique, la note bleue s'y glisse entre deux notes voisines.
     */
    val coreDegrees: List<Int> get() = intervals.indices.filter { it !in passing }
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
 * Une position de la gamme : un dessin de notes sur le manche, de [firstFret] à [lastFret], avec les mêmes notes sur chaque corde
 * (trois pour une gamme de sept notes, deux pour une pentatonique) : la façon classique d'apprendre une gamme sur le manche.
 */
class ScalePosition(val notes: List<ScaleNote>) {
    val firstFret: Int = notes.minOf { it.position.fret }
    val lastFret: Int = notes.maxOf { it.position.fret }
}

object ScalePositions {
    /** Notes par corde : trois pour une gamme de sept notes, deux pour une pentatonique (ou un blues, qui en est une). */
    private fun notesPerString(type: ScaleType): Int = if (type.coreDegrees.size >= 7) 3 else 2

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
     * Les positions de la gamme, une par degré : la position n commence sur le n-ième degré, joué sur la corde la plus grave (la
     * 1re sur la fondamentale, la 2e sur le degré suivant…). Sur chaque corde on joue les [notesPerString] notes de la gamme qui se
     * suivent, et la corde d'après reprend à la note suivante : on monte ainsi le manche en diagonale, sans jamais sauter une note
     * ni en répéter une. Les cases voisines d'une position sont donc celles de la suivante, décalée d'un degré.
     * Elles ne dépendent pas de la zone de travail : c'est un dictionnaire, pas un exercice.
     */
    fun find(board: Fretboard, scale: Scale): List<ScalePosition> {
        val instrument = board.instrument
        val core = scale.type.coreDegrees
        val corePitchClasses = core.map { scale.pitchClasses[it] }.toSet()
        val perString = notesPerString(scale.type)

        // Le dessin qui part de la case [startFret] de la corde grave. En mode strict, il est refusé si une corde devrait commencer
        // sous sa corde à vide (case négative : le sillet bloque la diagonale) ou si une note sort du manche ; sinon on saute ces notes.
        fun build(startFret: Int, strict: Boolean): List<ScaleNote>? {
            var previous = board.pitchAt(FretPosition(0, startFret)).midi - 1
            val picked = mutableListOf<ScaleNote>()
            for (string in 0 until instrument.stringCount) {
                val open = instrument.openStrings[string].midi
                var onString = 0
                var midi = previous
                while (onString < perString) {
                    var candidate = midi + 1
                    while (candidate % 12 !in corePitchClasses) candidate++
                    if (strict && (candidate < open || candidate > open + FretZone.MAX_FRET)) return null
                    if (candidate < open) {
                        candidate += 12 * ((open - candidate + 11) / 12)
                        while (candidate % 12 !in corePitchClasses) candidate++
                    }
                    if (candidate > open + FretZone.MAX_FRET) break
                    midi = candidate
                    picked += ScaleNote(FretPosition(string, midi - open), scale.degreeOf(midi % 12)!!)
                    onString++
                }
                previous = midi
            }
            return picked.takeIf { it.isNotEmpty() }
        }

        return core.mapNotNull { degree ->
            val startFret = (0..11).first { board.pitchAt(FretPosition(0, it)).pitchClass == scale.pitchClasses[degree] }
            // Au sillet, le dessin peut buter sur une corde à vide : on le joue alors une octave plus haut (mêmes notes).
            val picked = build(startFret, strict = true) ?: build(startFret + 12, strict = true) ?: build(startFret, strict = false) ?: return@mapNotNull null
            // Les notes de passage (la note bleue) : celles qui tombent entre deux notes de la position sur une même corde.
            val passingNotes = buildList {
                for (string in 0 until instrument.stringCount) {
                    val frets = picked.filter { it.position.string == string }.map { it.position.fret }
                    if (frets.isEmpty()) continue
                    for (fret in frets.min()..frets.max()) {
                        val position = FretPosition(string, fret)
                        val d = scale.degreeOf(board.pitchAt(position).pitchClass)
                        if (d != null && d in scale.type.passing) add(ScaleNote(position, d))
                    }
                }
            }
            ScalePosition((picked + passingNotes).sortedWith(compareBy({ it.position.string }, { it.position.fret })))
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
