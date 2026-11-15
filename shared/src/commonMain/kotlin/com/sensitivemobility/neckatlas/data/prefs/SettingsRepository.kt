package com.sensitivemobility.neckatlas.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.NoteNotation
import com.sensitivemobility.neckatlas.domain.quiz.QuizConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/** Nom du fichier des préférences, à placer dans le dossier de données privé de l'appli. */
const val SETTINGS_FILE_NAME = "neckatlas_settings.preferences_pb"

/** Ouvre le fichier de préférences situé à [path] (un chemin absolu, propre à chaque plateforme). */
internal fun createSettingsDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

private object Keys {
    val instrument = stringPreferencesKey("instrument")
    val zoneFirst = intPreferencesKey("zone_first")
    val zoneLast = intPreferencesKey("zone_last")
    val notation = stringPreferencesKey("notation")
    val accidentals = stringPreferencesKey("accidentals")
    val leftHanded = booleanPreferencesKey("left_handed")
    val soundEnabled = booleanPreferencesKey("sound_enabled")
    val quizMode = stringPreferencesKey("quiz_mode")
    val quizStrings = intPreferencesKey("quiz_strings")
    val quizNaturals = booleanPreferencesKey("quiz_naturals")
    val quizLength = stringPreferencesKey("quiz_length")
    val quizChordPool = stringPreferencesKey("quiz_chord_pool")
    val quizChordInversions = booleanPreferencesKey("quiz_chord_inversions")
    val theme = stringPreferencesKey("theme")
    val language = stringPreferencesKey("language")
    val haptics = booleanPreferencesKey("haptics")
}

class SettingsRepository internal constructor(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    /** Modifie les préférences de façon atomique : [change] reçoit les valeurs actuelles et rend les nouvelles. */
    suspend fun update(change: (AppSettings) -> AppSettings) {
        store.edit { preferences -> preferences.write(change(preferences.toSettings())) }
    }
}

private inline fun <reified E : Enum<E>> Preferences.readEnum(key: Preferences.Key<String>, default: E): E {
    val name = this[key] ?: return default
    return enumValues<E>().firstOrNull { it.name == name } ?: default
}

// Les cordes retenues du quiz tiennent dans un entier : le bit n est levé si la corde d'indice n est interrogée (7 au plus).
private fun stringsToMask(strings: Set<Int>): Int = strings.filter { it in 0..9 }.fold(0) { mask, string -> mask or (1 shl string) }

private fun maskToStrings(mask: Int): Set<Int> = (0..9).filter { mask and (1 shl it) != 0 }.toSet()

private fun Preferences.toSettings(): AppSettings {
    val defaults = AppSettings()
    val first = this[Keys.zoneFirst]
    val last = this[Keys.zoneLast]
    // Une valeur illisible (fichier ancien ou abîmé) ne doit jamais empêcher l'appli de démarrer.
    val zone = if (first != null && last != null) runCatching { FretZone(first, last) }.getOrNull() else null
    return AppSettings(
        instrument = readEnum(Keys.instrument, defaults.instrument),
        zone = zone ?: defaults.zone,
        notation = readEnum(Keys.notation, defaults.notation),
        accidentals = readEnum(Keys.accidentals, defaults.accidentals),
        leftHanded = this[Keys.leftHanded] ?: defaults.leftHanded,
        soundEnabled = this[Keys.soundEnabled] ?: defaults.soundEnabled,
        quiz = QuizConfig(
            mode = readEnum(Keys.quizMode, defaults.quiz.mode),
            strings = maskToStrings(this[Keys.quizStrings] ?: 0),
            naturalsOnly = this[Keys.quizNaturals] ?: defaults.quiz.naturalsOnly,
            length = readEnum(Keys.quizLength, defaults.quiz.length),
            chordPool = readEnum(Keys.quizChordPool, defaults.quiz.chordPool),
            chordInversions = this[Keys.quizChordInversions] ?: defaults.quiz.chordInversions,
        ),
        theme = readEnum(Keys.theme, defaults.theme),
        language = readEnum(Keys.language, defaults.language),
        haptics = this[Keys.haptics] ?: defaults.haptics,
    )
}

private fun MutablePreferences.write(settings: AppSettings) {
    this[Keys.instrument] = settings.instrument.name
    this[Keys.zoneFirst] = settings.zone.first
    this[Keys.zoneLast] = settings.zone.last
    this[Keys.notation] = settings.notation.name
    this[Keys.accidentals] = settings.accidentals.name
    this[Keys.leftHanded] = settings.leftHanded
    this[Keys.soundEnabled] = settings.soundEnabled
    this[Keys.quizMode] = settings.quiz.mode.name
    this[Keys.quizStrings] = stringsToMask(settings.quiz.strings)
    this[Keys.quizNaturals] = settings.quiz.naturalsOnly
    this[Keys.quizLength] = settings.quiz.length.name
    this[Keys.quizChordPool] = settings.quiz.chordPool.name
    this[Keys.quizChordInversions] = settings.quiz.chordInversions
    this[Keys.theme] = settings.theme.name
    this[Keys.language] = settings.language.name
    this[Keys.haptics] = settings.haptics
}
