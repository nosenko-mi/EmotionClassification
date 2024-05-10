package com.nosenkomi.emotionclassification.record

import android.media.AudioRecord

interface AudioRecorder {

    fun start()
    fun stop()
    fun readData(): FloatArray
    fun getRecorder(): AudioRecord
    fun getState(): Int

    fun getSampleRate(): Int
}