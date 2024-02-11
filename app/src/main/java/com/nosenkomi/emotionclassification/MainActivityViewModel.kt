package com.nosenkomi.emotionclassification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nosenkomi.emotionclassification.classifier.ClassificationResult
import com.nosenkomi.emotionclassification.classifier.LSTMClassifier
import com.nosenkomi.emotionclassification.record.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.tensorflow.lite.support.label.Category
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val recorder: AudioRecorder,
    private val classifier: LSTMClassifier
) : ViewModel() {

    private val TAG = this::class.simpleName
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _categories = MutableStateFlow(emptyList<Category>())
    val categories = _categories.asStateFlow()

    private val _error = MutableStateFlow<String>("")
    val error = _error.asStateFlow()

    fun startRecording() {
        recorder.start()
    }

    fun stopRecording() {
        recorder.stop()
    }

    fun startClassification() {
        if (isRecording.value) return
        _isRecording.update { true }
        classifier.startAudioClassification()
            .onEach { result ->
                when (result) {
                    is ClassificationResult.Error -> {
                        _error.value = result.message ?: "Oops... Something went wrong!"
                        Log.e(TAG, error.value)
                    }

                    is ClassificationResult.Success -> {
                        _categories.value = result.data.orEmpty()
                        Log.d(TAG, categories.value.toString())

                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun stopClassification() {
        if (!isRecording.value) return

        _isRecording.update { false }
        _categories.update { emptyList() }
        viewModelScope.cancel()
        classifier.stop()
    }

    private fun processAudioInput() {
    }
}