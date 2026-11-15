package com.sensitivemobility.neckatlas.ui

import com.sensitivemobility.neckatlas.data.prefs.AppLanguage
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppLanguageTest {
    @Test
    fun systemFollowsThePhone() {
        assertNull(AppLanguage.SYSTEM.tag)
    }

    @Test
    fun chosenLanguagesHaveTheirOwnTag() {
        assertEquals("fr", AppLanguage.FRENCH.tag)
        assertEquals("en", AppLanguage.ENGLISH.tag)
    }

    @Test
    fun tagsAreDistinct() {
        val tags = AppLanguage.entries.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
        assertTrue(tags.filterNotNull().all { it.length == 2 && it == it.lowercase() }, "codes de langue sur deux lettres : $tags")
    }

    @Test
    fun aFirstLaunchFollowsThePhone() {
        assertEquals(AppLanguage.SYSTEM, AppSettings().language)
    }
}
