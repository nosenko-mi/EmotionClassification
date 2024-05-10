package com.nosenkomi.emotionclassification.record

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.nosenkomi.emotionclassification.classifier.ClassificationResult
import com.nosenkomi.emotionclassification.classifier.Classifier
import com.nosenkomi.emotionclassification.feature_extractor.JlibrosaExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor

class CustomClassifier(
    private val context: Context,
    private var currentModel: String = CUSTOM_MODEL,
    private var classificationThreshold: Float = DISPLAY_THRESHOLD,
    private var overlap: Float = DEFAULT_OVERLAP_VALUE,
    private var numOfResults: Int = DEFAULT_NUM_OF_RESULTS,
    private var currentDelegate: Int = 0,
    private var numThreads: Int = 2
): Classifier {

    private val TAG = this::class.simpleName

    private val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private var recorder: AudioRecord? = null
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var executor: ScheduledThreadPoolExecutor

    private var chunkSize: Int = 3 * RECORDER_SAMPLE_RATE * CHANNEL_CONFIG * 16 / 8

    init {
        initClassifier()
    }

    fun initClassifier() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        // Configures a set of parameters for the classifier and what results will be returned.
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold)
            .setMaxResults(numOfResults)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            // Create the classifier and required supporting objects
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            tensorAudio = classifier.createInputTensorAudio()



        } catch (e: IllegalStateException) {
            Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
        }
    }

    override fun start(): Flow<ClassificationResult<List<Category>>> {
        recorder = classifier.createAudioRecord()
        return flow {
            if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
//                emit(Resource.Success<Gif>(gif))

                emit(ClassificationResult.Error<List<Category>>("recorder is not initialized"))
                return@flow
            }

            recorder?.startRecording()

            val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
                    classifier.requiredTensorAudioFormat.sampleRate) * 1000

//            val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()
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

    private fun classifyAudio(): Flow<List<Category>> = flow {
        if (recorder == null || recorder?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            emit(emptyList())
            return@flow
        }

//        val buffer = ByteArray(chunkSize)
//        if (recorder?.state == AudioRecord.STATE_INITIALIZED) {
//            val bytesRead = recorder!!.read(buffer, 0, chunkSize)
//            processAudioData(buffer, bytesRead)
//        } else {
//            Log.e(TAG, "AudioRecord not initialized properly")
//        }
        val newData = FloatArray(recorder!!.channelCount * recorder!!.bufferSizeInFrames)
        val loadedValues = recorder!!.read(newData, 0, newData.size, AudioRecord.READ_NON_BLOCKING)
        if (loadedValues > 0) {
            Log.d(TAG, "Processed audio data. Bytes read: $loadedValues, Data: ${newData.size}")
            val extractor = JlibrosaExtractor()

            val audioFeatures = extractor.extractFeatures(newData)
            extractor.extractMFCC(newData, 22050)
            Log.d(TAG, "Processed audio data. ${audioFeatures.features}")
            // Processed audio data. Bytes read: 31200, Data: 31200
//            processAudioData(newData, loadedValues)
        }

        tensorAudio.load(recorder)
        // classify
        val inferenceTime = SystemClock.uptimeMillis()
        val output = classifier.classify(tensorAudio)
        emit(output[0].categories)
    }.flowOn(Dispatchers.Default) // Use a background thread for inference, if necessary

    private fun processAudioData(data: ByteArray, bytesRead: Int) {
        Log.d(TAG, "Processed audio data. Bytes read: $bytesRead, Data: ${data.toList()}")
    }

    private fun stopAudioClassification() {
        recorder?.stop()
        recorder?.release() // Release the AudioRecord resources
    }

    override fun stop() {
        stopAudioClassification()
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
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CUSTOM_MODEL = "lstm-i64x64-p35k-oAe100-f068-v2.tflite"
    }

}