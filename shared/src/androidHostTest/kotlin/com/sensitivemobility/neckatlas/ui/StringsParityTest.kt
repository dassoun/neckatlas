package com.sensitivemobility.neckatlas.ui

import com.sensitivemobility.neckatlas.data.prefs.AppLanguage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * L'appli se laisse régler en français ou en anglais : chaque texte doit exister dans les deux langues, avec les mêmes
 * paramètres (`%1$d`, `%2$s`…), faute de quoi un texte manquerait, ou afficherait un mauvais nombre, dans l'une des deux.
 */
class StringsParityTest {
    // Gradle lance les tests depuis le dossier du module (`shared/`).
    private val resources = File("src/commonMain/composeResources")

    private fun read(folder: String): String {
        val file = File(resources, "$folder/strings.xml")
        check(file.isFile) { "Introuvable : ${file.absolutePath}" }
        return file.readText()
    }

    /** Les textes d'un fichier : nom -> son contenu (pour les pluriels et les listes, tout ce qui est entre les balises). */
    private fun entries(xml: String): Map<String, String> {
        val withoutComments = xml.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        return Regex("<(string|plurals|string-array)\\s+name=\"([^\"]+)\"[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
            .findAll(withoutComments)
            .associate { it.groupValues[2] to it.groupValues[3] }
    }

    private fun placeholders(text: String): List<String> =
        Regex("%\\d+\\$[a-zA-Z]|%[a-zA-Z]").findAll(text).map { it.value }.sorted().toList()

    private val english = entries(read("values"))
    private val french = entries(read("values-fr"))

    @Test
    fun filesAreNotEmpty() {
        assertTrue(english.size > 100, "texte anglais : ${english.size} entrées")
        assertTrue(french.size > 100, "texte français : ${french.size} entrées")
    }

    @Test
    fun everyTextExistsInBothLanguages() {
        assertEquals(emptySet(), english.keys - french.keys, "textes absents du français")
        assertEquals(emptySet(), french.keys - english.keys, "textes absents de l'anglais")
    }

    @Test
    fun placeholdersAreTheSameInBothLanguages() {
        val differing = english.keys.filter { it in french && placeholders(english.getValue(it)) != placeholders(french.getValue(it)) }
        assertEquals(emptyList(), differing, "paramètres différents entre l'anglais et le français")
    }

    @Test
    fun noTextIsEmpty() {
        val empty = (english + french.mapKeys { "fr:" + it.key }).filterValues { it.isBlank() }.keys
        assertEquals(emptySet(), empty, "textes vides")
    }

    @Test
    fun everyChoosableLanguageHasItsResources() {
        // Chaque langue proposée dans les réglages doit avoir son dossier de textes (l'anglais est celui par défaut).
        for (language in AppLanguage.entries) {
            val tag = language.tag ?: continue
            if (tag == "en") continue
            assertTrue(File(resources, "values-$tag/strings.xml").isFile, "pas de textes pour « $tag »")
        }
    }
}
