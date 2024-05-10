package com.nosenkomi.emotionclassification.feature_extractor

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.ZeroCrossingRateProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.mfcc.MFCC
import com.nosenkomi.emotionclassification.audio_features.AudioFeatures

class Extractor1D(
    audioBufferSize: Int,
    sampleRate: Int,
    bufferOverlap: Int,
): Extractor {

    private lateinit var dispatcher: AudioDispatcher

    init {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, audioBufferSize, bufferOverlap)
    }

    fun extractZRC(){
//        val processor = ZeroCrossingRateProcessor()
    }
    fun extractChromaSTFT(){

    }
    fun extractMFCC(){
//        val processor = MFCC()
    }
    fun extractRMS(){

    }
    fun extractMelSpectrogram(){

    }

    override fun extractFeatures(audioData: FloatArray): AudioFeatures {
        TODO("Not yet implemented")
    }
}