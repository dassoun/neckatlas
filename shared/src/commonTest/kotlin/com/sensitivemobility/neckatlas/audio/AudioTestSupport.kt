package com.sensitivemobility.neckatlas.audio

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sin

/**
 * Estime la fréquence d'un son rendu à [SAMPLE_RATE] par autocorrélation, avec interpolation parabolique pour descendre sous
 * l'échantillon. La mesure commence à [startSeconds] (après l'attaque) et ne regarde que les environs de [expectedHz].
 *
 * La fenêtre fait au moins [minWindow] échantillons et quatre périodes. Elle fausse un peu la mesure quand elle ne contient
 * pas un nombre entier de périodes, d'autant plus qu'elle en contient peu : pour une sinusoïde pure et grave, prendre
 * une fenêtre bien plus large (16 384) ramène l'erreur sous le cent.
 */
internal fun estimateFrequency(
    samples: ShortArray,
    expectedHz: Double,
    startSeconds: Double = 0.15,
    minWindow: Int = 4096,
): Double {
    val expectedPeriod = SAMPLE_RATE / expectedHz
    val start = (startSeconds * SAMPLE_RATE).toInt()
    val window = maxOf(minWindow, (expectedPeriod * 4).toInt())
    val x = FloatArray(window) { samples[start + it] / 32768f }
    val minLag = (expectedPeriod * 0.6).toInt()
    val maxLag = (expectedPeriod * 1.4).toInt() + 1

    fun correlation(lag: Int): Double {
        var sum = 0.0
        for (i in 0 until window - lag) sum += x[i] * x[i + lag]
        return sum / (window - lag)
    }

    var bestLag = minLag
    var best = Double.NEGATIVE_INFINITY
    for (lag in minLag..maxLag) {
        val r = correlation(lag)
        if (r > best) { best = r; bestLag = lag }
    }
    val before = correlation(bestLag - 1)
    val after = correlation(bestLag + 1)
    val shift = 0.5 * (before - after) / (before - 2 * best + after)
    return SAMPLE_RATE / (bestLag + shift)
}

/** Écart entre deux fréquences, en cents (centièmes de demi-ton) : positif si [measured] est plus aigu. */
internal fun cents(measured: Double, expected: Double): Double = 1200.0 * ln(measured / expected) / ln(2.0)

/** Une sinusoïde pure de [hz] à [rate] Hz pendant [seconds], d'amplitude [amplitude] (1 = pleine échelle). */
internal fun sine(hz: Double, rate: Int, seconds: Double, amplitude: Double = 0.5): ShortArray =
    ShortArray((seconds * rate).toInt()) { (amplitude * 32767 * sin(2 * PI * hz * it / rate)).toInt().toShort() }

/**
 * Exécute une fonction suspendante qui ne se suspend jamais (verrou libre, lecture de fichier immédiate) et rend son
 * résultat. Évite d'ajouter une bibliothèque de test des coroutines pour de la logique qui n'attend rien.
 */
internal fun <T> runNow(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
    return checkNotNull(outcome) { "Le bloc s'est suspendu : runNow ne convient qu'au code qui n'attend rien" }.getOrThrow()
}
