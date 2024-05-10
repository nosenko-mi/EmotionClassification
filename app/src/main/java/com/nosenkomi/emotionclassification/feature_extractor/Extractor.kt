package com.nosenkomi.emotionclassification.feature_extractor

import com.nosenkomi.emotionclassification.audio_features.AudioFeatures

interface Extractor {
    fun extractFeatures(audioData: FloatArray): AudioFeatures
}