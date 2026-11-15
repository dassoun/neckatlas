package com.sensitivemobility.neckatlas.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sensitivemobility.neckatlas.data.prefs.ThemeMode
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.sora
import org.jetbrains.compose.resources.Font

// Les couleurs de la marque sont celles du logo Sensitive Mobility, comme l'écran de démarrage : bleu nuit en fond (#08111C),
// turquoise de l'anneau en accent, ambre pour ce qui est choisi ou joué.
/** Le vert d'une bonne réponse (et son texte), lisible sur fond sombre comme sur fond clair. Le rouge d'une erreur est `colorScheme.error`. */
val CorrectColor = Color(0xFF2FBF71)
val OnCorrectColor = Color(0xFF03230F)

/** La couleur de la septième d'un accord (fondamentale : ambre, tierce : turquoise, quinte : bleu), et son texte. */
val SeventhColor = Color(0xFFB794F6)
val OnSeventhColor = Color(0xFF1F0A3D)

/**
 * Les couleurs qui portent un sens, fixes même si l'on change un jour la couleur de la marque : le turquoise est la tierce d'un
 * accord et une réussite correcte (70 à 89 %) sur la carte de chaleur, le bleu est la quinte. Les lier au thème les
 * confondrait avec les éléments d'interface le jour où la marque prendrait une de ces teintes.
 */
@Immutable
class AccentColors(val teal: Color, val onTeal: Color, val blue: Color, val onBlue: Color)

private val DarkAccents = AccentColors(Color(0xFF2EE6D0), Color(0xFF00201C), Color(0xFF3BB8F0), Color(0xFF00243A))
private val LightAccents = AccentColors(Color(0xFF0A8F82), Color(0xFFFFFFFF), Color(0xFF1C6FA8), Color(0xFFFFFFFF))

val LocalAccentColors = staticCompositionLocalOf { DarkAccents }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2EE6D0),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0F4A47),
    onPrimaryContainer = Color(0xFFB8F5EE),
    secondary = Color(0xFF3BB8F0),
    onSecondary = Color(0xFF00243A),
    secondaryContainer = Color(0xFF123B4D),
    onSecondaryContainer = Color(0xFFCDEEFF),
    tertiary = Color(0xFFF2B84B),
    onTertiary = Color(0xFF2A1C00),
    background = SplashBackground,
    onBackground = Color(0xFFE6EEF7),
    surface = Color(0xFF0D1826),
    onSurface = Color(0xFFE6EEF7),
    surfaceVariant = Color(0xFF16263A),
    onSurfaceVariant = Color(0xFF9FB3C8),
    surfaceContainerLowest = Color(0xFF060D15),
    surfaceContainerLow = Color(0xFF0B1622),
    surfaceContainer = Color(0xFF0F1D2D),
    surfaceContainerHigh = Color(0xFF142538),
    surfaceContainerHighest = Color(0xFF1A2E44),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000),
    errorContainer = Color(0xFF5A1D1D),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF3A4E66),
    outlineVariant = Color(0xFF22344A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A8F82),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F4EE),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF1C6FA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8F8),
    onSecondaryContainer = Color(0xFF0E1A26),
    tertiary = Color(0xFFB57A00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F8FC),
    onBackground = Color(0xFF0E1A26),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1A26),
    surfaceVariant = Color(0xFFE3ECF5),
    onSurfaceVariant = Color(0xFF4A5E73),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5FA),
    surfaceContainer = Color(0xFFE9F0F7),
    surfaceContainerHigh = Color(0xFFE3ECF5),
    surfaceContainerHighest = Color(0xFFDCE6F0),
    error = Color(0xFFD93025),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF8AA0B5),
    outlineVariant = Color(0xFFCBD7E3),
)
/**
 * Les titres, les grands chiffres et le nom de la note sont en Sora (une police géométrique, licence SIL OFL : voir
 * `files/licenses/Sora-OFL.txt`) ; le texte courant et les petites étiquettes restent dans la police du système, plus étroite
 * et plus lisible en petit. Sora est une police à graisse variable : un seul fichier donne toutes les graisses.
 */
@Composable
private fun neckAtlasTypography(): Typography {
    val sora = FontFamily(
        Font(Res.font.sora, FontWeight.Normal),
        Font(Res.font.sora, FontWeight.Medium),
        Font(Res.font.sora, FontWeight.SemiBold),
        Font(Res.font.sora, FontWeight.Bold),
    )
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = sora),
        displayMedium = base.displayMedium.copy(fontFamily = sora),
        displaySmall = base.displaySmall.copy(fontFamily = sora),
        headlineLarge = base.headlineLarge.copy(fontFamily = sora),
        headlineMedium = base.headlineMedium.copy(fontFamily = sora),
        headlineSmall = base.headlineSmall.copy(fontFamily = sora),
        titleLarge = base.titleLarge.copy(fontFamily = sora),
        titleMedium = base.titleMedium.copy(fontFamily = sora),
        titleSmall = base.titleSmall.copy(fontFamily = sora),
    )
}

@Composable
fun NeckAtlasTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SystemBarsAppearance(darkTheme = dark)
    CompositionLocalProvider(LocalAccentColors provides if (dark) DarkAccents else LightAccents) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = neckAtlasTypography(), content = content)
    }
}
