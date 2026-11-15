package com.sensitivemobility.neckatlas.domain.quiz

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.chord.Chord
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.chord.Voicing
import com.sensitivemobility.neckatlas.domain.chord.Voicings
import kotlin.random.Random

/** Les exercices : trois sur les notes, trois sur les accords. */
enum class QuizMode {
    /** « Où est le La sur la corde de Ré ? » : on touche la case sur la corde nommée. */
    FIND_ON_STRING,

    /** « Trouve un La » : n'importe quelle case de la zone qui donne cette note convient. */
    FIND_ANYWHERE,

    /** Une case est montrée : on choisit le nom de sa note. */
    NAME_NOTE,

    /** Une forme d'accord est montrée : on choisit sa fondamentale et son type. */
    NAME_CHORD,

    /** « Place Do majeur, 1er renversement, sur les cordes 3·2·1 » : on touche une note par corde. */
    PLACE_CHORD,

    /** On entend un accord sur une fondamentale donnée : on choisit son type. */
    EAR_CHORD,
    ;

    val isChord: Boolean get() = this == NAME_CHORD || this == PLACE_CHORD || this == EAR_CHORD
}

/** Durée d'une série : un nombre de questions, ou une minute pour en faire le plus possible. */
enum class QuizLength(val questions: Int?, val seconds: Int?) {
    QUESTIONS_10(10, null),
    QUESTIONS_20(20, null),
    SECONDS_60(null, 60),
}

/** Les accords interrogés : les quatre triades seulement, ou aussi les cinq septièmes. */
enum class ChordPool { TRIADS, ALL }

/**
 * Le réglage d'une série.
 *
 * @param strings indices des cordes interrogées (0 = la plus grave) ; vide = toutes. Sans effet pour
 * [QuizMode.FIND_ANYWHERE] ni pour les exercices d'accords.
 * @param naturalsOnly seulement Do Ré Mi Fa Sol La Si (pour les accords : seulement ces fondamentales).
 * @param chordPool, chordInversions ce que les exercices d'accords peuvent demander : triades seules ou aussi septièmes, position
 * fondamentale seule ou aussi renversements.
 */
data class QuizConfig(
    val mode: QuizMode = QuizMode.FIND_ON_STRING,
    val strings: Set<Int> = emptySet(),
    val naturalsOnly: Boolean = true,
    val length: QuizLength = QuizLength.QUESTIONS_10,
    val chordPool: ChordPool = ChordPool.TRIADS,
    val chordInversions: Boolean = false,
)

/** Une forme d'accord demandée : l'accord, son renversement et la corde la plus grave du groupe (0 = la plus grave). */
data class ChordSpec(val chord: Chord, val inversion: Int, val start: Int)

sealed interface Question {
    /** La note concernée (0 = Do … 11 = Si) ; pour un accord, sa fondamentale. */
    val pitchClass: Int

    /** La famille de l'accord interrogé, ou null pour les questions sur les notes. */
    val chordQuality: ChordQuality? get() = null

    /**
     * Trouver la note sur le manche : sur la corde [string] si elle est donnée, sinon n'importe où dans la zone, ou dans
     * [area] quand une fenêtre plus petite que la zone est donnée (elle contient toujours au moins une case juste).
     */
    data class Find(override val pitchClass: Int, val string: Int?, val area: FretArea? = null) : Question

    /** Nommer la note de la case [position]. */
    data class Name(val position: FretPosition, override val pitchClass: Int) : Question

    /** Nommer l'accord que forment les cases [positions] (une par corde, de la plus grave à la plus aiguë). */
    data class NameChord(val spec: ChordSpec, val positions: List<FretPosition>) : Question {
        override val pitchClass: Int get() = spec.chord.root
        override val chordQuality: ChordQuality get() = spec.chord.quality
    }

    /** Placer l'accord [spec] : une note par corde du groupe. */
    data class PlaceChord(val spec: ChordSpec) : Question {
        override val pitchClass: Int get() = spec.chord.root
        override val chordQuality: ChordQuality get() = spec.chord.quality
    }

    /** Reconnaître à l'oreille le type de l'accord joué par les cases [positions] ; sa fondamentale est donnée. */
    data class EarChord(val spec: ChordSpec, val positions: List<FretPosition>) : Question {
        override val pitchClass: Int get() = spec.chord.root
        override val chordQuality: ChordQuality get() = spec.chord.quality
    }
}

/** Une fenêtre du manche : des cases [zone] et des cordes voisines, de [firstString] à [lastString] (0 = la plus grave). */
data class FretArea(val zone: FretZone, val firstString: Int, val lastString: Int) {
    operator fun contains(position: FretPosition): Boolean =
        position.fret in zone && position.string in firstString..lastString
}

/** Les notes sans altération, en classes de hauteur. */
val NaturalPitchClasses: List<Int> = listOf(0, 2, 4, 5, 7, 9, 11)

