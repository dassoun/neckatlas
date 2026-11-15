package com.sensitivemobility.neckatlas.data.stats

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.quiz.NoteStat
import com.sensitivemobility.neckatlas.domain.quiz.QuizMode
import com.sensitivemobility.neckatlas.domain.quiz.SessionEntry

/**
 * Écriture des statistiques en texte court, pour les ranger dans les préférences. Les 12 notes tiennent sur une ligne
 * (`réponses,justes,millisecondes` séparés par des points-virgules, dans l'ordre Do … Si), les familles d'accords sur une
 * autre (même forme, dans l'ordre de [ChordQuality]), les cases du manche d'un instrument sur une troisième, l'historique sur
 * une quatrième (`date,exercice,justes,réponses,durée`).
 * La lecture ignore tout ce qu'elle ne comprend pas : un fichier abîmé ou écrit par une autre version ne doit jamais empêcher
 * l'appli de démarrer.
 */
internal object StatsCodec {
    private const val NOTE_COUNT = 12
    private const val MAX_STRINGS = 10
    private val qualityCount = ChordQuality.entries.size

    fun encodeNotes(notes: List<NoteStat>): String = encodeList(notes)

    fun decodeNotes(text: String?): List<NoteStat> = decodeList(text, NOTE_COUNT)

    fun encodeQualities(qualities: List<NoteStat>): String = encodeList(qualities)

    fun decodeQualities(text: String?): List<NoteStat> = decodeList(text, qualityCount)

    private fun encodeList(stats: List<NoteStat>): String = stats.joinToString(";") { "${it.attempts},${it.correct},${it.totalMillis}" }

    private fun decodeList(text: String?, size: Int): List<NoteStat> {
        val parts = text?.split(';').orEmpty()
        return List(size) { index -> parts.getOrNull(index)?.let(::decodeStat) ?: NoteStat() }
    }

    private fun decodeStat(text: String): NoteStat? {
        val fields = text.split(',')
        if (fields.size != 3) return null
        val attempts = fields[0].toIntOrNull() ?: return null
        val correct = fields[1].toIntOrNull() ?: return null
        val millis = fields[2].toLongOrNull() ?: return null
        if (attempts < 0 || correct < 0 || correct > attempts || millis < 0) return null
        return NoteStat(attempts, correct, millis)
    }

    /** Les cases d'un instrument : `corde:case:réponses,justes,millisecondes`, séparées par des points-virgules. */
    fun encodeCells(cells: Map<FretPosition, NoteStat>): String = cells.entries
        .sortedWith(compareBy({ it.key.string }, { it.key.fret }))
        .joinToString(";") { (position, stat) -> "${position.string}:${position.fret}:${stat.attempts},${stat.correct},${stat.totalMillis}" }

    fun decodeCells(text: String?): Map<FretPosition, NoteStat> = buildMap {
        for (part in text?.split(';').orEmpty()) {
            val fields = part.split(':')
            if (fields.size != 3) continue
            val string = fields[0].toIntOrNull() ?: continue
            val fret = fields[1].toIntOrNull() ?: continue
            if (string !in 0 until MAX_STRINGS || fret !in 0..FretZone.MAX_FRET) continue
            put(FretPosition(string, fret), decodeStat(fields[2]) ?: continue)
        }
    }

    fun encodeHistory(entries: List<SessionEntry>): String =
        entries.joinToString(";") { "${it.epochMillis},${it.mode.name},${it.correct},${it.answered},${it.durationMillis}" }

    fun decodeHistory(text: String?): List<SessionEntry> = text?.split(';').orEmpty().mapNotNull(::decodeEntry)

    private fun decodeEntry(text: String): SessionEntry? {
        val fields = text.split(',')
        if (fields.size != 5) return null
        val epoch = fields[0].toLongOrNull() ?: return null
        val mode = QuizMode.entries.firstOrNull { it.name == fields[1] } ?: return null
        val correct = fields[2].toIntOrNull() ?: return null
        val answered = fields[3].toIntOrNull() ?: return null
        val duration = fields[4].toLongOrNull() ?: return null
        if (correct < 0 || answered < correct || duration < 0) return null
        return SessionEntry(epoch, mode, correct, answered, duration)
    }
}
