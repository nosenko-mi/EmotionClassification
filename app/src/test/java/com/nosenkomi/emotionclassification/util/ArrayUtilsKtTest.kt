package com.nosenkomi.emotionclassification.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ArrayUtilsKtTest {

    @Test
    fun `replace should change array inplace`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)
        val replacement = floatArrayOf(11f, 22f)
        input.replace(0, replacement)
        val expected = floatArrayOf(11f, 22f, 3f, 4f, 5f)
        assertThat(input).usingTolerance(0.001).containsExactly(expected)
    }

    @Test
    fun `replace should not have index out of bounds for input array`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)
        val replacement = floatArrayOf(11f, 22f)
        input.replace(4, replacement)
        val expected = floatArrayOf(1f, 2f, 3f, 4f, 11f)
        assertThat(input).usingTolerance(0.001).containsExactly(expected)
    }
}