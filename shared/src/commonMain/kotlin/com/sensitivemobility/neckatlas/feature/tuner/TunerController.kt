package com.sensitivemobility.neckatlas.feature.tuner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sensitivemobility.neckatlas.audio.AudioInput
import com.sensitivemobility.neckatlas.audio.createAudioInput
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.domain.tuner.PitchDetector
import com.sensitivemobility.neckatlas.domain.tuner.PitchSmoother
import com.sensitivemobility.neckatlas.domain.tuner.Tuning
import com.sensitivemobility.neckatlas.domain.tuner.TunerReading
import kotlin.math.log10

/**
 * Écoute le micro et tient à jour ce que l'accordeur montre. L'analyse se fait sur le fil du micro, sans toucher à l'interface :
 * [reading] est un état Compose, que l'écran observe. [start] et [stop] s'appellent depuis le fil de l'interface.
 */
class TunerController(private val input: AudioInput = createAudioInput()) {
    /** Ce qui s'entend en ce moment, ou null (silence, son sans hauteur nette). */
    var reading: TunerReading? by mutableStateOf(null)
        private set

    /** Le niveau du micro, de 0 à 1 sur une échelle en décibels (-80 dB à -20 dB) : de quoi voir si le téléphone entend. */
    var level: Float by mutableStateOf(0f)
        private set

    /** Vrai tant que le micro écoute. */
    var listening: Boolean by mutableStateOf(false)
        private set

    /** Vrai si le micro n'a pas pu démarrer (déjà pris par une autre appli, par exemple). */
    var failed: Boolean by mutableStateOf(false)
        private set

    private val lock = Any()
    private val smoother = PitchSmoother()

    @Volatile
    private var instrument: Instrument = Instrument.GUITAR_6

    @Volatile
    private var target: Int? = null

    @Volatile
    private var referenceHz: Double = Tuning.DEFAULT_REFERENCE_HZ.toDouble()

    /** Commence à écouter pour [instrument] ; [target] : la corde visée (indice, 0 = la plus grave), ou null pour la plus proche ; [referenceHz] : le La4. */
    fun start(instrument: Instrument, target: Int?, referenceHz: Int) {
        configure(instrument, target, referenceHz)
        if (listening) return
        failed = false
        listening = input.start(::onWindow)
        failed = !listening
    }

    /** Change d'instrument ou de corde visée sans couper le micro. */
    fun configure(instrument: Instrument, target: Int?, referenceHz: Int) {
        this.instrument = instrument
        this.target = target
        this.referenceHz = referenceHz.toDouble()
        synchronized(lock) { smoother.reset() }
        reading = null
    }

    fun stop() {
        input.stop()
        listening = false
        synchronized(lock) { smoother.reset() }
        reading = null
        level = 0f
    }

    private fun onWindow(samples: FloatArray, sampleRate: Int) {
        if (!listening) return
        val db = 20f * log10(PitchDetector.rms(samples, samples.size - AudioInput.HOP_FRAMES).coerceAtLeast(1e-5f))
        level = ((db + 80f) / 60f).coerceIn(0f, 1f)
        val instrument = instrument
        val range = Tuning.searchRange(instrument)
        val hz = PitchDetector.detect(samples, sampleRate, range.start, range.endInclusive)
        val smooth = synchronized(lock) { smoother.add(hz) }
        reading = smooth?.let { Tuning.reading(instrument, it, target, referenceHz) }
    }
}
