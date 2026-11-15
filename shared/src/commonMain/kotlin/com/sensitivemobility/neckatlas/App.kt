package com.sensitivemobility.neckatlas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.quiz.QuizStats
import com.sensitivemobility.neckatlas.feature.chords.ChordsScreen
import com.sensitivemobility.neckatlas.feature.fretboard.FretboardScreen
import com.sensitivemobility.neckatlas.feature.progress.ProgressScreen
import com.sensitivemobility.neckatlas.feature.quiz.QuizController
import com.sensitivemobility.neckatlas.feature.quiz.QuizScreen
import com.sensitivemobility.neckatlas.feature.scales.ScalesScreen
import com.sensitivemobility.neckatlas.feature.settings.SettingsScreen
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.tab_chords
import com.sensitivemobility.neckatlas.shared.resources.tab_fretboard
import com.sensitivemobility.neckatlas.shared.resources.tab_progress
import com.sensitivemobility.neckatlas.shared.resources.tab_quiz
import com.sensitivemobility.neckatlas.shared.resources.tab_scales
import com.sensitivemobility.neckatlas.shared.resources.tab_settings
import com.sensitivemobility.neckatlas.ui.AppLanguageScope
import com.sensitivemobility.neckatlas.ui.LocalHapticsEnabled
import com.sensitivemobility.neckatlas.ui.NeckAtlasTheme
import com.sensitivemobility.neckatlas.ui.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Durée d'affichage de l'écran de démarrage de l'appli (le logo complet), avant le fondu vers le contenu. */
private const val SPLASH_DURATION_MS = 1500L

/** Les écrans de la barre du bas. Chaque nouvelle fonctionnalité s'ajoute ici. */
private enum class Tab(val icon: ImageVector, val label: StringResource) {
    FRETBOARD(Icons.Filled.MusicNote, Res.string.tab_fretboard),
    CHORDS(Icons.Filled.LibraryMusic, Res.string.tab_chords),
    SCALES(Icons.Filled.Stairs, Res.string.tab_scales),
    QUIZ(Icons.Filled.Quiz, Res.string.tab_quiz),
    PROGRESS(Icons.Filled.Insights, Res.string.tab_progress),
    SETTINGS(Icons.Filled.Settings, Res.string.tab_settings),
}

@Composable
fun App(container: AppContainer) {
    // rememberSaveable : après une rotation, l'écran de démarrage ne réapparaît pas.
    var splashVisible by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        splashVisible = false
    }

    Box(Modifier.fillMaxSize()) {
        // Le contenu se prépare dessous (lecture des préférences) : quand le logo s'efface, tout est prêt.
        AppContent(container)
        AnimatedVisibility(visible = splashVisible, enter = EnterTransition.None, exit = fadeOut(tween(400))) {
            SplashScreen()
        }
    }
}

@Composable
private fun AppContent(container: AppContainer) {
    val loaded by container.settings.settings.collectAsState(initial = null)
    // On attend la lecture des préférences pour ne pas afficher une première image avec le mauvais thème.
    val settings: AppSettings = loaded ?: return
    val stats by container.stats.stats.collectAsState(initial = QuizStats())
    val scope = rememberCoroutineScope()
    val onChange: ((AppSettings) -> AppSettings) -> Unit = { change -> scope.launch { container.settings.update(change) } }
    // Au-dessus des onglets : une série de quiz en cours survit à un coup d'œil aux réglages.
    val quiz = remember(container) { QuizController(scope, container.notes, container.stats) }

    NeckAtlasTheme(settings.theme) {
        CompositionLocalProvider(LocalHapticsEnabled provides settings.haptics) {
            // Avant la portée de langue, qui reconstruit tout ce qu'elle contient quand la langue change : on change la langue
            // depuis l'onglet des réglages, et on ne veut pas en être renvoyé.
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            AppLanguageScope(settings.language) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = index == selectedTab,
                                    onClick = { selectedTab = index },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(stringResource(tab.label)) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        when (Tab.entries[selectedTab]) {
                            Tab.FRETBOARD -> FretboardScreen(settings, onChange, container.notes)
                            Tab.CHORDS -> ChordsScreen(settings, onChange, container.notes)
                            Tab.SCALES -> ScalesScreen(settings, onChange, container.notes)
                            Tab.QUIZ -> QuizScreen(quiz, settings, stats, onChange)
                            Tab.PROGRESS -> ProgressScreen(
                                stats,
                                settings,
                                onReset = { scope.launch { container.stats.reset() } },
                                onStartQuiz = { selectedTab = Tab.QUIZ.ordinal },
                            )
                            Tab.SETTINGS -> SettingsScreen(settings, onChange)
                        }
                    }
                }
            }
        }
    }
}
