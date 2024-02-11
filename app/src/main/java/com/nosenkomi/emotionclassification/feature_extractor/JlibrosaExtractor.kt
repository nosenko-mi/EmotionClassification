package com.nosenkomi.emotionclassification.feature_extractor

import android.util.Log
import com.jlibrosa.audio.JLibrosa
import com.nosenkomi.emotionclassification.audio_features.AudioFeatures
import com.nosenkomi.emotionclassification.audio_features.MelSpectrogram
import java.nio.ByteBuffer

class JlibrosaExtractor : Extractor {

    private val jLibrosa = JLibrosa()

    override fun extractFeatures(audioData: FloatArray): AudioFeatures {
        val melSpectrogram = extractMelSpectrogram(audioData)
        val shape = Pair(melSpectrogram.data.size, melSpectrogram.data[0].size)
        Log.d(javaClass.simpleName, "melSpectrogram shape: $shape")
        return AudioFeatures(features = listOf(melSpectrogram))

    }

    public fun extractMFCC(
        yValues: FloatArray,
        sr: Int,
        nMFCC: Int = 64,
        nFFT: Int = 1024,
        nMels: Int = 128,
        hopLength: Int = 492
    ): Array<FloatArray> {
        val data = jLibrosa.generateMFCCFeatures(yValues, sr, nMFCC, nFFT, nMels, hopLength)
        val shape = Pair(data.size, data[0].size)
        Log.d(javaClass.simpleName, "mfcc shape: $shape")
        return data
    }

    public fun extractMFCCtoBuffer(
        yValues: FloatArray,
        sr: Int,
        nMFCC: Int = 64,
        nFFT: Int = 1024,
        nMels: Int = 128,
        hopLength: Int = 690
    ): ByteBuffer {
        val data = jLibrosa.generateMFCCFeatures(yValues, sr, nMFCC, nFFT, nMels, hopLength)
        val shape = Pair(data.size, data[0].size)
        Log.d(javaClass.simpleName, "mfcc shape: $shape")

        val floatArray = data.flatMap { it.toList() }.toTypedArray().toFloatArray()
        val byteBuffer =
            ByteBuffer.allocateDirect(floatArray.size * Float.SIZE_BYTES) // Allocate space
        Log.d(
            javaClass.simpleName,
            "floatArray.size=${floatArray.size}; byteBuffer.size=${byteBuffer.capacity()}"
        )
        for (floatValue in floatArray) {
            byteBuffer.putFloat(floatValue)
        }

        byteBuffer.flip() // Prepare for reading
        byteBuffer.rewind() // set index to 0
        return byteBuffer
    }

    private fun extractMelSpectrogram(yValues: FloatArray): MelSpectrogram {
        val data = jLibrosa.generateMelSpectroGram(yValues)
        return MelSpectrogram(data = data)
    }
}