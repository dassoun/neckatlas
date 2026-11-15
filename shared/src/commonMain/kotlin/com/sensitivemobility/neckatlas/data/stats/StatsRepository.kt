package com.sensitivemobility.neckatlas.data.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.quiz.Answer
import com.sensitivemobility.neckatlas.domain.quiz.QuizStats
import com.sensitivemobility.neckatlas.domain.quiz.SessionEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private object Keys {
    val notes = stringPreferencesKey("quiz_notes")
    val qualities = stringPreferencesKey("quiz_qualities")
    val history = stringPreferencesKey("quiz_history")

    /** Une clé par instrument : les cases d'une guitare 6 et d'une basse n'ont rien à voir. */
    fun cells(instrument: Instrument) = stringPreferencesKey("quiz_cells_${instrument.name}")
}

private fun Preferences.toStats(): QuizStats = QuizStats(
    notes = StatsCodec.decodeNotes(this[Keys.notes]),
    history = StatsCodec.decodeHistory(this[Keys.history]),
    qualities = StatsCodec.decodeQualities(this[Keys.qualities]),
    cells = Instrument.entries
        .associateWith { StatsCodec.decodeCells(this[Keys.cells(it)]) }
        .filterValues { it.isNotEmpty() },
)

/** Les statistiques du quiz, enregistrées sur le téléphone (dans le même fichier de préférences que les réglages). */
class StatsRepository internal constructor(private val store: DataStore<Preferences>) {
    val stats: Flow<QuizStats> = store.data.map { it.toStats() }

    /**
     * Ajoute les réponses d'une série (et la série à l'historique, si [entry] est donnée), de façon atomique. Les cases des
     * réponses sont créditées à [instrument].
     */
    suspend fun record(answers: List<Answer>, entry: SessionEntry?, instrument: Instrument? = null) {
        store.edit { preferences ->
            val updated = preferences.toStats().withAnswers(answers, entry, instrument = instrument)
            preferences[Keys.notes] = StatsCodec.encodeNotes(updated.notes)
            preferences[Keys.qualities] = StatsCodec.encodeQualities(updated.qualities)
            preferences[Keys.history] = StatsCodec.encodeHistory(updated.history)
            if (instrument != null) preferences[Keys.cells(instrument)] = StatsCodec.encodeCells(updated.cellStats(instrument))
        }
    }

    suspend fun reset() {
        store.edit { preferences ->
            preferences.remove(Keys.notes)
            preferences.remove(Keys.qualities)
            preferences.remove(Keys.history)
            Instrument.entries.forEach { preferences.remove(Keys.cells(it)) }
        }
    }
}