/** Ce qui est juste, et ce qui ne l'est pas. */
object QuizRules {
    /** Toutes les cases de la zone qui répondent à la question. */
    fun correctPositions(board: Fretboard, zone: FretZone, question: Question.Find): List<FretPosition> =
        board.positionsOf(question.pitchClass, zone)
            .filter { question.string == null || it.string == question.string }
            .filter { question.area == null || it in question.area }

    fun isCorrect(board: Fretboard, zone: FretZone, question: Question.Find, position: FretPosition): Boolean =
        position.fret in zone &&
            board.pitchAt(position).pitchClass == question.pitchClass &&
            (question.string == null || position.string == question.string) &&
            (question.area == null || position in question.area)

    /** La fenêtre des questions « n'importe où » : quatre cases sur trois cordes voisines. */
    const val AREA_FRETS = 4
    const val AREA_STRINGS = 3

    /**
     * Une fenêtre de [AREA_FRETS] cases et [AREA_STRINGS] cordes voisines (moins si la zone ou l'instrument sont plus petits) qui
     * contient [target] : la note y est donc toujours accessible. Sa place autour de [target] est tirée au sort.
     */
    fun areaAround(board: Fretboard, zone: FretZone, target: FretPosition, random: Random): FretArea {
        val width = minOf(AREA_FRETS, zone.last - zone.first + 1)
        val firstFret = random.nextInt(maxOf(zone.first, target.fret - width + 1), minOf(target.fret, zone.last - width + 1) + 1)
        val count = minOf(AREA_STRINGS, board.instrument.stringCount)
        val firstString = random.nextInt(maxOf(0, target.string - count + 1), minOf(target.string, board.instrument.stringCount - count) + 1)
        return FretArea(FretZone(firstFret, firstFret + width - 1), firstString, firstString + count - 1)
    }

    fun isCorrect(question: Question.Name, pitchClass: Int): Boolean = question.pitchClass == pitchClass

    fun isCorrect(question: Question.NameChord, root: Int, quality: ChordQuality): Boolean =
        question.spec.chord == Chord(root, quality)

    fun isCorrect(question: Question.EarChord, quality: ChordQuality): Boolean = question.spec.chord.quality == quality

    /** Le rang, dans l'accord, de la note que doit jouer la corde [string] d'une forme demandée, ou null hors du groupe. */
    fun placementRole(spec: ChordSpec, string: Int): Int? {
        val count = spec.chord.quality.toneCount
        if (string !in spec.start until spec.start + count) return null
        return (spec.inversion + string - spec.start) % count
    }

    /**
     * Juge les cases touchées pour une forme demandée : vrai pour chaque case dont la note est celle que sa corde doit
     * jouer (au renversement demandé). Seule la note compte, pas l'octave : toute case juste est acceptée.
     */
    fun judgePlacement(board: Fretboard, spec: ChordSpec, picked: List<FretPosition>): List<Boolean> = picked.map { position ->
        val role = placementRole(spec, position.string)
        role != null && board.pitchAt(position).pitchClass == spec.chord.pitchClasses[role]
    }

    /** Vrai quand chaque corde du groupe a reçu une case. */
    fun placementComplete(spec: ChordSpec, picked: List<FretPosition>): Boolean =
        (spec.start until spec.start + spec.chord.quality.toneCount).all { string -> picked.any { it.string == string } }

    /** Une forme juste à montrer comme correction (la plus basse de la zone), ou null s'il n'y en a pas. */
    fun correctVoicing(board: Fretboard, zone: FretZone, spec: ChordSpec): Voicing? =
        Voicings.find(board, zone, spec.chord, spec.inversion, spec.start).firstOrNull()
}

/** Ce qui fournit les questions d'une série. */
interface QuestionSource {
    /** La question suivante, différente de [previous] quand c'est possible. */
    fun next(previous: Question? = null): Question
}

/** Tirage au sort pondéré dans [all] : [weight] rend le poids de chaque question ; [previous] est évitée sauf s'il n'y a qu'un choix. */
private fun draw(all: List<Question>, previous: Question?, random: Random, weight: (Question) -> Double): Question {
    val pool = if (all.size > 1) all.filter { it != previous } else all
    val weights = pool.map { weight(it).coerceAtLeast(MIN_WEIGHT) }
    var draw = random.nextDouble() * weights.sum()
    for (i in pool.indices) {
        draw -= weights[i]
        if (draw <= 0.0) return pool[i]
    }
    return pool.last()
}

private const val MIN_WEIGHT = 0.01

/**
 * Tire les questions sur les notes d'une série. Toutes les questions possibles sont dressées d'avance (selon l'instrument,
 * la zone, les cordes et les notes retenues) puis tirées au sort, chacune avec un poids : [weight] rend ce poids pour une
 * note, ce qui permet de revenir plus souvent sur celles qu'on rate. La même question ne tombe jamais deux fois de suite,
 * sauf s'il n'y en a qu'une.
 */
