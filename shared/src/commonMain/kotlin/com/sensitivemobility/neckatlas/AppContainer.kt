package com.sensitivemobility.neckatlas

import com.sensitivemobility.neckatlas.audio.NotePlayer
import com.sensitivemobility.neckatlas.audio.createPcmOutput
import com.sensitivemobility.neckatlas.audio.createSampleBank
import com.sensitivemobility.neckatlas.data.prefs.SettingsRepository
import com.sensitivemobility.neckatlas.data.prefs.createSettingsDataStore
import com.sensitivemobility.neckatlas.data.stats.StatsRepository

/**
 * Les services partagés de l'appli, créés une fois par processus par chaque plateforme (Application sur Android, point
 * d'entrée sur iOS) puis passés à [App]. Pas de bibliothèque d'injection : tant qu'il n'y en a que quelques-uns, une
 * classe suffit.
 *
 * @param settingsFilePath chemin absolu du fichier des préférences, dans le dossier de données privé de l'appli
 * (voir `SETTINGS_FILE_NAME` pour son nom).
 */
class AppContainer(settingsFilePath: String) {
    // Un seul fichier pour tout ce que l'appli retient : DataStore refuse deux ouvertures du même fichier.
    private val store = createSettingsDataStore(settingsFilePath)
    val settings = SettingsRepository(store)
    val stats = StatsRepository(store)
    val notes = NotePlayer(createPcmOutput(), createSampleBank())
}
