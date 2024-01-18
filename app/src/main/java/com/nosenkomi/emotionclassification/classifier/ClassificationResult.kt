package com.nosenkomi.emotionclassification.classifier

sealed class ClassificationResult<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : ClassificationResult<T>(data)
    class Error<T>(message: String, data: T? = null) : ClassificationResult<T>(data, message)
}
