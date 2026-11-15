package com.sensitivemobility.neckatlas.feature.fretboard

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Un petit manche dessiné pour les écrans vides : même bois, mêmes frettes, même nacre et mêmes cordes que le vrai manche, avec
 * une case à trouver (le rond ambré) et une orbite qui rappelle l'icône. À poser dans un cadre d'environ 3 pour 4.
 */
@Composable
internal fun NeckIllustration(modifier: Modifier = Modifier) {
    val orbit = MaterialTheme.colorScheme.primary
    val target = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val dpf = density
        val neckWidth = size.width * 0.42f
        val neckHeight = size.height * 0.84f
        val left = (size.width - neckWidth) / 2f
        val top = (size.height - neckHeight) / 2f
        val right = left + neckWidth
        val corner = CornerRadius(7 * dpf)

        // Le bois, comme sur le vrai manche.
        drawRoundRect(
            Brush.horizontalGradient(0f to WoodEdge, 0.5f to WoodCenter, 1f to WoodEdge, startX = left, endX = right),
            Offset(left, top),
            Size(neckWidth, neckHeight),
            corner,
        )
        drawRoundRect(Color.Black.copy(alpha = 0.55f), Offset(left, top), Size(neckWidth, neckHeight), corner, style = Stroke(1.2f * dpf))

        // Cinq cases, donc quatre frettes, et un repère nacré au milieu de la troisième.
        for (fret in 1..4) drawFretWire(top + neckHeight * fret / 5f, left, right, dpf)
        drawPearl(Offset(size.width / 2f, top + neckHeight * 2.5f / 5f), 5 * dpf, dpf)

        // Quatre cordes : deux filées, deux plaines, de plus en plus fines.
        for (string in 0..3) {
            val x = left + neckWidth * (string + 0.5f) / 4f
            val thickness = (3.0f - string * 0.65f) * dpf
            val core = if (string < 2) WoundStringCore else PlainStringCore
            drawLine(Color.Black.copy(alpha = 0.38f), Offset(x + 1.4f * dpf, top), Offset(x + 1.4f * dpf, top + neckHeight), strokeWidth = thickness + 0.7f * dpf)
            drawLine(core, Offset(x, top), Offset(x, top + neckHeight), strokeWidth = thickness)
        }

        // La case à trouver : un rond ambré cerclé, comme dans le quiz.
        val markerCenter = Offset(left + neckWidth * 1.5f / 4f, top + neckHeight * 3.5f / 5f)
        val markerRadius = neckHeight / 5f * 0.34f
        drawCircle(target.copy(alpha = 0.3f), markerRadius + 5 * dpf, markerCenter)
        drawCircle(target, markerRadius, markerCenter)
        drawCircle(target, markerRadius + 3 * dpf, markerCenter, style = Stroke(2f * dpf))

        // L'orbite de l'icône : un arc épais, incliné, qui passe autour du manche.
        rotate(degrees = -24f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawArc(
                color = orbit,
                startAngle = 196f,
                sweepAngle = 262f,
                useCenter = false,
                topLeft = Offset(size.width * 0.03f, size.height * 0.3f),
                size = Size(size.width * 0.94f, size.height * 0.4f),
                style = Stroke(6f * dpf, cap = StrokeCap.Round),
            )
        }
    }
}
