package com.sensitivemobility.neckatlas.ui

import androidx.compose.runtime.Composable
import com.sensitivemobility.neckatlas.data.prefs.AppLanguage
import com.sensitivemobility.neckatlas.data.prefs.ThemeMode
import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.NoteNotation
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.quiz.ChordPool
import com.sensitivemobility.neckatlas.domain.scale.ScaleType
import com.sensitivemobility.neckatlas.domain.quiz.QuizLength
import com.sensitivemobility.neckatlas.domain.quiz.QuizMode
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.accidentals_both
import com.sensitivemobility.neckatlas.shared.resources.accidentals_flats
import com.sensitivemobility.neckatlas.shared.resources.accidentals_sharps
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_0
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_1
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_2
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_3
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_long_0
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_long_1
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_long_2
import com.sensitivemobility.neckatlas.shared.resources.chords_inversion_long_3
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_augmented
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_diminished
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_diminished_7
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_dominant_7
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_half_diminished
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_major
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_major_7
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_minor
import com.sensitivemobility.neckatlas.shared.resources.chords_quality_minor_7
import com.sensitivemobility.neckatlas.shared.resources.instrument_bass4
import com.sensitivemobility.neckatlas.shared.resources.instrument_bass5
import com.sensitivemobility.neckatlas.shared.resources.instrument_guitar6
import com.sensitivemobility.neckatlas.shared.resources.instrument_guitar7
import com.sensitivemobility.neckatlas.shared.resources.language_english
import com.sensitivemobility.neckatlas.shared.resources.language_french
import com.sensitivemobility.neckatlas.shared.resources.language_system
import com.sensitivemobility.neckatlas.shared.resources.notation_letters
import com.sensitivemobility.neckatlas.shared.resources.notation_solfege
import com.sensitivemobility.neckatlas.shared.resources.quiz_chords_all
import com.sensitivemobility.neckatlas.shared.resources.quiz_chords_triads
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_ear_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_ear_chord_desc
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_name_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_name_chord_desc
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_place_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_place_chord_desc
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_short_ear_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_short_name_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_short_place_chord
import com.sensitivemobility.neckatlas.shared.resources.quiz_length_10
import com.sensitivemobility.neckatlas.shared.resources.quiz_length_20
import com.sensitivemobility.neckatlas.shared.resources.quiz_length_60s
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_find_anywhere
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_find_anywhere_desc
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_find_on_string
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_find_on_string_desc
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_name_note
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_name_note_desc
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_short_find_anywhere
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_short_find_on_string
import com.sensitivemobility.neckatlas.shared.resources.quiz_mode_short_name_note
import com.sensitivemobility.neckatlas.shared.resources.scales_type_blues
import com.sensitivemobility.neckatlas.shared.resources.scales_type_dorian
import com.sensitivemobility.neckatlas.shared.resources.scales_type_harmonic_minor
import com.sensitivemobility.neckatlas.shared.resources.scales_type_locrian
import com.sensitivemobility.neckatlas.shared.resources.scales_type_lydian
import com.sensitivemobility.neckatlas.shared.resources.scales_type_major
import com.sensitivemobility.neckatlas.shared.resources.scales_type_major_pentatonic
import com.sensitivemobility.neckatlas.shared.resources.scales_type_melodic_minor
import com.sensitivemobility.neckatlas.shared.resources.scales_type_minor_pentatonic
import com.sensitivemobility.neckatlas.shared.resources.scales_type_mixolydian
import com.sensitivemobility.neckatlas.shared.resources.scales_type_natural_minor
import com.sensitivemobility.neckatlas.shared.resources.scales_type_phrygian
import com.sensitivemobility.neckatlas.shared.resources.theme_dark
import com.sensitivemobility.neckatlas.shared.resources.theme_light
import com.sensitivemobility.neckatlas.shared.resources.theme_system
import org.jetbrains.compose.resources.stringResource

// Les textes affichés pour les choix de l'utilisateur. Le domaine ne connaît pas les ressources de l'interface.

@Composable
fun Instrument.label(): String = stringResource(
    when (this) {
        Instrument.GUITAR_6 -> Res.string.instrument_guitar6
        Instrument.GUITAR_7 -> Res.string.instrument_guitar7
        Instrument.BASS_4 -> Res.string.instrument_bass4
        Instrument.BASS_5 -> Res.string.instrument_bass5
    },
)

@Composable
fun NoteNotation.label(): String = stringResource(
    when (this) {
        NoteNotation.SOLFEGE -> Res.string.notation_solfege
        NoteNotation.LETTERS -> Res.string.notation_letters
    },
)

@Composable
fun AccidentalStyle.label(): String = stringResource(
    when (this) {
        AccidentalStyle.BOTH -> Res.string.accidentals_both
        AccidentalStyle.SHARPS -> Res.string.accidentals_sharps
        AccidentalStyle.FLATS -> Res.string.accidentals_flats
    },
)

@Composable
fun QuizMode.label(): String = stringResource(
    when (this) {
        QuizMode.FIND_ON_STRING -> Res.string.quiz_mode_find_on_string
        QuizMode.FIND_ANYWHERE -> Res.string.quiz_mode_find_anywhere
        QuizMode.NAME_NOTE -> Res.string.quiz_mode_name_note
        QuizMode.NAME_CHORD -> Res.string.quiz_mode_name_chord
        QuizMode.PLACE_CHORD -> Res.string.quiz_mode_place_chord
        QuizMode.EAR_CHORD -> Res.string.quiz_mode_ear_chord
    },
)

