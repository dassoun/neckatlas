package com.sensitivemobility.neckatlas.audio

import androidx.compose.runtime.Composable

/**
 * Le micro, côté plateforme. Une fois démarré, rend des fenêtres de son (mono, flottants entre -1 et 1) toutes les
 * [HOP_FRAMES] nouveaux échantillons, chacune longue de [WINDOW_FRAMES] : de quoi mesurer une hauteur, même celle d'une basse.
 */
interface AudioInput {
    /**
     * Commence l'écoute ; [onWindow] est appelé depuis un fil de la plateforme (pas celui de l'interface) avec une fenêtre
     * neuve et la fréquence d'échantillonnage. Rend faux si le micro n'est pas disponible (permission refusée, déjà pris…).
     */
    fun start(onWindow: (samples: FloatArray, sampleRate: Int) -> Unit): Boolean

    /** Arrête l'écoute et rend le micro. Sans effet s'il n'écoutait pas. */
    fun stop()

    companion object {
        /** Taille d'une fenêtre : 8192 échantillons, soit ~186 ms à 44,1 kHz — deux périodes du Si grave d'une basse 5 cordes. */
        const val WINDOW_FRAMES = 8192

        /** Pas entre deux fenêtres : une mesure toutes les ~46 ms. */
        const val HOP_FRAMES = 2048
    }
}

/** Crée l'entrée audio de la plateforme (AudioRecord sur Android, AVAudioEngine sur iOS). */
internal expect fun createAudioInput(): AudioInput

/** L'autorisation d'utiliser le micro : [granted] est vrai une fois accordée ; [denied] après un refus ; [request] la demande. */
class MicrophonePermission(val granted: Boolean, val denied: Boolean, val request: () -> Unit)

/** L'état de l'autorisation du micro, avec de quoi la demander (une fenêtre du système sur Android, iOS). */
@Composable
expect fun rememberMicrophonePermission(): MicrophonePermission
