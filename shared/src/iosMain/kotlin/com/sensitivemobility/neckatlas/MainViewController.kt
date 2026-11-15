package com.sensitivemobility.neckatlas

import androidx.compose.ui.window.ComposeUIViewController
import com.sensitivemobility.neckatlas.data.prefs.SETTINGS_FILE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

// Une seule instance par processus : DataStore refuse deux ouvertures du même fichier.
private val container: AppContainer by lazy {
    AppContainer(documentsDirectoryPath() + "/" + SETTINGS_FILE_NAME)
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectoryPath(): String {
    val directory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(directory?.path) { "Dossier Documents introuvable" }
}

/** Appelée par SwiftUI (iosApp/iosApp/ContentView.swift) pour obtenir l'écran de l'appli. */
fun MainViewController(): UIViewController = ComposeUIViewController { App(container) }
