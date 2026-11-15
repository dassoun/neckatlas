package com.sensitivemobility.neckatlas.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.setActive

internal actual fun createAudioInput(): AudioInput = AvEngineInput()

/**
 * Le micro d'iOS : un tap sur l'entrée d'un AVAudioEngine, dont les blocs alimentent la même fenêtre glissante qu'Android.
 *
 * **Jamais compilé** : écrit sans Mac, d'après la documentation d'Apple et de Kotlin/Native. À vérifier au premier build.
 * L'appli iOS devra aussi déclarer `NSMicrophoneUsageDescription` dans son Info.plist.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AvEngineInput : AudioInput {
    private val engine = AVAudioEngine()
    private var running = false

    override fun start(onWindow: (samples: FloatArray, sampleRate: Int) -> Unit): Boolean {
        if (running) return true
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        session.setActive(true, error = null)

        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        val rate = format.sampleRate.toInt()
        if (rate <= 0) return false

        val window = FloatArray(AudioInput.WINDOW_FRAMES)
        var filled = 0
        var sinceLast = 0
        input.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            val channel = buffer?.floatChannelData?.get(0) ?: return@installTapOnBus
            val read = buffer.frameLength.toInt()
            if (read <= 0) return@installTapOnBus
            if (read >= window.size) {
                for (i in window.indices) window[i] = channel[read - window.size + i]
            } else {
                window.copyInto(window, 0, read, window.size)
                for (i in 0 until read) window[window.size - read + i] = channel[i]
            }
            filled = minOf(window.size, filled + read)
            sinceLast += read
            if (filled == window.size && sinceLast >= AudioInput.HOP_FRAMES) {
                sinceLast = 0
                onWindow(window.copyOf(), rate)
            }
        }
        running = engine.startAndReturnError(null)
        if (!running) input.removeTapOnBus(0u)
        return running
    }

    override fun stop() {
        if (!running) return
        engine.inputNode.removeTapOnBus(0u)
        engine.stop()
        running = false
    }
}

@Composable
actual fun rememberMicrophonePermission(): MicrophonePermission {
    val session = AVAudioSession.sharedInstance()
    var granted by remember { mutableStateOf(session.recordPermission == AVAudioSessionRecordPermissionGranted) }
    var denied by remember { mutableStateOf(session.recordPermission == AVAudioSessionRecordPermissionDenied) }
    return MicrophonePermission(granted, denied) {
        session.requestRecordPermission { result ->
            granted = result
            denied = !result
        }
    }
}
