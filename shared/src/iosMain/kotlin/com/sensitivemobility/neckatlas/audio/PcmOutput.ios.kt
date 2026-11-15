package com.sensitivemobility.neckatlas.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive

internal actual fun createPcmOutput(): PcmOutput = AvEnginePcmOutput()

/**
 * Sortie audio iOS : un moteur AVAudioEngine et une réserve de lecteurs, chaque note prenant le suivant à tour de rôle (la
 * plus ancienne est coupée quand la réserve est épuisée).
 *
 * **Jamais compilé** : écrit sans Mac, d'après la documentation d'Apple et de Kotlin/Native. À vérifier au premier build.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AvEnginePcmOutput : PcmOutput {
    private val engine = AVAudioEngine()
    private val format = AVAudioFormat(standardFormatWithSampleRate = SAMPLE_RATE.toDouble(), channels = 1u)
    private val players = List(MAX_VOICES) { AVAudioPlayerNode() }
    private var next = 0
    private var started = false

    init {
        for (player in players) {
            engine.attachNode(player)
            engine.connect(player, to = engine.mainMixerNode, format = format)
        }
    }

    override fun play(samples: ShortArray, gain: Float) {
        if (!ensureStarted()) return
        val frames = samples.size
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frames.toUInt()) ?: return
        buffer.frameLength = frames.toUInt()
        val channel = buffer.floatChannelData?.get(0) ?: return
        for (i in 0 until frames) channel[i] = samples[i] / 32768f * gain

        val player = players[next]
        next = (next + 1) % players.size
        player.stop()
        player.scheduleBuffer(buffer, completionHandler = null)
        player.play()
    }

    private fun ensureStarted(): Boolean {
        if (started) return true
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
        started = engine.startAndReturnError(null)
        return started
    }

    private companion object {
        const val MAX_VOICES = 8
    }
}
