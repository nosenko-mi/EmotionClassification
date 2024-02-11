package com.nosenkomi.emotionclassification.classifier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import com.nosenkomi.emotionclassification.feature_extractor.JlibrosaExtractor
import com.nosenkomi.emotionclassification.ml.LstmI64x64P35kOae100F068V2
import com.nosenkomi.emotionclassification.record.AndroidAudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class LSTMClassifier(
    private val context: Context,
): Classifier {
    private val TAG = this::class.simpleName
    private var audioRecorder: AudioRecord? = null
    private lateinit var model: LstmI64x64P35kOae100F068V2
    private lateinit var mfcc: TensorBuffer

    init {
        initialize()
    }

    private fun initialize() {
        // Creates inputs for reference.
        // 1 float32 = 4 bytes
        // 64x64 = 4096 values. 4096*4 = 16384 bytes
        mfcc = TensorBuffer.createFixedSize(intArrayOf(1, 64, 64), DataType.FLOAT32)
    }

    private fun classifyAudio(): Flow<List<Category>> = flow {
        Log.d(TAG, "classifyAudio: start")
        if (audioRecorder!!.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.d(TAG, "classifyAudio:  emit(emptyList())")
            emit(emptyList())
            return@flow
        }

        Log.d(TAG, "classifyAudio:  create byte array")
        val newData = FloatArray(audioRecorder!!.channelCount * audioRecorder!!.bufferSizeInFrames)

        Log.d(TAG, "classifyAudio:  recorder buffer.sizeInFrames=${audioRecorder!!.bufferSizeInFrames}")
        Log.d(TAG, "classifyAudio:  newData.size=${newData.size}")
        Log.d(TAG, "classifyAudio:  read from recorder")
        val loadedValues = audioRecorder!!.read(newData, 0, newData.size, AudioRecord.READ_NON_BLOCKING)
        Log.d(TAG, "classifyAudio: Bytes read: $loadedValues")

        if (loadedValues > 0) {
            Log.d(TAG, "Process audio data. Bytes read: $loadedValues, Data: ${newData.toList()}")
            val extractor = JlibrosaExtractor()
//            val floatArray = FloatArray(loadedValues.first.remaining() / Float.SIZE_BYTES) // Calculate size based on bytes

            val processedData = extractor.extractMFCCtoBuffer(newData, 44100)
            Log.d(TAG, "Process audio data. Bytes read: $loadedValues, MFCC.flatSize: ${mfcc.flatSize}")
            mfcc.loadBuffer(processedData)

            val inferenceTime = SystemClock.uptimeMillis()
            val outputs = model.process(mfcc)
            val probability = outputs.probabilityAsCategoryList
            Log.d(TAG, "Probability: ${probability}")
            emit(probability)
        } else{
            emit(emptyList())
            return@flow
        }


    }.flowOn(Dispatchers.Default) // Use a background thread for inference, if necessary


    fun startAudioClassification(): Flow<ClassificationResult<List<Category>>> {
        createRecorder()
        model = LstmI64x64P35kOae100F068V2.newInstance(context)
        audioRecorder?.startRecording()
        return flow {
            Log.d(TAG, "recorder state: ${audioRecorder!!.state}")
            if (audioRecorder == null || audioRecorder!!.state == AudioRecord.STATE_UNINITIALIZED) {
                emit(ClassificationResult.Error<List<Category>>("recorder is not initialized"))
                releaseRecorder()
                return@flow
            }

//            val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
//                    classifier.requiredTensorAudioFormat.sampleRate) * 1000

            val interval = 2000L
            Log.d(TAG, "interval: $interval (ms)")
            while (true) {
                delay(interval)
                Log.d(TAG, "classification tic")
                classifyAudio().collect { categories ->
                    emit(ClassificationResult.Success<List<Category>>(categories))
                }
                // Delay for the specified interval before the next classification
            }
        }.flowOn(Dispatchers.IO) // Use a background thread for recording and classification, if necessary
    }

    private fun createRecorder(){
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Could not create recorder. Permissions denied.")
            // TODO: Consider calling ActivityCompat#requestPermissions
            return
        }
//        val bufferSize = mfcc.flatSize * Float.SIZE_BYTES
        val bufferSize = 2 * 2 * RECORDER_SAMPLE_RATE // 2 sec * 2 (compensate for mono) * sample rate
        audioRecorder = AudioRecord(RAW_AUDIO_SOURCE, RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
    }

    private fun releaseRecorder(){
        audioRecorder?.let {
            it.stop()
            it.release() // !set to null after release
        }
        audioRecorder = null
    }

    private fun stopAudioClassification() {
        model.close()
    }


    override fun start(): List<Category> {
        createRecorder()

        if (audioRecorder == null || audioRecorder!!.state == AudioRecord.STATE_UNINITIALIZED){
            Log.e(TAG, "Cannot start classification: recorder error")
            releaseRecorder()
            return emptyList()
        }

        startAudioClassification()
        return emptyList()
    }

    override fun stop() {
        stopAudioClassification()
        releaseRecorder()
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_NNAPI = 1
        const val DISPLAY_THRESHOLD = 0.3f
        const val DEFAULT_NUM_OF_RESULTS = 2
        const val DEFAULT_OVERLAP_VALUE = 0.5f
        const val RECORDER_SAMPLE_RATE = 44100
        const val RAW_AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_COUNT = 1
//        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    }

}