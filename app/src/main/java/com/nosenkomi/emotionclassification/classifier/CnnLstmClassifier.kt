package com.nosenkomi.emotionclassification.classifier

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.nosenkomi.emotionclassification.feature_extractor.JlibrosaExtractor
import com.nosenkomi.emotionclassification.ml.CnnGruV8SeqScaleTranspose16khz
import com.nosenkomi.emotionclassification.record.AndroidAudioRecorder
import com.nosenkomi.emotionclassification.record.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class CnnLstmClassifier(
    private val context: Context,
): Classifier {
    private val TAG = this::class.simpleName
    private lateinit var model: CnnGruV8SeqScaleTranspose16khz
    private lateinit var mfcc: TensorBuffer
    private val interval = 2000L // ms
    private val smallIntervalMs = 500L
    private var audioRecorder: AudioRecorder = AndroidAudioRecorder(context, intervalSeconds = 2, sampleRate = 16000)
    private val featureExtractor = JlibrosaExtractor()

    init {
        initialize()
    }

    private fun initialize() {
        // Creates inputs for reference.
        // 1 float32 = 4 bytes
        // 64x64 = 4096 values. 4096*4 = 16384 bytes
        mfcc = TensorBuffer.createFixedSize(intArrayOf(1, 64, 64, 1), DataType.FLOAT32)
    }

    override fun start(): Flow<ClassificationResult<List<Category>>> {
//        createRecorder()
        model = CnnGruV8SeqScaleTranspose16khz.newInstance(context)
        audioRecorder.start()
        return flow {
            Log.d(TAG, "recorder state: ${audioRecorder!!.getState()}")
            if (audioRecorder == null || audioRecorder!!.getState() == AudioRecord.STATE_UNINITIALIZED) {
                emit(ClassificationResult.Error<List<Category>>("recorder is not initialized"))
                audioRecorder.stop()
//                releaseRecorder()
                return@flow
            }
            delay(interval)   // Delay before to spare 1 classification
            while (currentCoroutineContext().isActive) {

//                val newValues: FloatArray = readFromRecorder()
                val newValues: FloatArray = audioRecorder.readData()

                if (newValues.isEmpty()){
                    emit(ClassificationResult.Error<List<Category>>("could not read from recorder"))
                    audioRecorder.stop()
//                    releaseRecorder()
                    return@flow
                }

                val processedData = featureExtractor.extractMelToBuffer(newValues, 16000, hopLength = 502, scale = true, transpose = true)
                mfcc.loadBuffer(processedData)

                val inferenceTime = SystemClock.uptimeMillis()
                Log.i(TAG, "inference time: $inferenceTime")
                val outputs = model.process(mfcc)
                val probability = outputs.probabilityAsCategoryList
                Log.i(TAG, "probabilities: $probability")
                if (probability.any { category -> category.score.isNaN() }){
                    Log.i(TAG, "mfcc has NaN: ${mfcc.floatArray.any{it.isNaN()}}")
                }
                emit(ClassificationResult.Success<List<Category>>(probability))
                delay(interval)

            }
        }.flowOn(Dispatchers.IO) // Use a background thread for recording and classification, if necessary
    }

//    private fun readFromRecorder(): FloatArray{
//        val newData = FloatArray(audioRecorder!!.channelCount * audioRecorder!!.bufferSizeInFrames)
//        val loadedValues = audioRecorder!!.read(newData, 0, newData.size, AudioRecord.READ_NON_BLOCKING)
//        if (loadedValues < 0) return floatArrayOf()
//        return newData
//    }
//
//    private fun createRecorder(){
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Log.e(TAG, "Could not create recorder. Permissions denied.")
//            // TODO: Consider calling ActivityCompat#requestPermissions
//            return
//        }
////        val bufferSize = mfcc.flatSize * Float.SIZE_BYTES
//        val bufferSize = 2 * 2 * RECORDER_SAMPLE_RATE // 2 sec * 2 (compensate for mono) * sample rate
//        audioRecorder = AudioRecord(RAW_AUDIO_SOURCE, RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
//    }
//
//    private fun releaseRecorder(){
//        audioRecorder?.let {
//            it.stop()
//            it.release() // !set to null after release
//        }
//        audioRecorder = null
//    }

    private fun stopAudioClassification() {
        model.close()
    }

    override fun stop() {
        stopAudioClassification()
        audioRecorder.stop()
//        releaseRecorder()
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