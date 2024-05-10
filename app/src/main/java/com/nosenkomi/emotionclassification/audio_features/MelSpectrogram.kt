package com.nosenkomi.emotionclassification.audio_features

data class MelSpectrogram(
    val data: Array<DoubleArray> = emptyArray()
): Feature
