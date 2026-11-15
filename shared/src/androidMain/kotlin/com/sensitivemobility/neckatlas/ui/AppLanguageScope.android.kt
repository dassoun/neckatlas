package com.sensitivemobility.neckatlas.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Sur Android les ressources de Compose sont choisies d'après `Locale.getDefault()`, et les composants Material (descriptions
 * pour l'accessibilité…) d'après les ressources de l'activité : on change les deux. La méthode est celle de la documentation de
 * Compose Multiplatform (« Manage local resource environment »), sans modifier la `Configuration` de l'activité elle-même.
 */
@Composable
internal actual fun ProvideAppLanguage(tag: String?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // « Système » : la langue du téléphone, lue à la source. `Locale.getDefault()` est justement ce que l'on remplace ici.
    val locale = if (tag == null) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
    // Une nouvelle configuration, de valeur différente : c'est ce qui prévient les composants qui la lisent.
    val localized = remember(configuration, locale) { Configuration(configuration).apply { setLocale(locale) } }

    // Doit précéder la composition du contenu : c'est là qu'il lit la langue. Sans effet si elle n'a pas changé.
    Locale.setDefault(locale)
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(localized, context.resources.displayMetrics)

    CompositionLocalProvider(LocalConfiguration provides localized) { content() }
}
