package com.sensitivemobility.neckatlas.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * La teinte d'une réussite : vert (90 % et plus), turquoise (70 à 89), ambre (50 à 69), rouge (moins de 50) ; `null` si la
 * note ou la case n'a jamais été demandée. Commune à la carte de chaleur, aux cases de statistiques et aux anneaux de score.
 */
fun accuracyColor(colors: ColorScheme, accents: AccentColors, accuracy: Double?): Color? = when {
    accuracy == null -> null
    accuracy >= 0.9 -> CorrectColor
    accuracy >= 0.7 -> accents.teal
    accuracy >= 0.5 -> colors.tertiary
    else -> colors.error
}

/**
 * Un anneau qui se remplit de [fraction] (0 à 1) quand il apparaît, avec [content] en son centre (le score, le pourcentage).
 * Le tour est gris, l'arc de la couleur [color].
 */
@Composable
fun ScoreRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) { progress.animateTo(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            // Un arc de longueur nulle avec un bout rond dessinerait quand même un point : on ne dessine rien à zéro.
            if (progress.value > 0.002f) {
                drawArc(
                    color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
