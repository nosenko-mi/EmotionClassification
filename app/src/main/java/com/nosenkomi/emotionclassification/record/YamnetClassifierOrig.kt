package com.nosenkomi.emotionclassification.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import com.nosenkomi.emotionclassification.classifier.AudioClassificationListener
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class YamnetClassifierOrig(
    private val context: Context,
    val listener: AudioClassificationListener,
    var currentModel: String = YAMNET_MODEL,
    var classificationThreshold: Float = DISPLAY_THRESHOLD,
    var overlap: Float = DEFAULT_OVERLAP_VALUE,
    var numOfResults: Int = DEFAULT_NUM_OF_RESULTS,
    var currentDelegate: Int = 0,
    var numThreads: Int = 2
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

    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var executor: ScheduledThreadPoolExecutor

    private val classifyRunnable = Runnable {
        classifyAudio()
    }

    init {
        initClassifier()
    }

    fun initClassifier() {
        // Set general detection options, e.g. number of used threads
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU.
        // Possible to also use a GPU delegate, but this requires that the classifier be created
        // on the same thread that is using the classifier, which is outside of the scope of this
        // sample's design.
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
            recorder = classifier.createAudioRecord()
            startAudioClassification()
        } catch (e: IllegalStateException) {
            listener.onError(
                "Audio Classifier failed to initialize. See error logs for details"
            )

            Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
        }
    }

    fun startAudioClassification() {
        if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        recorder?.startRecording()
        executor = ScheduledThreadPoolExecutor(1)

        // Each model will expect a specific audio recording length. This formula calculates that
        // length using the input buffer size and tensor format sample rate.
        // For example, YAMNET expects 0.975 second length recordings.
        // This needs to be in milliseconds to avoid the required Long value dropping decimals.
        val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
                classifier.requiredTensorAudioFormat.sampleRate) * 1000

        val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()

        executor.scheduleAtFixedRate(
            classifyRunnable,
            0,
            interval,
            TimeUnit.MILLISECONDS)
    }

    private fun classifyAudio() {
        tensorAudio.load(recorder)
        var inferenceTime = SystemClock.uptimeMillis()
        val output = classifier.classify(tensorAudio)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        listener.onResult(output[0].categories, inferenceTime)
    }

    fun stopAudioClassification() {
        recorder?.stop()
        executor.shutdownNow()
    }


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

    override fun readData(): FloatArray {
        TODO("Not yet implemented")
    }

    override fun getRecorder(): AudioRecord {
        TODO("Not yet implemented")
    }

    override fun getState(): Int {
        TODO("Not yet implemented")
    }

    override fun getSampleRate(): Int {
        TODO("Not yet implemented")
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

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_NNAPI = 1
        const val DISPLAY_THRESHOLD = 0.3f
        const val DEFAULT_NUM_OF_RESULTS = 2
        const val DEFAULT_OVERLAP_VALUE = 0.5f
        const val YAMNET_MODEL = "yamnet.tflite"
        const val SPEECH_COMMAND_MODEL = "speech.tflite"
    }

}