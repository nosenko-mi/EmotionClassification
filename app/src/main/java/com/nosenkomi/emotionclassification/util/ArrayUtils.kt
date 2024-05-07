package com.nosenkomi.emotionclassification.util

fun Array<FloatArray>.maxValue(): Float{
    if (this.isEmpty()){
        throw RuntimeException("array is empty")
    }

    var maxValue = this[0][0]
    for (i in this.indices){
        for (j in this[i].indices){
            if (this[i][j] > maxValue){
                maxValue = this[i][j]
            }
        }
    }
    return maxValue
}

fun Array<FloatArray>.maxValue(index: Int, axis: Int = 0): Float{
    if (this.isEmpty()){
        throw RuntimeException("array is empty")
    }
    when (axis) {
        0 -> {
            if (index < 0 || index >= this.size) {
                throw IndexOutOfBoundsException("Row index $index is out of bounds")
            }
            return this[index].maxOrNull()!!
        }
        1 -> {
            if (index < 0 || index >= this[0].size) {
                throw IndexOutOfBoundsException("Column index $index is out of bounds")
            }
            return this.maxOf { row -> row[index] }
        }
        else -> {
            throw RuntimeException("Invalid axis value: ${axis}. Valid options: 0 or 1")
        }
    }
}

fun Array<FloatArray>.minValue(index: Int, axis: Int = 0): Float {
    if (this.isEmpty()) {
        throw RuntimeException("Array is empty")
    }
    when (axis) {
        0 -> {
            if (index < 0 || index >= this.size) {
                throw IndexOutOfBoundsException("Row index $index is out of bounds")
            }
            return this[index].minOrNull()!!
        }
        1 -> {
            if (index < 0 || index >= this[0].size) {
                throw IndexOutOfBoundsException("Column index $index is out of bounds")
            }
            return this.minOf { row -> row[index] }
        }
        else -> {
            throw RuntimeException("Invalid axis value: ${axis}. Valid options: 0 or 1")
        }
    }
}


fun Array<FloatArray>.minValue(): Float{
    if (this.isEmpty()){
        throw RuntimeException("array is empty")
    }

    var minValue = this[0][0]
    for (i in this.indices){
        for (j in this[i].indices){
            if (this[i][j] < minValue){
                minValue = this[i][j]
            }
        }
    }
    return minValue
}

/**
 * Performs min-max scaling on matrix
 *
 * @param tMin minimum range value
 * @param tMax maximum range value
 * @return
 * X = ((X - X.min(axis=0)) / (X.max(axis=0) - X.min(axis=0))) * (tMax - tMin) + tMin
 **/
fun Array<FloatArray>.minMaxScaleRespectColumns(tMin: Int = 0, tMax: Int = 1): Array<FloatArray>{

    val numRows = this.size
    val numCols = this[0].size

    val scaledArray: Array<FloatArray> = Array(numRows) { FloatArray(numCols) { 0f } }

    val colMins = mutableListOf<Float>()
    for (i in 0..this[0].size-1){
        colMins.add(this.minValue(index = i, axis = 1))
    }
    val colMaxs = mutableListOf<Float>()
    for (i in 0..this[0].size-1){
        colMaxs.add(this.maxValue(index = i, axis = 1))
    }

    var range: Float
    for (i in this.indices){
        for (j in this[i].indices){
            range = colMaxs[j] - colMins[j]
            if (range <= 0) {
                scaledArray[i][j] = tMin.toFloat()
            } else{
                scaledArray[i][j] = ((this[i][j] - colMins[j])/(colMaxs[j] - colMins[j])) * (tMax - tMin) + tMin
            }
        }
    }
    return scaledArray

}

fun Array<FloatArray>.minMaxScale(): Array<FloatArray>{

    val numRows = this.size
    val numCols = this[0].size
    val scaledArray: Array<FloatArray> = Array(numRows) { FloatArray(numCols) { 0f } }

    val max = this.maxValue()
    val min = this.minValue()
    val range = max - min

    for (i in this.indices){
        for (j in this[i].indices){
            scaledArray[i][j] = (this[i][j] - min)/range
        }
    }

    return scaledArray

}