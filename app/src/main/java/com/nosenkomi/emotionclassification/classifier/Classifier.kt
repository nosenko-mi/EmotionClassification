package com.nosenkomi.emotionclassification.classifier

import kotlinx.coroutines.flow.Flow
import org.tensorflow.lite.support.label.Category

interface Classifier {

    fun start(): Flow<ClassificationResult<List<Category>>>

    fun stop()
}