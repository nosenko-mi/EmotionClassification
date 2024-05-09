package com.nosenkomi.emotionclassification.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.nosenkomi.emotionclassification.util.replace

class AndroidAudioRecorder(
    private val context: Context,
    private val intervalSeconds: Int = 2,
    private val sampleRate: Int = 44100,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_FLOAT,
    private val rawAudioSource: Int = MediaRecorder.AudioSource.UNPROCESSED
    ): AudioRecorder {

    companion object {
        const val TAG = "AndroidAudioRecorder"
    }

    private val minBufferSize: Int =
    AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val recordingSize: Int = (intervalSeconds * sampleRate * Float.SIZE_BYTES)
    private val queue: ArrayDeque<Float> = ArrayDeque()
    private lateinit var data: FloatArray

    @Volatile
    private var isRecordingAudio = false
    private var recorder: AudioRecord? = null

    override fun start() {
        createRecorder()
        if (recorder!!.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "error initializing AudioRecord")
            return
        }
        isRecordingAudio = true
        recorder!!.startRecording()
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
        Log.d(TAG, "readData: expected size=${newData.size}; loaded values=${loadedValues}")
        if (loadedValues <= 0) return floatArrayOf()

        if (queue.isEmpty()){
            queue.addAll(newData.toTypedArray())
            while (queue.size < newData.size){
                queue.add(0f)
            }
        } else{
            queue.addAll(newData.toTypedArray())
            while (queue.size > newData.size){
                queue.removeFirst()
            }
        }
        Log.d(TAG, "readData: queue size=${queue.size}; values=${queue.toList()}")

        return queue.toFloatArray()

    }

    override fun getRecorder(): AudioRecord {
        if (recorder == null) {
            createRecorder()
        }
        return recorder!!
    }

    override fun getState(): Int {
        return if (!isRecordingAudio ){
            AudioRecord.STATE_UNINITIALIZED
        } else {
            recorder!!.state
        }
    }

    override fun getSampleRate(): Int {
        return sampleRate
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
        val bufferSize = (recordingSize)
        Log.d(TAG, "buffer size: $bufferSize; min size: ${this.minBufferSize}")
        recorder = AudioRecord(rawAudioSource, sampleRate, channelConfig, audioFormat, bufferSize)
        data = FloatArray(recorder!!.channelCount * recorder!!.bufferSizeInFrames)

    }

}