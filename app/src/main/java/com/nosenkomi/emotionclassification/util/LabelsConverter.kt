package com.nosenkomi.emotionclassification.util

import android.content.Context
import com.nosenkomi.emotionclassification.R
import org.tensorflow.lite.support.label.Category

sealed class Emotion(val label: String) {
    data class Anger(private val score: Float) : Emotion("anger") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.agner_label)

        override fun getScore() = score
    }

    data class Contempt(private val score: Float) : Emotion("contempt") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.contempt_label)

        override fun getScore() = score

    }

    data class Neutral(private val score: Float) : Emotion("neutral") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.neutral_label)

        override fun getScore() = score

    }

    data class Happiness(private val score: Float) : Emotion("happiness") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.happiness_label)

        override fun getScore() = score

    }

    data class Sadness(private val score: Float) : Emotion("sadness") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.sadness_label)

        override fun getScore() = score

    }

    data class Fear(private val score: Float) : Emotion("fear") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.fear_label)

        override fun getScore() = score

    }

    data class Surprise(private val score: Float) : Emotion("surprise") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.surprise_label)

        override fun getScore() = score

    }

    data class Silence(private val score: Float) : Emotion("Silence") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.silence_label)

        override fun getScore() = score

    }

    data class Unidentified(private val score: Float) : Emotion("unidentified") {
        override fun getLocalizedDisplayName(context: Context) =
            context.getString(R.string.cant_identify_label)

        override fun getScore() = score
    }

    abstract fun getLocalizedDisplayName(context: Context): String
    abstract fun getScore(): Float
}

fun Category.toEmotion(): Emotion {
    return when (this.label) {
        "anger" -> Emotion.Anger(this.score)
        "contempt" -> Emotion.Contempt(this.score)
        "neutral" -> Emotion.Neutral(this.score)
        "happiness" -> Emotion.Happiness(this.score)
        "sadness" -> Emotion.Sadness(this.score)
        "fear" -> Emotion.Fear(this.score)
        "surprise" -> Emotion.Surprise(this.score)
        "Silence" -> Emotion.Silence(this.score)
        else -> {
            Emotion.Unidentified(this.score)
        }
    }
}