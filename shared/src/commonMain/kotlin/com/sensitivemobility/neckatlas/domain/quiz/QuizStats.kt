package com.sensitivemobility.neckatlas.domain.quiz

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality

/**
 * Une réponse donnée pendant une série. [millis] : le temps mis à répondre. [quality] : pour une question d'accord, la famille
 * de l'accord interrogé (la réponse compte alors pour cette famille, et non pour la note [pitchClass]). [positions] : les cases
 * du manche à créditer pour la carte de chaleur : la case touchée si la réponse est juste, la ou les cases qu'il fallait
 * trouver si elle est fausse ; vide quand la question ne vise pas une case précise.
 */
data class Answer(
    val pitchClass: Int,
    val correct: Boolean,
    val millis: Long,
    val quality: ChordQuality? = null,
    val positions: List<FretPosition> = emptyList(),
)

/** Les résultats cumulés d'une note (ou d'une famille d'accords), toutes séries confondues. */
data class NoteStat(val attempts: Int = 0, val correct: Int = 0, val totalMillis: Long = 0L) {
    /** Part de réponses justes (0 à 1), ou null si elle n'a jamais été demandée. */
    val accuracy: Double? get() = if (attempts == 0) null else correct.toDouble() / attempts

    /** Temps moyen de réponse, ou null si elle n'a jamais été demandée. */
    val averageMillis: Long? get() = if (attempts == 0) null else totalMillis / attempts

    operator fun plus(answer: Answer): NoteStat = NoteStat(
        attempts = attempts + 1,
        correct = correct + if (answer.correct) 1 else 0,
        // Un téléphone posé en cours de série ne doit pas fausser la moyenne.
        totalMillis = totalMillis + answer.millis.coerceIn(0L, MAX_ANSWER_MILLIS),
    )

    companion object {
        const val MAX_ANSWER_MILLIS = 30_000L
    }
}

/** Une série terminée, pour l'historique. */
data class SessionEntry(
    val epochMillis: Long,
    val mode: QuizMode,
    val correct: Int,
    val answered: Int,
    val durationMillis: Long,
)

/**
 * Tout ce que l'appli retient du quiz : les résultats de chacune des 12 notes, de chacune des familles d'accords, de chaque
 * case du manche (pour chaque instrument : la carte de chaleur), et les dernières séries.
 */
data class QuizStats(
    val notes: List<NoteStat> = List(12) { NoteStat() },
    val history: List<SessionEntry> = emptyList(),
    val qualities: List<NoteStat> = List(ChordQuality.entries.size) { NoteStat() },
    val cells: Map<Instrument, Map<FretPosition, NoteStat>> = emptyMap(),
) {
    init {
        require(notes.size == 12) { "Il faut les résultats des 12 notes, pas ${notes.size}" }
        require(qualities.size == ChordQuality.entries.size) { "Il faut les résultats des ${ChordQuality.entries.size} familles d'accords" }
    }

    val answered: Int get() = notes.sumOf { it.attempts } + qualities.sumOf { it.attempts }
    val correct: Int get() = notes.sumOf { it.correct } + qualities.sumOf { it.correct }

    /** Poids d'un résultat dans le tirage : 1 pour une réussite constante, jusqu'à 5 pour un échec constant, 3 sans historique. */
    private fun weightOf(stat: NoteStat): Double {
        // Le taux d'échec est lissé (comme si l'élément avait déjà un échec et un succès) pour qu'une seule réponse ne fasse
        // pas basculer le poids.
        val missRate = (stat.attempts - stat.correct + 1.0) / (stat.attempts + 2.0)
        return 1.0 + 4.0 * missRate
    }

    /** Poids d'une note dans le tirage des questions sur les notes. */
    fun weight(pitchClass: Int): Double = weightOf(notes[pitchClass])

    /** Poids d'une famille d'accords dans le tirage des questions d'accords. */
    fun weight(quality: ChordQuality): Double = weightOf(qualities[quality.ordinal])

    /** Les résultats case par case d'un instrument (seules les cases déjà interrogées y figurent). */
    fun cellStats(instrument: Instrument): Map<FretPosition, NoteStat> = cells[instrument].orEmpty()

    /**
     * Le poids d'une case par rapport à une case neuve : 1 pour une case jamais interrogée, plus d'un pour une case ratée,
     * moins d'un pour une case maîtrisée (de 1/3 à 5/3). Il se multiplie au poids de la note : les cases où l'on se trompe
     * reviennent plus souvent, sans que le tirage change tant qu'on n'a rien de plus à dire.
     */
    fun cellFactor(instrument: Instrument, position: FretPosition): Double =
        weightOf(cells[instrument]?.get(position) ?: NoteStat()) / UNSEEN_WEIGHT

    /** Les notes à retravailler : demandées au moins [minAttempts] fois et réussies à moins de 90 %, les pires d'abord. */
    fun weakest(limit: Int = 3, minAttempts: Int = 3): List<Int> = weakestIndices(notes, limit, minAttempts)

    /** Pareil pour les familles d'accords. */
    fun weakestQualities(limit: Int = 3, minAttempts: Int = 3): List<ChordQuality> =
        weakestIndices(qualities, limit, minAttempts).map { ChordQuality.entries[it] }

    private fun weakestIndices(stats: List<NoteStat>, limit: Int, minAttempts: Int): List<Int> = stats.indices
        .filter { stats[it].attempts >= minAttempts && (stats[it].accuracy ?: 1.0) < WEAK_BELOW }
        .sortedWith(compareBy({ stats[it].accuracy }, { -(stats[it].averageMillis ?: 0L) }))
        .take(limit)

    /**
     * Ajoute les réponses d'une série (et la série à l'historique, qui garde les [keepHistory] dernières). Les cases des
     * réponses ([Answer.positions]) sont créditées à [instrument] ; sans instrument, elles sont ignorées.
     */
    fun withAnswers(answers: List<Answer>, entry: SessionEntry?, keepHistory: Int = 30, instrument: Instrument? = null): QuizStats {
        val updatedNotes = notes.toMutableList()
        val updatedQualities = qualities.toMutableList()
        val updatedCells = instrument?.let { cells[it].orEmpty().toMutableMap() }
        for (answer in answers) {
            val quality = answer.quality
            if (quality != null) {
                updatedQualities[quality.ordinal] = updatedQualities[quality.ordinal] + answer
            } else {
                updatedNotes[answer.pitchClass] = updatedNotes[answer.pitchClass] + answer
            }
            if (updatedCells != null) {
                for (position in answer.positions) updatedCells[position] = (updatedCells[position] ?: NoteStat()) + answer
            }
        }
        val newHistory = if (entry == null) history else (history + entry).takeLast(keepHistory)
        val newCells = if (instrument != null && updatedCells != null) cells + (instrument to updatedCells) else cells
        return QuizStats(updatedNotes, newHistory, updatedQualities, newCells)
    }

    private companion object {
        const val WEAK_BELOW = 0.9

        /** Le poids d'un élément jamais interrogé : voir [weight]. */
        const val UNSEEN_WEIGHT = 3.0
    }
}
