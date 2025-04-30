package com.example.frontend_triptales.ui.theme.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AnimatedAppTitle(fullText: String, typingSpeedMillis: Long = 100L) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            delay(typingSpeedMillis)
        }
    }

    Text(
        text = displayedText,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
