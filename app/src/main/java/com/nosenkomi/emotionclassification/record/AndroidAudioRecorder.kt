package com.nosenkomi.emotionclassification.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlin.concurrent.thread

class AndroidAudioRecorder(
    private val context: Context
): AudioRecorder {

    private val TAG = this::class.simpleName

    private val RECORDER_SAMPLE_RATE = 44100
    private val RAW_AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    @Volatile
    private var isRecordingAudio = false
    private var recorder: AudioRecord? = null

    @Volatile
    var data: ByteArray = ByteArray(BUFFER_SIZE_RECORDING/2)


    override fun start() {
        isRecordingAudio = true
        createRecorder()
        if (recorder!!.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "error initializing AudioRecord")
            return
        }
        recorder!!.startRecording()
        val recodingThread = thread {
            processAudioInput()
        }
    }

    override fun stop() {
        isRecordingAudio = false
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    private fun processAudioInput(){
        while (isRecordingAudio) {
            val bytesRead = recorder!!.read(data, 0, data.size)
            if (bytesRead <= 0) {
                break
            }
            Log.d(TAG, "First byte: ${data.first()}")
        }
    }

    private fun createRecorder(){
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling ActivityCompat#requestPermissions
            return
        }
        recorder = AudioRecord(RAW_AUDIO_SOURCE, RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE_RECORDING)
    }


}