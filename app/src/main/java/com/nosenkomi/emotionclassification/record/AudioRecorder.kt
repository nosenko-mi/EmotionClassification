package com.nosenkomi.emotionclassification.record

interface AudioRecorder {

    fun start()
    fun stop()
    fun readData(): FloatArray

    fun getState(): Int
}