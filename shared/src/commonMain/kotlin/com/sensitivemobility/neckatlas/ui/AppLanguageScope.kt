package com.sensitivemobility.neckatlas.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.sensitivemobility.neckatlas.data.prefs.AppLanguage

/** Le code de langue (BCP 47) à appliquer, ou `null` pour suivre le téléphone. */
val AppLanguage.tag: String?
    get() = when (this) {
        AppLanguage.SYSTEM -> null
        AppLanguage.FRENCH -> "fr"
        AppLanguage.ENGLISH -> "en"
    }

/**
 * Fait de [language] la langue de tous les textes de [content] : les ressources (`stringResource`) sont choisies d'après la
 * langue de la plateforme, que [ProvideAppLanguage] remplace par celle de l'utilisateur.
 *
 * Quand la langue change, ce qui est dessous est reconstruit : rien d'autre n'avertit les textes déjà affichés (la langue
 * n'est pas un état observable de Compose). L'état qui doit survivre à ce changement (l'onglet ouvert, une série de quiz en
 * cours) se garde donc au-dessus de cet appel.
 */
@Composable
fun AppLanguageScope(language: AppLanguage, content: @Composable () -> Unit) {
    ProvideAppLanguage(language.tag) {
        key(language) { content() }
    }
}

/**
 * Applique la langue [tag] (`null` : celle du système) pour le contenu. À la charge de chaque plateforme, car chacune lit sa
 * langue ailleurs : voir `AppLanguageScope.android.kt` et `AppLanguageScope.ios.kt`.
 */
@Composable
internal expect fun ProvideAppLanguage(tag: String?, content: @Composable () -> Unit)
