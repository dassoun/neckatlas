package com.sensitivemobility.neckatlas.domain.tuner

import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.Pitch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/** Ce que l'accordeur a entendu : la fréquence, la corde visée et l'écart à cette corde en cents (+ : trop haut). */
class TunerReading(val frequencyHz: Double, val string: Int, val cents: Double) {
    /** Assez juste pour s'arrêter d'accorder. */
    val inTune: Boolean get() = abs(cents) <= Tuning.IN_TUNE_CENTS
}

object Tuning {
    /** Le diapason par défaut : La4 = 440 Hz. */
    const val DEFAULT_REFERENCE_HZ = 440

    /** Les diapasons que l'accordeur propose : de 430 à 450 Hz (415 et 432 existent, mais sortent de l'usage courant). */
    val REFERENCE_RANGE: IntRange = 430..450

    /** La fréquence de [pitch] quand le La4 vaut [referenceHz]. */
    fun frequencyOf(pitch: Pitch, referenceHz: Double = DEFAULT_REFERENCE_HZ.toDouble()): Double =
        referenceHz * 2.0.pow((pitch.midi - 69) / 12.0)

    /** Sous cet écart (en cents) on dit que la corde est juste : l'oreille ne l'entend plus. */
    const val IN_TUNE_CENTS = 5.0

    /** L'écart de [hz] à [referenceHz] en cents : 100 par demi-ton, 1200 par octave. */
    fun cents(hz: Double, referenceHz: Double): Double = 1200.0 * ln(hz / referenceHz) / ln(2.0)

    /** La corde à vide la plus proche de [hz], mesurée en cents (et non en Hz : l'écart de 5 Hz n'est pas le même au grave et à l'aigu). */
    fun nearestString(instrument: Instrument, hz: Double, referenceHz: Double = DEFAULT_REFERENCE_HZ.toDouble()): Int =
        instrument.openStrings.indices.minBy { abs(cents(hz, frequencyOf(instrument.openStrings[it], referenceHz))) }

    /** L'écoute de [hz] pour [instrument] : sur la corde [target] si elle est choisie, sinon sur la corde la plus proche. */
    fun reading(instrument: Instrument, hz: Double, target: Int? = null, referenceHz: Double = DEFAULT_REFERENCE_HZ.toDouble()): TunerReading {
        val string = target?.takeIf { it in instrument.openStrings.indices } ?: nearestString(instrument, hz, referenceHz)
        return TunerReading(hz, string, cents(hz, frequencyOf(instrument.openStrings[string], referenceHz)))
    }

    /** Les fréquences que l'accordeur cherche pour [instrument] : un peu sous la corde la plus grave, au-dessus de la plus aiguë. */
    fun searchRange(instrument: Instrument): ClosedFloatingPointRange<Double> =
        instrument.openStrings.first().frequencyHz * 0.75..instrument.openStrings.last().frequencyHz * 1.7
}

/**
 * Lisse les mesures : la médiane des dernières, pour que l'aiguille ne saute pas sur une mesure isolée (une harmonique, un
 * bruit de médiator). Quelques silences de suite l'effacent : la note s'est éteinte.
 */
class PitchSmoother(private val size: Int = 5, private val maxMisses: Int = 10) {
    private val recent = ArrayDeque<Double>()
    private var misses = 0

    /** Ajoute une mesure (null = rien d'audible) et rend la fréquence lissée, ou null s'il n'y a plus rien à montrer. */
    fun add(hz: Double?): Double? {
        if (hz == null) {
            if (++misses >= maxMisses) recent.clear()
        } else {
            misses = 0
            recent.addLast(hz)
            while (recent.size > size) recent.removeFirst()
        }
        if (recent.isEmpty()) return null
        return recent.sorted()[recent.size / 2]
    }

    fun reset() {
        recent.clear()
        misses = 0
    }
}
