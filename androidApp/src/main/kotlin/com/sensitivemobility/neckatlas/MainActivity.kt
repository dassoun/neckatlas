package com.sensitivemobility.neckatlas

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Doit précéder super.onCreate : bascule du thème de démarrage vers le thème normal.
        installSplashScreen()
        enableEdgeToEdge()
        // Verrouillé en vertical sur téléphone : le manche est dessiné debout, sur toute la hauteur de l'écran. Les tablettes
        // et les pliables ouverts (plus petit côté d'au moins 600 dp) restent libres de tourner.
        if (resources.configuration.smallestScreenWidthDp < LARGE_SCREEN_DP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        val container = (application as NeckAtlasApplication).container
        setContent { App(container) }
    }

    private companion object {
        const val LARGE_SCREEN_DP = 600
    }
}
