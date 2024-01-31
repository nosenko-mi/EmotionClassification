package com.nosenkomi.emotionclassification.feature_extractor

import android.util.Log
import com.jlibrosa.audio.JLibrosa
import com.nosenkomi.emotionclassification.audio_features.AudioFeatures
import com.nosenkomi.emotionclassification.audio_features.MelSpectrogram

class JlibrosaExtractor: Extractor {

    private val jLibrosa = JLibrosa()

/*
    fun main(args: Array<String>) {
        // TODO Auto-generated method stub
        val mNumFrames: Int
        val mSampleRate: Int
        val mChannels: Int
        val sourceFile = File("audioFiles/100263-2-0-126.wav")
        var wavFile: WavFile? = null
        wavFile = WavFile.openWavFile(sourceFile)
        mNumFrames = wavFile.numFrames.toInt()
        mSampleRate = wavFile.sampleRate.toInt()
        mChannels = wavFile.numChannels
        val buffer = Array(mChannels) {
            FloatArray(
                mNumFrames
            )
        }
        var frameOffset = 0
        val loopCounter = mNumFrames * mChannels / 4096 + 1
        for (i in 0 until loopCounter) {
            frameOffset = wavFile.readFrames(buffer, mNumFrames, frameOffset).toInt()
        }
        val df = DecimalFormat("#.#####")
        df.setRoundingMode(RoundingMode.CEILING)
        val meanBuffer = DoubleArray(mNumFrames)
        for (q in 0 until mNumFrames) {
            var frameVal = 0.0
            for (p in 0 until mChannels) {
                frameVal = frameVal + buffer[p][q]
            }
            meanBuffer[q] = df.format(frameVal / mChannels).toDouble()
        }

        val mfccConvert = MFCC()
        mfccConvert.setSampleRate(mSampleRate)
        val nMFCC = 40
        mfccConvert.setN_mfcc(nMFCC)
        val mfccInput: FloatArray = mfccConvert.process(meanBuffer)
        val nFFT = mfccInput.size / nMFCC
        val mfccValues = Array(nMFCC) {
            DoubleArray(
                nFFT
            )
        }

        //loop to convert the mfcc values into multi-dimensional array
        for (i in 0 until nFFT) {
            var indexCounter = i * nMFCC
            val rowIndexValue = i % nFFT
            for (j in 0 until nMFCC) {
                mfccValues[j][rowIndexValue] = mfccInput[indexCounter].toDouble()
                indexCounter++
            }
        }

        //code to take the mean of mfcc values across the rows such that
        //[nMFCC x nFFT] matrix would be converted into
        //[nMFCC x 1] dimension - which would act as an input to tflite model
        val meanMFCCValues = FloatArray(nMFCC)
        for (p in 0 until nMFCC) {
            var fftValAcrossRow = 0.0
            for (q in 0 until nFFT) {
                fftValAcrossRow = fftValAcrossRow + mfccValues[p][q]
            }
            val fftMeanValAcrossRow = fftValAcrossRow / nFFT
            meanMFCCValues[p] = fftMeanValAcrossRow.toFloat()
        }
    }
*/

    override fun extractFeatures(audioData: FloatArray): AudioFeatures {
        val melSpectrogram = extractMelSpectrogram(audioData)
        val shape = Pair(melSpectrogram.data.size, melSpectrogram.data[0].size)
        Log.d(javaClass.simpleName, "melSpectrogram shape: $shape")
        return AudioFeatures(features = listOf(melSpectrogram))

    }

    private fun extractMelSpectrogram(yValues: FloatArray): MelSpectrogram {
        val data = jLibrosa.generateMelSpectroGram(yValues)
        return MelSpectrogram(data = data)
    }
}