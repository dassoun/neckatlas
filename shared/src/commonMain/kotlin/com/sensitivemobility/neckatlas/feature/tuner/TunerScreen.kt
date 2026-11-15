package com.sensitivemobility.neckatlas.feature.tuner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensitivemobility.neckatlas.audio.NotePlayer
import com.sensitivemobility.neckatlas.audio.rememberMicrophonePermission
import com.sensitivemobility.neckatlas.data.prefs.AppSettings
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.tuner.TunerReading
import com.sensitivemobility.neckatlas.domain.tuner.Tuning
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.quiz_string_chip
import com.sensitivemobility.neckatlas.shared.resources.tuner_auto
import com.sensitivemobility.neckatlas.shared.resources.tuner_back
import com.sensitivemobility.neckatlas.shared.resources.tuner_cents
import com.sensitivemobility.neckatlas.shared.resources.tuner_hint
import com.sensitivemobility.neckatlas.shared.resources.tuner_hint_target
import com.sensitivemobility.neckatlas.shared.resources.tuner_hz
import com.sensitivemobility.neckatlas.shared.resources.tuner_in_tune
import com.sensitivemobility.neckatlas.shared.resources.tuner_level
import com.sensitivemobility.neckatlas.shared.resources.tuner_mic_unavailable
import com.sensitivemobility.neckatlas.shared.resources.tuner_permission_button
import com.sensitivemobility.neckatlas.shared.resources.tuner_permission_denied
import com.sensitivemobility.neckatlas.shared.resources.tuner_permission_text
import com.sensitivemobility.neckatlas.shared.resources.tuner_target
import com.sensitivemobility.neckatlas.shared.resources.tuner_reference
import com.sensitivemobility.neckatlas.shared.resources.tuner_reference_less
import com.sensitivemobility.neckatlas.shared.resources.tuner_reference_more
import com.sensitivemobility.neckatlas.shared.resources.tuner_title
import com.sensitivemobility.neckatlas.shared.resources.tuner_too_high
import com.sensitivemobility.neckatlas.shared.resources.tuner_too_low
import com.sensitivemobility.neckatlas.ui.CorrectColor
import com.sensitivemobility.neckatlas.ui.label
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

private const val AUTO = -1

/** La plage de la jauge : de -50 à +50 cents, soit un quart de ton de chaque côté. */
private const val GAUGE_CENTS = 50f

/**
 * L'accordeur : l'appli écoute le micro et dit, pour la corde jouée, de combien elle est trop haute ou trop basse. Il écoute
 * tant que l'écran est affiché. Toucher une corde la vise (et la fait entendre, pour accorder à l'oreille) ; « Auto » reprend la
 * corde la plus proche de ce qui s'entend.
 *
 * @param onClose revient au manche.
 */
@Composable
fun TunerScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, notes: NotePlayer, onClose: () -> Unit) {
    val instrument = settings.instrument
    val controller = remember { TunerController() }
    val permission = rememberMicrophonePermission()
    // Le choix de la corde est lié à l'instrument : une corde d'une autre guitare ne désignerait plus la même note.
    var target by rememberSaveable(instrument) { mutableIntStateOf(AUTO) }
    val targetString = target.takeIf { it != AUTO }

    // Le micro n'écoute que pendant que l'écran est là et que la permission est accordée.
    DisposableEffect(controller) { onDispose { controller.stop() } }
    LaunchedEffect(permission.granted, instrument, targetString, settings.tunerReferenceHz) {
        if (permission.granted) controller.start(instrument, targetString, settings.tunerReferenceHz) else controller.stop()
    }

    val reading = controller.reading
    Column(Modifier.fillMaxSize().padding(top = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.tuner_back))
            }
            Text(stringResource(Res.string.tuner_title, instrument.label()), style = MaterialTheme.typography.titleMedium)
        }

        if (!permission.granted) {
            PermissionCard(denied = permission.denied, onRequest = permission.request)
        } else {
            val shown = reading?.string ?: targetString
            NoteReadout(settings, reading, shown, targetString)
            Gauge(reading, Modifier.padding(horizontal = 16.dp))
            Status(reading, controller.failed)
            LevelBar(controller.level, Modifier.padding(horizontal = 16.dp))
        }

        StringChips(settings, shownString = reading?.string, target = target) { chosen ->
            target = chosen
            if (chosen != AUTO && settings.soundEnabled) notes.play(instrument.openStrings[chosen], instrument.timbre)
        }
        ReferenceRow(settings.tunerReferenceHz) { hz -> onChange { it.copy(tunerReferenceHz = hz) } }
    }
}

@Composable
private fun PermissionCard(denied: Boolean, onRequest: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.tuner_permission_text), style = MaterialTheme.typography.bodyMedium)
        if (denied) {
            Text(stringResource(Res.string.tuner_permission_denied), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onRequest, Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Text(stringResource(Res.string.tuner_permission_button), Modifier.padding(start = 8.dp))
        }
    }
}

