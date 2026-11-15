package com.sensitivemobility.neckatlas.data.prefs

import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.NoteNotation
import com.sensitivemobility.neckatlas.domain.quiz.QuizConfig

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** La langue des textes de l'appli : celle du téléphone par défaut, ou une langue choisie dans les réglages. */
enum class AppLanguage { SYSTEM, FRENCH, ENGLISH }

/** Les préférences de l'utilisateur, enregistrées sur le téléphone. Les valeurs par défaut sont celles d'une première ouverture. */
data class AppSettings(
    val instrument: Instrument = Instrument.GUITAR_6,
    /** Zone du manche sur laquelle on travaille : les 12 premières cases par défaut. */
    val zone: FretZone = FretZone.DEFAULT,
    val notation: NoteNotation = NoteNotation.SOLFEGE,
    val accidentals: AccidentalStyle = AccidentalStyle.BOTH,
    /** Manche inversé (sillet à droite). */
    val leftHanded: Boolean = false,
    /** Joue la note quand on touche le manche. */
    val soundEnabled: Boolean = true,
    /** Le dernier réglage du quiz, repris à l'ouverture suivante. */
    val quiz: QuizConfig = QuizConfig(),
    val theme: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Un léger retour vibré quand on touche le manche. */
    val haptics: Boolean = true,
    /** Le diapason de l'accordeur : la fréquence du La4, en Hz. */
    val tunerReferenceHz: Int = 440,
)
