package com.sensitivemobility.neckatlas.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Le retour vibré est-il activé (Réglages) ? Posé une fois pour toute l'appli par `App`, pour que le manche, utilisé par
 * plusieurs écrans, n'ait pas à se le faire passer.
 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }
