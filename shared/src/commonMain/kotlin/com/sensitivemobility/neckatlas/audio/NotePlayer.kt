package com.sensitivemobility.neckatlas.audio

import com.sensitivemobility.neckatlas.domain.Pitch
import com.sensitivemobility.neckatlas.domain.Timbre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Joue des notes de guitare ou de basse. Le son est préparé à la demande (quelques millisecondes pour une note : lecture
 * d'un échantillon, accordage) hors du fil de l'interface, puis confié à la sortie audio de la plateforme. À appeler depuis
 * le fil de l'interface.
 */
class NotePlayer internal constructor(private val output: PcmOutput, private val samples: SampleBank) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sequence: Job? = null

    /** Joue une note. Les notes déjà en train de sonner continuent. */
    fun play(pitch: Pitch, timbre: Timbre, gain: Float = 1f) {
        scope.launch { output.play(samples.voice(pitch, timbre), gain) }
    }

    /** Joue des notes l'une après l'autre (cordes à vide, arpège) ; remplace la suite précédente si elle sonnait encore. */
    fun playSequence(pitches: List<Pitch>, timbre: Timbre, intervalMillis: Long = 450, gain: Float = 0.8f) {
        sequence?.cancel()
        sequence = scope.launch {
            for (pitch in pitches) {
                output.play(samples.voice(pitch, timbre), gain)
                delay(intervalMillis)
            }
        }
    }
}
