package com.sensitivemobility.neckatlas.audio

/**
 * Le haut-parleur, côté plateforme. Reçoit des sons déjà calculés (16 bits, mono, à [SAMPLE_RATE]) et les mélange : plusieurs
 * notes peuvent sonner en même temps, ce qui servira aux accords. Peut être appelé depuis n'importe quel fil.
 */
interface PcmOutput {
    /** Joue [samples] tout de suite, par-dessus ce qui sonne déjà. [gain] : 1 = niveau normal, 0,5 = moitié moins fort. */
    fun play(samples: ShortArray, gain: Float)
}

/** Crée la sortie audio de la plateforme (AudioTrack sur Android, AVAudioEngine sur iOS). */
internal expect fun createPcmOutput(): PcmOutput
