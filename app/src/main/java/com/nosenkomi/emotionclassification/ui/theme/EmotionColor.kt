package com.nosenkomi.emotionclassification.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class EmotionColor(
    val default: Color = Color(233, 233, 233, 255),
    val anger: Color = Color(249, 87, 56),
    val sadness: Color = Color(47, 13, 102, 255),
    val fear: Color = Color(56, 172, 249, 255),
    val surprise: Color = Color(56, 249, 111, 255),
    val happiness: Color = Color(238, 150, 75),
    val neutral: Color = Color(250, 240, 202),
)

val LocalEmotionColor = compositionLocalOf { EmotionColor() }

val MaterialTheme.emotionColor: EmotionColor
    @Composable
    @ReadOnlyComposable
    get() = LocalEmotionColor.current