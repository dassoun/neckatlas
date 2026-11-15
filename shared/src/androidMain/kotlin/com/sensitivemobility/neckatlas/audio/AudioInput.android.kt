package com.sensitivemobility.neckatlas.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal actual fun createAudioInput(): AudioInput = AudioRecordInput()

/**
 * Le micro d'Android : un AudioRecord lu par un fil dédié, qui naît à [start] et s'arrête à [stop]. Les échantillons arrivent
 * par petits blocs ; une fenêtre glissante de [AudioInput.WINDOW_FRAMES] est remise à chaque [AudioInput.HOP_FRAMES] nouveaux.
 */
internal class AudioRecordInput : AudioInput {
    private val lock = Any()
    private var worker: Thread? = null

    @Volatile
    private var running = false

    @SuppressLint("MissingPermission") // la permission est demandée par l'écran avant d'écouter ; sans elle, on attrape l'exception
    override fun start(onWindow: (samples: FloatArray, sampleRate: Int) -> Unit): Boolean = synchronized(lock) {
        if (worker != null) return true
        val minBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBytes <= 0) return false
        // La source « non traitée » évite la suppression de bruit et le contrôle de gain du téléphone, qui prennent une note
        // tenue pour un bruit de fond et l'éteignent ; à défaut, le micro ordinaire.
        val record = listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC).firstNotNullOfOrNull { source ->
            try {
                AudioRecord(
                    source,
                    SAMPLE_RATE_IN,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBytes, BLOCK_FRAMES * 2 * 4),
                ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            } catch (e: Exception) {
                Log.w(TAG, "Source $source indisponible", e)
                null
            }
        } ?: return false
        running = true
        worker = Thread({ readLoop(record, onWindow) }, "NeckAtlas-micro").also { it.start() }
        true
    }

    override fun stop() {
        val thread = synchronized(lock) {
            running = false
            worker.also { worker = null }
        }
        // Le fil rend le micro lui-même, en sortant de sa boucle ; on l'attend brièvement pour ne pas le relancer trop tôt.
        runCatching { thread?.join(JOIN_MS) }
    }

    private fun readLoop(record: AudioRecord, onWindow: (FloatArray, Int) -> Unit) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val window = FloatArray(AudioInput.WINDOW_FRAMES)
        val block = ShortArray(BLOCK_FRAMES)
        var filled = 0
        var sinceLast = 0
        try {
            record.startRecording()
            while (running) {
                val read = record.read(block, 0, BLOCK_FRAMES)
                if (read <= 0) {
                    if (read < 0) break
                    continue
                }
                // La fenêtre glisse : les plus anciens échantillons sortent, les nouveaux entrent par la fin.
                if (read >= window.size) {
                    for (i in window.indices) window[i] = block[read - window.size + i] / 32768f
                } else {
                    System.arraycopy(window, read, window, 0, window.size - read)
                    for (i in 0 until read) window[window.size - read + i] = block[i] / 32768f
                }
                filled = minOf(window.size, filled + read)
                sinceLast += read
                if (filled == window.size && sinceLast >= AudioInput.HOP_FRAMES) {
                    sinceLast = 0
                    onWindow(window.copyOf(), SAMPLE_RATE_IN)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur de lecture du micro", e)
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    private companion object {
        const val TAG = "NeckAtlasMic"
        const val SAMPLE_RATE_IN = 44_100
        const val BLOCK_FRAMES = 1024
        const val JOIN_MS = 300L
    }
}

@Composable
actual fun rememberMicrophonePermission(): MicrophonePermission {
    val context = LocalContext.current
    fun check() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(check()) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        denied = !result
    }
    return MicrophonePermission(granted, denied) { launcher.launch(Manifest.permission.RECORD_AUDIO) }
}
