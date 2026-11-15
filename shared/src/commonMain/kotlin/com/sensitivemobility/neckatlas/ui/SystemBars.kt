package com.sensitivemobility.neckatlas.ui

import androidx.compose.runtime.Composable

/**
 * Adapte les icônes de la barre d'état et de navigation au thème de l'appli (claires sur fond sombre et inversement), qui
 * peut différer de celui du système.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
