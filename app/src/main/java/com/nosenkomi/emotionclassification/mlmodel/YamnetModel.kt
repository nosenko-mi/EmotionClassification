package com.nosenkomi.emotionclassification.mlmodel

import android.content.Context
import android.util.Log
import com.nosenkomi.emotionclassification.ml.CnnGruV8SeqScaleTranspose16khz
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.AudioClassifier.AudioClassifierOptions
import org.tensorflow.lite.task.core.BaseOptions

class YamnetModel(
    private val context: Context,
) : MLModel<TensorAudio> {

    private lateinit var yamnetClassifier: AudioClassifier
    private var isActive = false

    init {
        create()
    }

    override fun runInference(input: TensorAudio): List<Category> {
        if (!isActive){
            create()
        }
        return yamnetClassifier.classify(input)[0].categories
    }

    override fun getInput(): TensorAudio {
        return yamnetClassifier.createInputTensorAudio()
    }

    override fun create() {
        if (isActive) {
            return
        }
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(2)

        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(0.3f)
            .setMaxResults(1)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            yamnetClassifier =
                AudioClassifier.createFromFileAndOptions(context, "yamnet.tflite", options)
            isActive = true
        } catch (e: IllegalStateException) {
            Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
        }
    }

    override fun destroy() {
        yamnetClassifier.close()
    }

}