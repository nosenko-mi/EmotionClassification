package com.nosenkomi.emotionclassification.mlmodel

import org.tensorflow.lite.support.label.Category

interface MLModel<T>{
    fun getInput(): T
    fun runInference(input: T): List<Category>
    fun create()
    fun destroy()
}