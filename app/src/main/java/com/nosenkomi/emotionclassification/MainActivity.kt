package com.nosenkomi.emotionclassification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.nosenkomi.emotionclassification.ui.theme.EmotionClassificationTheme
import com.nosenkomi.emotionclassification.ui.theme.emotionColor
import com.nosenkomi.emotionclassification.util.formatTime
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )
        setContent {
            val context = LocalContext.current
            val timerValue by viewModel.timer.collectAsState()
            val permissionDialogVisible by viewModel.permissionDialogVisible.collectAsState()
            val recordPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewModel.startClassification()
                } else {
                    viewModel.updatePermissionDialogVisibility(true)
                }
            }
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
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (permissionDialogVisible) {
                                AlertDialog(
                                    dialogTitle = getString(R.string.permission_is_required_text),
                                    dialogText = getString(R.string.audio_recording_permission_dialog),
                                    icon = Icons.Default.Info,
                                    dismissText = stringResource(id = R.string.dismiss_btn),
                                    confirmText = stringResource(id = R.string.go_to_settings_btn),
                                    onDismissRequest = {
                                        viewModel.updatePermissionDialogVisibility(
                                            false
                                        )
                                    },
                                    onConfirmation = {
                                        openAppSettings(context)
                                        viewModel.updatePermissionDialogVisibility(false)
                                    },
                                )
                            }
                            if (categories.value.isNotEmpty()) {
                                LazyColumn {
                                    items(categories.value) { category ->
                                        with(category) {
                                            CategoryItem(
                                                label = label,
                                                displayName = getLocalizedDisplayName(context),
                                                score = getScore(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(32.dp))
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
                                        when (recordPermissionState.status) {
                                            PermissionStatus.Granted -> {
                                                viewModel.startClassification()
                                            }

                                            else -> {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }) {
                                    Text(text = context.getString(R.string.start_btn))
                                }
                                Spacer(Modifier.width(16.dp))
                                Button(
                                    onClick = { viewModel.stopClassification() }) {
                                    Text(text = context.getString(R.string.stop_btn))
                                }
                            }
                            Spacer(Modifier.height(32.dp))
//                            Waveform(
//                                isRecording = isRecording,
//                                activeColor = MaterialTheme.colorScheme.primary,
//                                inactiveColor = MaterialTheme.colorScheme.secondary
//                            )
                        }

                    }
                }
            }
        }
    }


}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

@Composable
private fun TimerWidget(
    timeValue: Long,
) {
    Text(text = timeValue.formatTimeShortStyle(), fontSize = 20.sp)
}


@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    barWidth: Float = 8f,
    gapWidth: Float = 10f,
    maxBars: Int = 20,
    isRecording: State<Boolean> = mutableStateOf(true),
    activeColor: Color = Color.Black,
) {

    val infiniteAnimation = rememberInfiniteTransition(label = "barsInfiniteAnimation")
    val animations = mutableListOf<State<Float>>()
    val random = remember { Random(System.currentTimeMillis()) }

    val heightDivider by animateFloatAsState(
        targetValue = if (isRecording.value) 1f else 6f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "heightDivider"
    )

    repeat(maxBars) {
        val durationMillis = random.nextInt(500, 2000)
        animations += infiniteAnimation.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis),
                repeatMode = RepeatMode.Reverse,
            ), label = ""
        )
    }

    val initialMultipliers = remember {
        mutableListOf<Float>().apply {
            repeat(maxBars) { this += random.nextFloat() }
        }
    }

    Canvas(
        modifier = modifier
            .height(78.dp)
            .fillMaxWidth()
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val canvasCenterY: Float = canvasHeight / 2
        val count = (canvasWidth / (barWidth + gapWidth)).toInt().coerceAtMost(maxBars)

        val animatedVolumeWidth = count * (barWidth + gapWidth)
        var startOffset = (canvasWidth - animatedVolumeWidth) / 2

        val barMinHeight = 0f
        val barMaxHeight = canvasHeight / 2f / heightDivider

        repeat(count) { index ->
            val currentSize = animations[index % animations.size].value
            var barHeightPercent = initialMultipliers[index] + currentSize
            if (barHeightPercent > 1.0f) {
                val diff = barHeightPercent - 1.0f
                barHeightPercent = 1.0f - diff
            }
            val barHeight = MathUtils.lerp(barMinHeight, barMaxHeight, barHeightPercent)
            drawLine(
                color = activeColor,
                start = Offset(startOffset, canvasCenterY - barHeight / 2),
                end = Offset(startOffset, canvasCenterY + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
            startOffset += barWidth + gapWidth
        }


    }
}

@Preview
@Composable
fun CategoryItem(
    modifier: Modifier = Modifier,
    label: String = "label",
    displayName: String = "displayname",
    score: Float = 0.5f,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = displayName, fontSize = 30.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "%.2f".format(score), fontSize = 18.sp, fontStyle = FontStyle.Italic)
    }
}

