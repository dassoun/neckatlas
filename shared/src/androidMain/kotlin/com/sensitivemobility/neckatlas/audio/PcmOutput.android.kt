package com.sensitivemobility.neckatlas.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log

internal actual fun createPcmOutput(): PcmOutput = AudioTrackPcmOutput()

/**
 * Mélange les notes dans un seul AudioTrack en flux, alimenté par un fil dédié. Le fil naît à la première note et s'arrête
 * une demi-seconde après la dernière : rien ne tourne quand l'appli est silencieuse.
 */
internal class AudioTrackPcmOutput : PcmOutput {
    private class Voice(val samples: ShortArray, val gain: Float) {
        var position = 0
    }

    private val lock = Any()
    private val voices = ArrayList<Voice>()
    private var worker: Thread? = null

    override fun play(samples: ShortArray, gain: Float) {
        synchronized(lock) {
            // Au-delà de MAX_VOICES notes simultanées, la plus ancienne est coupée.
            if (voices.size >= MAX_VOICES) voices.removeAt(0)
            voices.add(Voice(samples, gain))
            if (worker == null) worker = Thread(::mixLoop, "NeckAtlas-audio").also { it.start() }
        }
    }

    private fun mixLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val track = try {
            createTrack()
        } catch (e: Exception) {
            Log.w(TAG, "Sortie audio indisponible", e)
            synchronized(lock) {
                voices.clear()
                worker = null
            }
            return
        }
        val mix = FloatArray(CHUNK_FRAMES)
        val pcm = ShortArray(CHUNK_FRAMES)
        var silentChunks = 0
        try {
            track.play()
            while (true) {
                mix.fill(0f)
                val idle = synchronized(lock) {
                    val iterator = voices.iterator()
                    while (iterator.hasNext()) {
                        val voice = iterator.next()
                        val count = minOf(CHUNK_FRAMES, voice.samples.size - voice.position)
                        for (i in 0 until count) mix[i] += voice.samples[voice.position + i] * voice.gain
                        voice.position += count
                        if (voice.position >= voice.samples.size) iterator.remove()
                    }
                    silentChunks = if (voices.isEmpty()) silentChunks + 1 else 0
                    // La décision et la remise à zéro de `worker` se font sous le même verrou : une note qui arrive juste
                    // après démarre un nouveau fil au lieu d'être confiée à celui-ci, qui s'en va.
                    (silentChunks > IDLE_CHUNKS).also { if (it) worker = null }
                }
                if (idle) break
                for (i in 0 until CHUNK_FRAMES) {
                    pcm[i] = mix[i].toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                track.write(pcm, 0, CHUNK_FRAMES)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur de lecture audio", e)
            synchronized(lock) {
                voices.clear()
                if (worker === Thread.currentThread()) worker = null
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun createTrack(): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBytes, CHUNK_FRAMES * 2 * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private companion object {
        const val TAG = "NeckAtlasAudio"
        const val CHUNK_FRAMES = 512
        const val IDLE_CHUNKS = SAMPLE_RATE / CHUNK_FRAMES / 2
        const val MAX_VOICES = 12
    }
}