class QuestionGenerator(
    private val board: Fretboard,
    private val zone: FretZone,
    private val config: QuizConfig,
    private val weight: (Int) -> Double = { 1.0 },
    private val random: Random = Random.Default,
    private val cellFactor: (FretPosition) -> Double = { 1.0 },
) : QuestionSource {
    private val all: List<Question>

    init {
        require(!config.mode.isChord) { "Pour les accords : ChordQuestionGenerator" }
        val allStrings = (0 until board.instrument.stringCount).toList()
        val strings = if (config.mode == QuizMode.FIND_ANYWHERE) {
            allStrings
        } else {
            config.strings.filter { it in allStrings }.sorted().ifEmpty { allStrings }
        }
        fun positions(naturalsOnly: Boolean) = strings
            .flatMap { string -> zone.frets.map { fret -> FretPosition(string, fret) } }
            .filter { !naturalsOnly || board.pitchAt(it).pitchClass in NaturalPitchClasses }
        // Une zone minuscule peut ne contenir aucune note naturelle : mieux vaut interroger une altérée que rien.
        val candidates = positions(config.naturalsOnly).ifEmpty { positions(false) }
        all = when (config.mode) {
            QuizMode.FIND_ON_STRING -> candidates.map { Question.Find(board.pitchAt(it).pitchClass, it.string) }.distinct()
            QuizMode.FIND_ANYWHERE -> candidates.map { Question.Find(board.pitchAt(it).pitchClass, null) }.distinct()
            else -> candidates.map { Question.Name(it, board.pitchAt(it).pitchClass) }
        }
    }

    override fun next(previous: Question?): Question {
        // Les questions « n'importe où » sont dressées sans fenêtre : on compare la précédente sans la sienne, puis on en tire une.
        val before = if (previous is Question.Find) previous.copy(area = null) else previous
        val question = draw(all, before, random) { weight(it.pitchClass) * positionFactor(it) }
        if (config.mode != QuizMode.FIND_ANYWHERE || question !is Question.Find) return question
        val target = board.positionsOf(question.pitchClass, zone).random(random)
        return question.copy(area = QuizRules.areaAround(board, zone, target, random))
    }

    /**
     * Le poids de la case visée par une question : la case montrée pour « nommer », la plus faible des cases possibles pour
     * « trouver sur une corde » ; 1 quand la question ne vise pas une case précise.
     */
    private fun positionFactor(question: Question): Double = when (question) {
        is Question.Name -> cellFactor(question.position)
        is Question.Find ->
            if (question.string == null) {
                1.0
            } else {
                board.positionsOf(question.pitchClass, zone).filter { it.string == question.string }.maxOfOrNull(cellFactor) ?: 1.0
            }
        else -> 1.0
    }
}

/**
 * Tire les questions d'accords d'une série. Toutes les formes que la zone permet (accord × renversement × groupe de cordes,
 * pour les fondamentales, les familles et les renversements retenus) sont dressées d'avance puis tirées au sort, avec un
 * poids par famille d'accord : [weight] permet de revenir plus souvent sur celles qu'on rate. [available] est faux quand la
 * zone est trop petite pour contenir la moindre forme : il n'y a alors pas de série possible.
 */
class ChordQuestionGenerator(
    board: Fretboard,
    zone: FretZone,
    private val config: QuizConfig,
    private val weight: (ChordQuality) -> Double = { 1.0 },
    private val random: Random = Random.Default,
) : QuestionSource {
    private val all: List<Question>

    val available: Boolean get() = all.isNotEmpty()

    init {
        require(config.mode.isChord) { "Pour les notes : QuestionGenerator" }
        val qualities = ChordQuality.entries.filter { config.chordPool == ChordPool.ALL || !it.isSeventh }

        fun questions(naturalRootsOnly: Boolean): List<Question> = buildList {
            val roots = if (naturalRootsOnly) NaturalPitchClasses else (0..11).toList()
            for (root in roots) {
                for (quality in qualities) {
                    val chord = Chord(root, quality)
                    val inversions = if (config.chordInversions) 0 until quality.toneCount else 0..0
                    for (inversion in inversions) {
                        for (start in Voicings.stringSets(board.instrument, quality.toneCount)) {
                            val voicings = Voicings.find(board, zone, chord, inversion, start)
                            if (voicings.isEmpty()) continue
                            val spec = ChordSpec(chord, inversion, start)
                            when (config.mode) {
                                QuizMode.NAME_CHORD -> voicings.forEach { add(Question.NameChord(spec, it.positions)) }
                                QuizMode.PLACE_CHORD -> add(Question.PlaceChord(spec))
                                else -> add(Question.EarChord(spec, voicings.first().positions))
                            }
                        }
                    }
                }
            }
        }
        all = questions(config.naturalsOnly).ifEmpty { questions(false) }
    }

    override fun next(previous: Question?): Question {
        check(available) { "Aucune forme d'accord dans cette zone" }
        return draw(all, previous, random) { weight(it.chordQuality ?: ChordQuality.MAJOR) }
    }
}
