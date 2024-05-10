package com.nosenkomi.emotionclassification.mlmodel

import android.content.Context
import com.nosenkomi.emotionclassification.ml.CnnGruV8SeqScaleTranspose16khz
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class CnnGruModel(
    private val context: Context,
) : MLModel<TensorBuffer> {

    private lateinit var model: CnnGruV8SeqScaleTranspose16khz
    private var isActive = false

    init {
        create()
    }
    override fun runInference(input: TensorBuffer): List<Category> {
        if (!isActive) {
            create()
        }
        return model.process(input).probabilityAsCategoryList
    }

    override fun getInput(): TensorBuffer {
        return TensorBuffer.createFixedSize(intArrayOf(1, 64, 64, 1), DataType.FLOAT32)
    }

    override fun create() {
        if (isActive) {
            return
        }
        model = CnnGruV8SeqScaleTranspose16khz.newInstance(context)
        isActive = true
    }

    override fun destroy() {
        isActive = false
        model.close()
    }

}