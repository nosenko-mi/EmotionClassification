package com.nosenkomi.emotionclassification.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.nosenkomi.emotionclassification.classifier.LSTMClassifier
import kotlin.concurrent.thread

class AndroidAudioRecorder(
    private val context: Context,
    private val intervalSeconds: Int = 2,
): AudioRecorder {

    private val TAG = this::class.simpleName

    private val RECORDER_SAMPLE_RATE = 44100
    private val RAW_AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    private val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    @Volatile
    private var isRecordingAudio = false
    private var recorder: AudioRecord? = null

    @Volatile
    var data: ByteArray = ByteArray(BUFFER_SIZE_RECORDING/2)


    override fun start() {
        createRecorder()
        if (recorder!!.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "error initializing AudioRecord")
            return
        }
        isRecordingAudio = true
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

    override fun readData(): FloatArray {
        if (!isRecordingAudio){
            return floatArrayOf()
        }
        val newData = FloatArray(recorder!!.channelCount * recorder!!.bufferSizeInFrames)
        val loadedValues = recorder!!.read(newData, 0, newData.size, AudioRecord.READ_NON_BLOCKING)
        if (loadedValues < 0) return floatArrayOf()
        return newData
    }

    override fun getState(): Int {
        if (!isRecordingAudio ){
            return AudioRecord.STATE_UNINITIALIZED
        } else {
            return recorder!!.state
        }
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
            Log.d(TAG, "Permissions denied")
            return
        }
        val bufferSize =
            (intervalSeconds * 2 * RECORDER_SAMPLE_RATE) // 2 sec * 2 (compensate for mono) * sample rate
        Log.d(TAG, "buffer size: $bufferSize; min size: $BUFFER_SIZE_RECORDING")
        recorder = AudioRecord(RAW_AUDIO_SOURCE, RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
    }

}