package com.nosenkomi.emotionclassification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jlibrosa.audio.JLibrosa
import com.nosenkomi.emotionclassification.classifier.AudioClassificationListener
import com.nosenkomi.emotionclassification.ui.theme.EmotionClassificationTheme
import com.nosenkomi.emotionclassification.util.WAVReader
import com.nosenkomi.emotionclassification.util.formatTime
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.support.label.Category
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.random.Random


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    //    private lateinit var yamnetClassifier: YamnetClassifier
    private val audioClassificationListener = object : AudioClassificationListener {
        override fun onError(error: String) {
//            requireActivity().runOnUiThread {
//                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
//                adapter.categoryList = emptyList()
//                adapter.notifyDataSetChanged()
//            }
        }

        override fun onResult(results: List<Category>, inferenceTime: Long) {
//            requireActivity().runOnUiThread {
//                adapter.categoryList = results
//                adapter.notifyDataSetChanged()
//                fragmentAudioBinding.bottomSheetLayout.inferenceTimeVal.text =
//                    String.format("%d ms", inferenceTime)
//            }
        }
    }

    fun getMelSpectrogram() {
//        val audioFile = R.raw.f_ans001aes

        val jlibrosa = JLibrosa()

//        val audioData = jlibrosa.loadAndRead()
    }

    fun assetFilePath(context: Context, assetName: String): String {
//        val file = File(context.filesDir, assetName)
//        Log.d("mainactivity", "file $file")
//        Log.d("mainactivity", "exists ${file.exists()}")
//        Log.d("mainactivity", "length ${file.length()}")
//
//        if (file.exists() && file.length() > 0) {
//            return file.absolutePath
//        }
//        return ""
        val assets = context.assets
        val inputStream: InputStream = assets.open(assetName)

        // Create a temporary file to store the asset
        val tempFile = File(context.filesDir, assetName)
        tempFile.createNewFile()

        // Copy the asset to the temporary file
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()

        Log.d("mainactivity", "file path: ${tempFile.absolutePath}")
        val jlibrosa = JLibrosa()

        try {
            val data = jlibrosa.loadAndRead(tempFile.absolutePath, 22050, 2)
            Log.d("mainactivity", "file path: ${data.size}")
        } catch (e: Exception) {
            Log.e("mainactivity", "cannot read file: ${e.printStackTrace()}")
        }

        return tempFile.absolutePath
    }

    fun readWav(context: Context, assetName: String) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            0
        )

        val reader = WAVReader(applicationContext)
        reader.readAsset("f_ans001aes.wav")
        val data = reader.readAudioFile("f_ans001aes.wav")

//        yamnetClassifier = YamnetClassifier(
//            this.applicationContext,
////            audioClassificationListener
//        )
        setContent {
            val context = LocalContext.current
            val timerValue by viewModel.timer.collectAsState()
            EmotionClassificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val categories = viewModel.categories.collectAsState()
                        val error = viewModel.error.collectAsState()
                        val isRecording = viewModel.isRecording.collectAsState()
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (categories.value.isNotEmpty()) {
                                LazyColumn() {
                                    items(categories.value) { category ->
                                        with(category) {
                                            CategoryItem(
                                                label = label,
                                                displayName = displayName,
                                                score = score,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (error.value.isNotBlank()) {
                                Text(
                                    text = error.value,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                )
                            }
                            Spacer(Modifier.height(32.dp))
                            TimerWidget(timerValue)
                            Spacer(Modifier.height(32.dp))
                            Row {
                                Button(
                                    onClick = {
                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) -> {
                                                viewModel.startClassification()
                                            }
                                            else -> {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }) {
                                    Text(text = "Start")
                                }
                                Spacer(Modifier.width(16.dp))
                                Button(
                                    onClick = { viewModel.stopClassification() }) {
                                    Text(text = "Stop")
                                }
                            }
                            Spacer(Modifier.height(32.dp))
//                            Waveform(barWidth = 6f, gapWidth = 8f, maxLines = 96, isRecording = isRecording,activeColor = MaterialTheme.colorScheme.primary)
                        }

                    }
                }
            }
        }
    }


}

@Composable
private fun TimerWidget(
    timeValue: Long,
) {
    Text(text = timeValue.formatTime(), fontSize = 24.sp)
}


@Composable
fun Waveform(
    barWidth: Float,
    gapWidth: Float,
    maxLines: Int,
    isRecording: State<Boolean>,
    activeColor: Color = Color.Black,
    inactiveColor: Color = Color.Gray,
) {

    val infiniteTransition = rememberInfiniteTransition(label = "waveformAnimation")
    val dx by infiniteTransition.animateValue(
        initialValue = 0f,
        targetValue = gapWidth,
        typeConverter = Float.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    val heightDivider by animateFloatAsState(
        targetValue = if (isRecording.value) 1f else 6f,
        animationSpec = tween(1000, easing = LinearEasing), label = "heightDivider"
    )

    Canvas(
        modifier = Modifier
            .height(78.dp)
            .fillMaxWidth()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val canvasCenterY: Float = canvasHeight / 2
        val canvasCenterX: Float = canvasWidth / 2

        val count: Int = (canvasWidth / (barWidth + gapWidth)).toInt().coerceAtMost(maxLines)

        val animatedVolumeWidth = count * (barWidth + gapWidth)
        var startOffset: Float = (canvasWidth - animatedVolumeWidth) / 2


        val barMinHeight = barWidth
        val barMaxHeight = canvasHeight / 2f / heightDivider
        var volume = barMinHeight
        repeat(count) {
            volume = Random.nextInt(barMinHeight.toInt(), (canvasHeight / 2).toInt()).toFloat()
            if (startOffset < canvasCenterX) {
                drawLine(
                    start = Offset(x = startOffset + dx, y = canvasCenterY - volume / 2),
                    end = Offset(x = startOffset + dx, y = canvasCenterY + volume / 2),
                    color = activeColor,
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            } else {
                drawLine(
                    start = Offset(x = startOffset, y = canvasCenterY - barMinHeight / 2),
                    end = Offset(x = startOffset, y = canvasCenterY + barMinHeight / 2),
                    color = inactiveColor,
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }

            startOffset += barWidth + gapWidth
        }
    }
}

@Composable
fun CategoryItem(
    label: String,
    displayName: String,
    score: Float,
    modifier: Modifier = Modifier
) {
    Row {
        Text(text = "$label | $displayName | $score")
    }
}

fun normalize(value: Float, min: Float, max: Float): Float {
//    zi = (xi – min(x)) / (max(x) – min(x))
    return (value - min) / (max - min)
}
