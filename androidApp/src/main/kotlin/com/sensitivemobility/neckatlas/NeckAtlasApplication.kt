package com.sensitivemobility.neckatlas

import android.app.Application
import com.sensitivemobility.neckatlas.data.prefs.SETTINGS_FILE_NAME

class NeckAtlasApplication : Application() {
    /** Une seule instance par processus : DataStore refuse deux ouvertures du même fichier. */
    val container: AppContainer by lazy {
        val directory = filesDir.resolve("datastore").apply { mkdirs() }
        AppContainer(directory.resolve(SETTINGS_FILE_NAME).absolutePath)
    }
}
