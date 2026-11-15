package com.sensitivemobility.neckatlas.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sensitivemobility.neckatlas.domain.Instrument
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Teste le choix de la langue contre un vrai fichier de préférences (DataStore), comme sur le téléphone. */
class SettingsLanguageTest {
    private fun newStore(): DataStore<Preferences> {
        val dir = Files.createTempDirectory("neckatlas-settings-test")
        return createSettingsDataStore(dir.resolve(SETTINGS_FILE_NAME).toString())
    }

    private fun SettingsRepository.read(): AppSettings = runBlocking { settings.first() }

    @Test
    fun followsThePhoneOnAFirstLaunch() {
        assertEquals(AppLanguage.SYSTEM, SettingsRepository(newStore()).read().language)
    }

    @Test
    fun remembersEachChosenLanguage() {
        val repository = SettingsRepository(newStore())
        for (language in AppLanguage.entries) {
            runBlocking { repository.update { it.copy(language = language) } }
            assertEquals(language, repository.read().language)
        }
    }

    @Test
    fun choosingALanguageKeepsTheOtherSettings() {
        val repository = SettingsRepository(newStore())
        runBlocking { repository.update { it.copy(theme = ThemeMode.LIGHT, leftHanded = true, instrument = Instrument.BASS_5) } }
        runBlocking { repository.update { it.copy(language = AppLanguage.FRENCH) } }
        val settings = repository.read()
        assertEquals(AppLanguage.FRENCH, settings.language)
        assertEquals(ThemeMode.LIGHT, settings.theme)
        assertEquals(true, settings.leftHanded)
        assertEquals(Instrument.BASS_5, settings.instrument)
    }

    @Test
    fun anUnreadableStoredLanguageFallsBackToThePhone() {
        val store = newStore()
        runBlocking { store.edit { it[stringPreferencesKey("language")] = "KLINGON" } }
        assertEquals(AppLanguage.SYSTEM, SettingsRepository(store).read().language)
    }

    @Test
    fun settingsFromBeforeTheLanguageExistedStillLoad() {
        // Un fichier écrit par une version sans choix de langue n'a pas la clé : l'appli doit démarrer, en langue du téléphone.
        val store = newStore()
        runBlocking { store.edit { it[stringPreferencesKey("theme")] = ThemeMode.LIGHT.name } }
        val settings = SettingsRepository(store).read()
        assertEquals(AppLanguage.SYSTEM, settings.language)
        assertEquals(ThemeMode.LIGHT, settings.theme)
    }
}
