package com.nosenkomi.emotionclassification.feature_extractor

import android.util.Log
import com.jlibrosa.audio.JLibrosa
import com.nosenkomi.emotionclassification.audio_features.AudioFeatures
import com.nosenkomi.emotionclassification.audio_features.MelSpectrogram
import com.nosenkomi.emotionclassification.util.maxValue
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.max

class JlibrosaExtractor : Extractor {

    private val jLibrosa = JLibrosa()

    override fun extractFeatures(audioData: FloatArray): AudioFeatures {
        val melSpectrogram = extractMel(audioData, sr = 16000)
//        val shape = Pair(melSpectrogram.data.size, melSpectrogram.data[0].size)
//        Log.d(javaClass.simpleName, "melSpectrogram shape: $shape")
        return AudioFeatures(features = listOf(MelSpectrogram()))

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

    fun extractMelToBuffer(
        yValues: FloatArray,
        sr: Int,
        nFFT: Int = 1024,
        nMels: Int = 64,
        hopLength: Int = 690
    ): ByteBuffer {
        val data = jLibrosa.generateMelSpectroGram(yValues, sr, nFFT, nMels, hopLength)
        val shape = Pair(data.size, data[0].size)
        println("mel shape: $shape")

        return dataToByteBuffer(data)
    }

    /**
     * Convert a power-spectrogram (magnitude squared) to decibel (dB) units.
     * Computes the scaling ``10 * log10(S / max(S))`` in a numerically stable way.
     * Based on:
     * https://librosa.org/doc/main/_modules/librosa/core/spectrum.html#power_to_db
     **/
    private fun powerToDB(data: Array<FloatArray>, ref: Float = 1f, aMin: Float = 1e-10f, topDB: Float = 80f): Array<FloatArray> {

        val refAbs = abs(ref)
        val topDBAbs = abs(topDB)

        val magnitudes = data.map { array ->
            array.map { it.absoluteValue }
        }

        val numRows = data.size
        val numCols = data[0].size

        val dbData: Array<FloatArray> = Array(numRows) { FloatArray(numCols) { 0f } }
        for (i in data.indices){
            for (j in data[i].indices){
                dbData[i][j] = (10.0 * log10(max(aMin, magnitudes[i][j]))).toFloat()
            }
        }

        for (i in dbData.indices){
            for (j in dbData[i].indices){
                dbData[i][j] -= (10.0 * log10(max(aMin, refAbs))).toFloat()
            }
        }

        val dbMax = dbData.maxValue()

        for (i in dbData.indices){
            for (j in dbData[i].indices){
                dbData[i][j] = max(dbData[i][j], dbMax- topDBAbs)
            }
        }

        return dbData
    }

    fun extractMel(
        yValues: FloatArray,
        sr: Int,
        nFFT: Int = 1024,
        nMels: Int = 64,
        hopLength: Int = 690
    ): Array<FloatArray> {
        val data = jLibrosa.generateMelSpectroGram(yValues, sr, nFFT, nMels, hopLength)
        val dataDB = powerToDB(data, ref = data.maxValue())
        val shape = Pair(data.size, data[0].size)
        println("mel shape: $shape")
        return dataDB
    }

    private fun dataToByteBuffer(data: Array<out FloatArray>): ByteBuffer {
        val floatArray = data.flatMap { it.toList() }.toTypedArray().toFloatArray()
        val byteBuffer =
            ByteBuffer.allocateDirect(floatArray.size * Float.SIZE_BYTES) // Allocate space
        println(
            "floatArray.size=${floatArray.size}; byteBuffer.size=${byteBuffer.capacity()}"
        )
        for (floatValue in floatArray) {
            byteBuffer.putFloat(floatValue)
        }

        byteBuffer.flip() // Prepare for reading
        byteBuffer.rewind() // set index to 0
        return byteBuffer
    }
}