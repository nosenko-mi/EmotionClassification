package com.nosenkomi.emotionclassification.classifier

import org.tensorflow.lite.support.label.Category

interface Classifier {

    fun start(): List<Category>

    fun stop()
}