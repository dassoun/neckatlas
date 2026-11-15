package com.sensitivemobility.neckatlas.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Teste le réglage du retour vibré contre un vrai fichier de préférences (DataStore), comme sur le téléphone. */
class SettingsHapticsTest {
    private fun newStore(): DataStore<Preferences> {
        val dir = Files.createTempDirectory("neckatlas-settings-test")
        return createSettingsDataStore(dir.resolve(SETTINGS_FILE_NAME).toString())
    }

    private fun SettingsRepository.read(): AppSettings = runBlocking { settings.first() }

    @Test
    fun isOnByDefault() {
        assertEquals(true, SettingsRepository(newStore()).read().haptics)
        assertEquals(true, AppSettings().haptics)
    }

    @Test
    fun remembersBothChoices() {
        val repository = SettingsRepository(newStore())
        runBlocking { repository.update { it.copy(haptics = false) } }
        assertEquals(false, repository.read().haptics)
        runBlocking { repository.update { it.copy(haptics = true) } }
        assertEquals(true, repository.read().haptics)
    }

    @Test
    fun turningItOffKeepsTheOtherSettings() {
        val repository = SettingsRepository(newStore())
        runBlocking { repository.update { it.copy(language = AppLanguage.FRENCH, leftHanded = true, soundEnabled = false) } }
        runBlocking { repository.update { it.copy(haptics = false) } }
        val settings = repository.read()
        assertEquals(false, settings.haptics)
        assertEquals(AppLanguage.FRENCH, settings.language)
        assertEquals(true, settings.leftHanded)
        assertEquals(false, settings.soundEnabled)
    }

    @Test
    fun settingsFromBeforeTheOptionExistedStillLoadWithItOn() {
        // Un fichier écrit par une version sans ce réglage n'a pas la clé : l'appli doit démarrer, retour vibré activé.
        val store = newStore()
        runBlocking { store.edit { it[stringPreferencesKey("theme")] = ThemeMode.LIGHT.name } }
        val settings = SettingsRepository(store).read()
        assertEquals(true, settings.haptics)
        assertEquals(ThemeMode.LIGHT, settings.theme)
    }
}
