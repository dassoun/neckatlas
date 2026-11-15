package com.sensitivemobility.neckatlas.feature.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.instrument_title
import com.sensitivemobility.neckatlas.shared.resources.setup_summary
import com.sensitivemobility.neckatlas.shared.resources.setup_tuner
import com.sensitivemobility.neckatlas.shared.resources.zone_preset
import com.sensitivemobility.neckatlas.shared.resources.zone_range
import com.sensitivemobility.neckatlas.shared.resources.zone_range_open
import com.sensitivemobility.neckatlas.shared.resources.zone_range_single
import com.sensitivemobility.neckatlas.shared.resources.zone_title
import com.sensitivemobility.neckatlas.ui.Section
import com.sensitivemobility.neckatlas.ui.label
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val ZonePresets = listOf(12, 22, 24)

// Ce fichier regroupe le choix de l'instrument et de la zone de travail, commun au manche et au quiz.

@Composable
fun zoneText(zone: FretZone): String = when {
    zone.first == 0 -> stringResource(Res.string.zone_range_open, zone.last)
    zone.first == zone.last -> stringResource(Res.string.zone_range_single, zone.first)
    else -> stringResource(Res.string.zone_range, zone.first, zone.last)
}

/** Une ligne qui dit sur quoi on travaille, et qui ouvre les réglages de l'instrument et de la zone. */
@Composable
fun SetupSummary(instrument: Instrument, zone: FretZone, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.setup_summary, instrument.label(), zoneText(zone)),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * La feuille des réglages de l'instrument et de la zone, qui monte du bas de l'écran quand [open] est vrai. Avec [onTuner], un
 * bouton « Accordeur » sous le choix de l'instrument ferme la feuille et appelle [onTuner].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSheetHost(
    open: Boolean,
    onDismiss: () -> Unit,
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onTuner: (() -> Unit)? = null,
) {
    if (!open) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SetupSheet(settings, onChange, onTuner = onTuner?.let { openTuner -> { onDismiss(); openTuner() } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupSheet(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onTuner: (() -> Unit)?) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Section(stringResource(Res.string.instrument_title)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Instrument.entries.forEach { option ->
                    FilterChip(
                        selected = option == settings.instrument,
                        onClick = { onChange { it.copy(instrument = option) } },
                        label = { Text(option.label()) },
                    )
                }
            }
            if (onTuner != null) {
                OutlinedButton(onClick = onTuner) {
                    Icon(Icons.Filled.Mic, contentDescription = null, Modifier.size(18.dp))
                    Text(stringResource(Res.string.setup_tuner), Modifier.padding(start = 8.dp))
                }
            }
        }
        Section(stringResource(Res.string.zone_title)) {
            ZoneSelector(settings.zone) { newZone -> onChange { it.copy(zone = newZone) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneSelector(zone: FretZone, onZoneChange: (FretZone) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ZonePresets.forEach { count ->
            FilterChip(
                selected = zone == FretZone.firstFrets(count),
                onClick = { onZoneChange(FretZone.firstFrets(count)) },
                label = { Text(stringResource(Res.string.zone_preset, count)) },
            )
        }
    }

    // Pendant le glissement on garde la valeur du curseur ; elle n'est enregistrée qu'au relâchement. Elle est oubliée
    // dès que la zone enregistrée change (par exemple après un choix prédéfini).
    var dragging by remember(zone) { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    RangeSlider(
        value = dragging ?: (zone.first.toFloat()..zone.last.toFloat()),
        onValueChange = { dragging = it },
        valueRange = 0f..FretZone.MAX_FRET.toFloat(),
        steps = FretZone.MAX_FRET - 1,
        onValueChangeFinished = { dragging?.let { onZoneChange(it.toZone()) } },
    )
    Text(zoneText(dragging?.toZone() ?: zone), style = MaterialTheme.typography.bodyMedium)
}

/** Arrondit les deux bouts du curseur à des cases entières ; la zone compte au moins la case 1. */
private fun ClosedFloatingPointRange<Float>.toZone(): FretZone {
    val first = start.roundToInt().coerceIn(0, FretZone.MAX_FRET)
    val last = endInclusive.roundToInt().coerceIn(maxOf(first, 1), FretZone.MAX_FRET)
    return FretZone(first, last)
}
