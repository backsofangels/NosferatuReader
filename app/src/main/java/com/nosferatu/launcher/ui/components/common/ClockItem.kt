package com.nosferatu.launcher.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockItem() {
    val currentTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000)
        }
    }
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Text(
        text = sdf.format(Date(currentTime)),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
    )
}