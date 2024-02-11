package com.nosenkomi.emotionclassification.util

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder.BIG_ENDIAN
import java.nio.ByteOrder.LITTLE_ENDIAN


// source of inspiration
// https://github.com/rizveeredwan/working-with-wav-files-in-android?tab=readme-ov-file#reading-a-wav-file

class WAVReader(
    private val context: Context
) {
    private var type = intArrayOf(0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1)
    private var numberOfBytes = intArrayOf(4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 2, 4, 4)
    private var chunkSize = 0
    var subChunk1Size: Int = 0
    var sampleRate: Int = 0
    var byteRate: Int = 0
    var subChunk2Size: Int = 1
    var bytePerSample: Int = 0
    var audioFormat: Short = 0
    var numChannels: Short = 0
    var blockAlign: Short = 0
    var bitsPerSample: Short = 8
    var chunkID: String? = null
    var format: String? = null
    var subChunk2ID: String? = null
    var subChunk1ID: String? = null

    fun readAsset(assetName: String) {
        val assets = context.assets
        val inputStream: InputStream = assets.open(assetName)
        val headerBuffer = ByteArray(78)
        try {
            val bytesRead = inputStream.read(headerBuffer, 0, 78)
            val chunk = headerBuffer.copyOfRange(36, 40)
            val chunk2 = headerBuffer.copyOfRange(70, 74)
            val chunkId = String(chunk)
            val chunk2Id = String(chunk2)
            Log.d(
                javaClass.simpleName,
                "bytes read: $bytesRead; " +
                        "chunk id: $chunkId; " +
                        "chunk 2 id: $chunk2Id"
            )

            if (chunkId != "data") {
                Log.e(
                    javaClass.simpleName,
                    "could not read file $assetName. Unknown header $chunkId"
                )
            }
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "${e.printStackTrace()}")
        }

        inputStream.close()

    }

    fun readAudioFile(assetName: String): FloatArray? {
        return try {
            val assets = context.assets
            val inputStream: InputStream = assets.open(assetName)

            var byteBuffer: ByteBuffer
            for (i in numberOfBytes.indices) {
                val byteArray = ByteArray(numberOfBytes[i])
                var read = inputStream.read(byteArray, 0, numberOfBytes[i])
                byteBuffer = byteArrayToNumber(byteArray, numberOfBytes[i], type[i])
                when (i) {
                    0 -> {chunkID = String(byteArray); println(chunkID)}
                    1 -> {chunkSize = byteBuffer.int; println(chunkSize)}
                    2 -> {format = String(byteArray); println(format)}
                    3 -> {subChunk1ID = String(byteArray); println(subChunk1ID)}
                    4 -> {subChunk1Size = byteBuffer.int; println(subChunk1Size)}
                    5 -> {audioFormat = byteBuffer.short; println(audioFormat)}
                    6 -> {numChannels = byteBuffer.short; println(numChannels)}
                    7 -> {sampleRate = byteBuffer.int; println(sampleRate)}
                    8 -> {byteRate = byteBuffer.int; println(byteRate)}
                    9 -> {blockAlign = byteBuffer.short; println(blockAlign)}
                    10 -> {bitsPerSample = byteBuffer.short; println(bitsPerSample)}
                    11 -> {
                        subChunk2ID = String(byteArray)
                        if (subChunk2ID!!.compareTo("data") == 0) {
                            continue
                        } else if (subChunk2ID!!.compareTo("LIST") == 0) {
                            val byteArray2 = ByteArray(4)
                            read = inputStream.read(byteArray2, 0, 4)
                            byteBuffer = byteArrayToNumber(byteArray2, 4, 1)
                            val temp = byteBuffer.int
                            //redundant data reading
                            val byteArray3 = ByteArray(temp)
                            read = inputStream.read(byteArray3, 0, temp)
                            read = inputStream.read(byteArray2, 0, 4)
                            subChunk2ID = String(byteArray2)
                        }
                    }
                    12 -> {
                        subChunk2Size = byteBuffer.int
                        println(subChunk2Size)
                    }
                    else -> {

                    }
                }

            }
            bytePerSample = bitsPerSample / 8
            var value: Float
            val dataVector = ArrayList<Float>()
            while (true) {
                val byteArray = ByteArray(bytePerSample)
                val v = inputStream.read(byteArray, 0, bytePerSample)
                value = convertToFloat(byteArray, 1)
                dataVector.add(value)
                if (v == -1) break
            }
            val data = FloatArray(dataVector.size)
            for (i in dataVector.indices) {
                data[i] = dataVector[i]
            }
            inputStream.close()
//            System.out.println("Total data bytes $sum")
            Log.d(javaClass.simpleName, "data size: ${data.size}")
            data
        } catch (e: Exception) {
            println("Error: $e")
            FloatArray(1)
        }
    }

    private fun convertToFloat(array: ByteArray?, type: Int): Float {
        val buffer = ByteBuffer.wrap(array)
        if (type == 1) {
            buffer.order(LITTLE_ENDIAN)
        }
        return buffer.short.toFloat()
    }


    private fun byteArrayToNumber(bytes: ByteArray, numOfBytes: Int, type: Int): ByteBuffer {
        val buffer =
            ByteBuffer.allocate(numOfBytes); //allocating a space in ByteBuffer object based on the desired numOfBytes value
        if (type == 0){
            buffer.order(BIG_ENDIAN); // Check the illustration. If it says little endian, use LITTLE_ENDIAN
        }
        else{
            buffer.order(LITTLE_ENDIAN);
        }
        buffer.put(bytes); // putting the bytes
        buffer.rewind(); // this is basically for byte size syncronization
        return buffer;
    }
//    usage:
//    byteBuffer = ByteArrayToNumber(byteArray, numOfBytes, type); // type = BIG or LITTLE ENDIAN, numOfBytes = denotes the allocating space size, byteArray = where the read byte information is
//    byteBuffer.getInt() // will provide the int converted value stored in byteBuffer
//    byteBuffer.getShort() // will provide the short converted value stored in byteBuffer

}
