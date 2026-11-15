package com.sensitivemobility.neckatlas.ui

import androidx.compose.runtime.Composable
import platform.Foundation.NSUserDefaults

/**
 * Sur iOS les ressources de Compose sont choisies d'après `NSLocale.preferredLanguages`, que la clé `AppleLanguages` des
 * préférences de l'appli remplace ; la supprimer rend la langue du système. Méthode de la documentation de Compose
 * Multiplatform (« Manage local resource environment »).
 *
 * NON COMPILÉ NI ESSAYÉ : pas de Mac. À vérifier au premier passage sur Xcode, en particulier que le changement se voit tout de
 * suite et non au prochain lancement de l'appli.
 */
@Composable
internal actual fun ProvideAppLanguage(tag: String?, content: @Composable () -> Unit) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (tag == null) {
        defaults.removeObjectForKey(APPLE_LANGUAGES)
    } else {
        defaults.setObject(listOf(tag), forKey = APPLE_LANGUAGES)
    }
    content()
}

private const val APPLE_LANGUAGES = "AppleLanguages"
