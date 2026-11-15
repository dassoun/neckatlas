package com.sensitivemobility.neckatlas.domain.tuner

import kotlin.math.sqrt

/**
 * Trouve la hauteur d'un son : l'algorithme YIN (de Cheveigné et Kawahara, 2002), une autocorrélation « corrigée » qui évite
 * de prendre l'octave au-dessus ou au-dessous. Une corde grave d'une basse est riche en harmoniques et pauvre en fondamentale :
 * c'est le cas que YIN gère le mieux. Du code pur, sans plateforme : on le teste avec des sons calculés.
 */
object PitchDetector {
    /**
     * Sous ce niveau (valeur efficace, 1 = pleine échelle), on considère que c'est du silence. Très bas exprès : une guitare
     * électrique non amplifiée, captée par le micro d'un téléphone sans traitement, ne dépasse guère 0,001. La mesure ne dépend pas
     * du niveau ; c'est la netteté du son, pas son volume, qui écarte le bruit.
     */
    const val MIN_RMS = 0.0003f

    /** La valeur efficace de [samples] (0 = silence, 1 = pleine échelle). */
    fun rms(samples: FloatArray, from: Int = 0): Float {
        var energy = 0.0
        for (i in from until samples.size) energy += samples[i] * samples[i]
        return sqrt(energy / (samples.size - from).coerceAtLeast(1)).toFloat()
    }

    /** Seuil de YIN : plus il est bas, plus on exige un son net. 0,15 est la valeur de l'article. */
    private const val THRESHOLD = 0.15f

    /** Au-delà de cette valeur le minimum trouvé n'est pas assez net pour être une hauteur (du bruit, une voix parlée…). */
    private const val CLARITY_LIMIT = 0.45f

    /**
     * La fréquence (Hz) du son dans [samples] (mono, échantillonné à [sampleRate]), entre [minHz] et [maxHz], ou null si c'est
     * du silence ou un son sans hauteur nette. Il faut au moins deux fois la plus longue période cherchée : pour 30 Hz à
     * 44,1 kHz, environ 3000 échantillons ; plus la fenêtre est longue, plus c'est précis.
     */
    fun detect(samples: FloatArray, sampleRate: Int, minHz: Double, maxHz: Double): Double? {
        require(minHz > 0 && maxHz > minHz) { "Bornes de fréquence invalides : $minHz..$maxHz" }
        val maxLag = (sampleRate / minHz).toInt()
        val minLag = maxOf(2, (sampleRate / maxHz).toInt())
        val window = samples.size - maxLag - 1
        if (window < maxLag || minLag >= maxLag) return null

        if (rms(samples) < MIN_RMS) return null

        // d(τ) : la différence entre le son et lui-même décalé de τ ; elle s'annule quand τ vaut la période.
        val difference = FloatArray(maxLag + 1)
        for (lag in 1..maxLag) {
            var sum = 0f
            for (j in 0 until window) {
                val delta = samples[j] - samples[j + lag]
                sum += delta * delta
            }
            difference[lag] = sum
        }

        // d'(τ) : la même, divisée par sa moyenne cumulée, pour que la valeur trompeuse en τ = 0 ne gagne pas.
        val normalized = FloatArray(maxLag + 1)
        normalized[0] = 1f
        var running = 0f
        for (lag in 1..maxLag) {
            running += difference[lag]
            normalized[lag] = if (running == 0f) 1f else difference[lag] * lag / running
        }

        // Le premier creux sous le seuil, suivi jusqu'à son fond ; à défaut, le creux le plus net s'il est assez net.
        var best = -1
        var lag = minLag
        while (lag <= maxLag) {
            if (normalized[lag] < THRESHOLD) {
                while (lag + 1 <= maxLag && normalized[lag + 1] < normalized[lag]) lag++
                best = lag
                break
            }
            lag++
        }
        if (best < 0) {
            var lowest = minLag
            for (candidate in minLag..maxLag) if (normalized[candidate] < normalized[lowest]) lowest = candidate
            if (normalized[lowest] > CLARITY_LIMIT) return null
            best = lowest
        }

        return sampleRate / refine(normalized, best, minLag, maxLag)
    }

    /** Affine la période entière [lag] par une parabole passant par les trois points voisins : la précision passe sous le cent. */
    private fun refine(values: FloatArray, lag: Int, minLag: Int, maxLag: Int): Double {
        if (lag <= minLag || lag >= maxLag) return lag.toDouble()
        val before = values[lag - 1].toDouble()
        val here = values[lag].toDouble()
        val after = values[lag + 1].toDouble()
        val denominator = before - 2 * here + after
        if (denominator == 0.0) return lag.toDouble()
        return lag + 0.5 * (before - after) / denominator
    }
}