@Composable
fun QuizMode.description(): String = stringResource(
    when (this) {
        QuizMode.FIND_ON_STRING -> Res.string.quiz_mode_find_on_string_desc
        QuizMode.FIND_ANYWHERE -> Res.string.quiz_mode_find_anywhere_desc
        QuizMode.NAME_NOTE -> Res.string.quiz_mode_name_note_desc
        QuizMode.NAME_CHORD -> Res.string.quiz_mode_name_chord_desc
        QuizMode.PLACE_CHORD -> Res.string.quiz_mode_place_chord_desc
        QuizMode.EAR_CHORD -> Res.string.quiz_mode_ear_chord_desc
    },
)

/** Le nom court de l'exercice, pour une ligne de l'historique. */
@Composable
fun QuizMode.shortLabel(): String = stringResource(
    when (this) {
        QuizMode.FIND_ON_STRING -> Res.string.quiz_mode_short_find_on_string
        QuizMode.FIND_ANYWHERE -> Res.string.quiz_mode_short_find_anywhere
        QuizMode.NAME_NOTE -> Res.string.quiz_mode_short_name_note
        QuizMode.NAME_CHORD -> Res.string.quiz_mode_short_name_chord
        QuizMode.PLACE_CHORD -> Res.string.quiz_mode_short_place_chord
        QuizMode.EAR_CHORD -> Res.string.quiz_mode_short_ear_chord
    },
)

@Composable
fun ChordPool.label(): String = stringResource(
    when (this) {
        ChordPool.TRIADS -> Res.string.quiz_chords_triads
        ChordPool.ALL -> Res.string.quiz_chords_all
    },
)

@Composable
fun QuizLength.label(): String = stringResource(
    when (this) {
        QuizLength.QUESTIONS_10 -> Res.string.quiz_length_10
        QuizLength.QUESTIONS_20 -> Res.string.quiz_length_20
        QuizLength.SECONDS_60 -> Res.string.quiz_length_60s
    },
)

/** Le nom complet d'une famille d'accords : « majeur », « 7 de dominante »… (les puces utilisent [ChordQuality.symbol]). */
@Composable
fun ChordQuality.displayName(): String = stringResource(
    when (this) {
        ChordQuality.MAJOR -> Res.string.chords_quality_major
        ChordQuality.MINOR -> Res.string.chords_quality_minor
        ChordQuality.DIMINISHED -> Res.string.chords_quality_diminished
        ChordQuality.AUGMENTED -> Res.string.chords_quality_augmented
        ChordQuality.DOMINANT_7 -> Res.string.chords_quality_dominant_7
        ChordQuality.MAJOR_7 -> Res.string.chords_quality_major_7
        ChordQuality.MINOR_7 -> Res.string.chords_quality_minor_7
        ChordQuality.HALF_DIMINISHED -> Res.string.chords_quality_half_diminished
        ChordQuality.DIMINISHED_7 -> Res.string.chords_quality_diminished_7
    },
)

/** Le nom d'une gamme : « majeure », « dorien », « pentatonique mineure »… */
@Composable
fun ScaleType.displayName(): String = stringResource(
    when (this) {
        ScaleType.MAJOR -> Res.string.scales_type_major
        ScaleType.DORIAN -> Res.string.scales_type_dorian
        ScaleType.PHRYGIAN -> Res.string.scales_type_phrygian
        ScaleType.LYDIAN -> Res.string.scales_type_lydian
        ScaleType.MIXOLYDIAN -> Res.string.scales_type_mixolydian
        ScaleType.NATURAL_MINOR -> Res.string.scales_type_natural_minor
        ScaleType.LOCRIAN -> Res.string.scales_type_locrian
        ScaleType.HARMONIC_MINOR -> Res.string.scales_type_harmonic_minor
        ScaleType.MELODIC_MINOR -> Res.string.scales_type_melodic_minor
        ScaleType.MAJOR_PENTATONIC -> Res.string.scales_type_major_pentatonic
        ScaleType.MINOR_PENTATONIC -> Res.string.scales_type_minor_pentatonic
        ScaleType.BLUES -> Res.string.scales_type_blues
    },
)

/** Le renversement d'un accord en toutes lettres (0 = position fondamentale), ou en abrégé pour une puce. */
@Composable
fun inversionName(inversion: Int, short: Boolean): String = stringResource(
    when (inversion) {
        0 -> if (short) Res.string.chords_inversion_0 else Res.string.chords_inversion_long_0
        1 -> if (short) Res.string.chords_inversion_1 else Res.string.chords_inversion_long_1
        2 -> if (short) Res.string.chords_inversion_2 else Res.string.chords_inversion_long_2
        else -> if (short) Res.string.chords_inversion_3 else Res.string.chords_inversion_long_3
    },
)

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.DARK -> Res.string.theme_dark
        ThemeMode.LIGHT -> Res.string.theme_light
        ThemeMode.SYSTEM -> Res.string.theme_system
    },
)

@Composable
fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> Res.string.language_system
        AppLanguage.FRENCH -> Res.string.language_french
        AppLanguage.ENGLISH -> Res.string.language_english
    },
)
