package com.nosenkomi.emotionclassification.classifier

import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import com.nosenkomi.emotionclassification.feature_extractor.JlibrosaExtractor
import com.nosenkomi.emotionclassification.mlmodel.MLModel
import com.nosenkomi.emotionclassification.record.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject


class EmotionClassifier @Inject constructor(
    private val serModel: MLModel<TensorBuffer>,
    private val yamnetModel: MLModel<TensorAudio>,
    private val audioRecorder: AudioRecorder,
) : Classifier {
    private val TAG = this::class.simpleName
    private var mel: TensorBuffer = serModel.getInput()
    private var tensorAudio: TensorAudio = yamnetModel.getInput()
    private val warmUpIntervalMs: Long = 2000
    private val classificationIntervalMs: Long = 500

    private val featureExtractor = JlibrosaExtractor()

    override fun start(): Flow<ClassificationResult<List<Category>>> {
        audioRecorder.start()
        return flow {
            Log.d(TAG, "recorder state: ${audioRecorder.getState()}")
            if (audioRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                emit(ClassificationResult.Error<List<Category>>("recorder is not initialized"))
                audioRecorder.stop()
                return@flow
            }
            delay(warmUpIntervalMs)
            while (currentCoroutineContext().isActive) {

                val newValues: FloatArray = audioRecorder.readData()
                if (newValues.isEmpty()) {
                    emit(ClassificationResult.Error<List<Category>>("could not read from recorder"))
                    audioRecorder.stop()
                    return@flow
                }

                tensorAudio.load(audioRecorder.getRecorder())
                val yamnetOutput = yamnetModel.runInference(tensorAudio)
                Log.i(TAG, "yamnet output: $yamnetOutput")
                if (yamnetOutput.any { category -> category.label.equals("Silence") }) {
                    emit(ClassificationResult.Success<List<Category>>(yamnetOutput))
                    delay(classificationIntervalMs)
                    continue
                }

                val processedData = featureExtractor.extractMelToBuffer(
                    newValues,
                    audioRecorder.getSampleRate(),
                    hopLength = 502,
                    scale = true,
                    transpose = true
                )
                mel.loadBuffer(processedData)

                val inferenceTime = SystemClock.uptimeMillis()
                Log.i(TAG, "inference time: $inferenceTime")

                val probabilities = serModel.runInference(mel)
                Log.i(TAG, "probabilities: $probabilities.")
                if (probabilities.any { category -> category.score.isNaN() }) {
                    Log.i(TAG, "mfcc has NaN: ${mel.floatArray.any { it.isNaN() }}")
                }
                emit(ClassificationResult.Success<List<Category>>(probabilities))
                delay(classificationIntervalMs)

            }
        }.flowOn(Dispatchers.IO)
    }

    private fun stopAudioClassification() {
        serModel.destroy()
        yamnetModel.destroy()
        audioRecorder.stop()
    }

    override fun stop() {
        stopAudioClassification()
    }
}