/** La note visée en grand, avec la fréquence entendue dessous. */
@Composable
private fun NoteReadout(settings: AppSettings, reading: TunerReading?, shownString: Int?, targetString: Int?) {
    val instrument = settings.instrument
    val colors = MaterialTheme.colorScheme
    val inTune = reading?.inTune == true
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (shownString == null) {
            Text("–", fontSize = 84.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
            Text(stringResource(Res.string.tuner_hint), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        } else {
            val open = instrument.openStrings[shownString]
            Text(
                NoteNames.label(open.pitchClass, settings.notation, settings.accidentals),
                fontSize = 84.sp,
                fontWeight = FontWeight.Bold,
                color = if (inTune) CorrectColor else colors.onSurface,
            )
            Text(
                stringResource(
                    Res.string.tuner_target,
                    instrument.stringNumber(shownString),
                    NoteNames.label(open, settings.notation, settings.accidentals),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
            if (reading == null && targetString != null) {
                Text(
                    stringResource(Res.string.tuner_hint_target, instrument.stringNumber(targetString)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (reading != null) {
            val tenths = (reading.frequencyHz * 10).roundToInt()
            Text(
                stringResource(Res.string.tuner_hz, tenths / 10, tenths % 10),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** La jauge : une règle de -50 à +50 cents, la zone « juste » au milieu, et une aiguille qui glisse vers l'écart mesuré. */
@Composable
private fun Gauge(reading: TunerReading?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val clamped = (reading?.cents?.toFloat() ?: 0f).coerceIn(-GAUGE_CENTS, GAUGE_CENTS)
    val cents by animateFloatAsState(clamped, tween(140), label = "needle")
    val alpha by animateFloatAsState(if (reading == null) 0.25f else 1f, tween(200), label = "needleAlpha")
    val inTune = reading?.inTune == true
    val needleColor = if (inTune) CorrectColor else colors.tertiary
    val zone = CorrectColor.copy(alpha = 0.28f)
    val tick = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val track = colors.surfaceContainerHigh

    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        val dp = density
        val centerX = size.width / 2f
        val half = size.width / 2f - 12 * dp
        val top = 10 * dp
        val bottom = size.height - 10 * dp
        drawRoundRect(track, Offset(0f, top), Size(size.width, bottom - top), CornerRadius(14 * dp))
        // La zone juste : ± IN_TUNE_CENTS autour du centre.
        val zoneHalf = half * (Tuning.IN_TUNE_CENTS.toFloat() / GAUGE_CENTS)
        drawRoundRect(zone, Offset(centerX - zoneHalf, top), Size(2 * zoneHalf, bottom - top), CornerRadius(8 * dp))
        // Graduations tous les 10 cents, plus longue au centre et à ±50.
        for (step in -5..5) {
            val x = centerX + half * step / 5f
            val long = step == 0 || abs(step) == 5
            val inset = if (long) 14 * dp else 26 * dp
            drawLine(tick, Offset(x, top + inset), Offset(x, bottom - inset), strokeWidth = if (step == 0) 2.5f * dp else 1.5f * dp, cap = StrokeCap.Round)
        }
        val x = centerX + half * cents / GAUGE_CENTS
        drawLine(needleColor.copy(alpha = alpha), Offset(x, top + 4 * dp), Offset(x, bottom - 4 * dp), strokeWidth = 5 * dp, cap = StrokeCap.Round)
    }
}

/** Une ligne qui dit quoi faire : « Juste », ou trop bas / trop haut, avec l'écart en cents. */
@Composable
private fun Status(reading: TunerReading?, failed: Boolean) {
    val colors = MaterialTheme.colorScheme
    val (text, color) = when {
        failed -> stringResource(Res.string.tuner_mic_unavailable) to colors.error
        reading == null -> "" to colors.onSurfaceVariant
        reading.inTune -> stringResource(Res.string.tuner_in_tune) to CorrectColor
        reading.cents < 0 -> stringResource(Res.string.tuner_too_low) to colors.tertiary
        else -> stringResource(Res.string.tuner_too_high) to colors.tertiary
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = color, textAlign = TextAlign.Center)
        if (reading != null) {
            val rounded = reading.cents.roundToInt()
            Text(
                stringResource(Res.string.tuner_cents, if (rounded >= 0) "+" else "−", abs(rounded)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** « Auto » puis une pastille par corde (numéro et note) : la corde entendue est cochée en auto, la corde visée sinon. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StringChips(settings: AppSettings, shownString: Int?, target: Int, onChoose: (Int) -> Unit) {
    val instrument = settings.instrument
    FlowRow(
        Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = target == AUTO, onClick = { onChoose(AUTO) }, label = { Text(stringResource(Res.string.tuner_auto)) })
        // Les cordes de la plus aiguë (1) à la plus grave, comme on les numérote.
        for (string in instrument.openStrings.indices.reversed()) {
            val note = NoteNames.label(instrument.openStrings[string].pitchClass, settings.notation, settings.accidentals)
            FilterChip(
                selected = if (target == AUTO) shownString == string else target == string,
                onClick = { onChoose(string) },
                label = { Text(stringResource(Res.string.quiz_string_chip, instrument.stringNumber(string), note)) },
            )
        }
    }
}

/** Le diapason : « La = 440 Hz », avec un pas de 1 Hz de chaque côté. Un accordeur ou un instrument réglé autrement s'y accorde. */
@Composable
private fun ReferenceRow(referenceHz: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = { onChange((referenceHz - 1).coerceIn(Tuning.REFERENCE_RANGE)) },
            enabled = referenceHz > Tuning.REFERENCE_RANGE.first,
        ) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.tuner_reference_less))
        }
        Text(
            stringResource(Res.string.tuner_reference, referenceHz),
            Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(
            onClick = { onChange((referenceHz + 1).coerceIn(Tuning.REFERENCE_RANGE)) },
            enabled = referenceHz < Tuning.REFERENCE_RANGE.last,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.tuner_reference_more))
        }
    }
}

/** Une fine barre de niveau : elle bouge quand le micro entend quelque chose, même trop faible pour que la note soit reconnue. */
@Composable
private fun LevelBar(level: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(level, tween(120), label = "level")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { animated }, Modifier.fillMaxWidth().height(4.dp))
        Text(stringResource(Res.string.tuner_level), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
