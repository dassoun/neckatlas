package com.sensitivemobility.neckatlas.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sensitivemobility.neckatlas.data.prefs.AppLanguage
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.data.prefs.ThemeMode
import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.NoteNotation
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.settings_accidentals
import com.sensitivemobility.neckatlas.shared.resources.settings_left_handed
import com.sensitivemobility.neckatlas.shared.resources.settings_haptics
import com.sensitivemobility.neckatlas.shared.resources.settings_haptics_desc
import com.sensitivemobility.neckatlas.shared.resources.settings_language
import com.sensitivemobility.neckatlas.shared.resources.settings_left_handed_desc
import com.sensitivemobility.neckatlas.shared.resources.settings_notation
import com.sensitivemobility.neckatlas.shared.resources.settings_sound
import com.sensitivemobility.neckatlas.shared.resources.settings_sound_desc
import com.sensitivemobility.neckatlas.shared.resources.settings_theme
import com.sensitivemobility.neckatlas.ui.Section
import com.sensitivemobility.neckatlas.ui.label
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Section(stringResource(Res.string.settings_sound)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.settings_sound_desc),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = settings.soundEnabled, onCheckedChange = { value -> onChange { it.copy(soundEnabled = value) } })
            }
        }

        Section(stringResource(Res.string.settings_haptics)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.settings_haptics_desc),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = settings.haptics, onCheckedChange = { value -> onChange { it.copy(haptics = value) } })
            }
        }

        Section(stringResource(Res.string.settings_notation)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteNotation.entries.forEach { option ->
                    FilterChip(
                        selected = option == settings.notation,
                        onClick = { onChange { it.copy(notation = option) } },
                        label = { Text(option.label()) },
                    )
                }
            }
        }

        Section(stringResource(Res.string.settings_accidentals)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccidentalStyle.entries.forEach { option ->
                    FilterChip(
                        selected = option == settings.accidentals,
                        onClick = { onChange { it.copy(accidentals = option) } },
                        label = { Text(option.label()) },
                    )
                }
            }
        }

        Section(stringResource(Res.string.settings_left_handed)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.settings_left_handed_desc),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = settings.leftHanded, onCheckedChange = { value -> onChange { it.copy(leftHanded = value) } })
            }
        }

        Section(stringResource(Res.string.settings_theme)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == settings.theme,
                        onClick = { onChange { it.copy(theme = option) } },
                        label = { Text(option.label()) },
                    )
                }
            }
        }

        Section(stringResource(Res.string.settings_language)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = option == settings.language,
                        onClick = { onChange { it.copy(language = option) } },
                        label = { Text(option.label()) },
                    )
                }
            }
        }
    }
}
