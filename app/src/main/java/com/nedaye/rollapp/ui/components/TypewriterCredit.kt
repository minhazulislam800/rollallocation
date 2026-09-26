package com.nedaye.rollapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import com.nedaye.rollapp.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Home-screen credit line: reveals the text one character at a time like a
 * terminal typing, with a small blinking cursor block after it.
 */
@Composable
fun TypewriterCredit(
    fullText: String = "Developed by Muhammad Minhazul Islam",
    charDelayMs: Long = 55
) {
    val colors = LocalAppColors.current
    var shown by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        shown = ""
        for (i in fullText.indices) {
            shown = fullText.substring(0, i + 1)
            delay(charDelayMs)
        }
    }

    val infinite = rememberInfiniteTransition(label = "cursor-blink")
    val cursorAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "cursor-alpha"
    )

    Row {
        Text(
            text = shown,
            color = colors.inkSoft,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = "▌",
            color = colors.accent,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.alpha(cursorAlpha)
        )
    }
}
