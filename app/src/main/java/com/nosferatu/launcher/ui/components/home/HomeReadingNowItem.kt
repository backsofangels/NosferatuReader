package com.nosferatu.launcher.ui.components.home

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.LocalAppColors
import org.json.JSONObject
import java.io.File

@SuppressLint("DefaultLocale")
@Composable
fun HomeReadingNowItem(
    modifier: Modifier = Modifier,
    book: EbookEntity,
    onClick: () -> Unit
) {
    fun formatPercentage(value: Double): String {
        return if (value % 1 == 0.0) {
            String.format(java.util.Locale.US, "%.0f", value)
        } else {
            String.format(java.util.Locale.US, "%.1f", value)
        }
    }

    val bookPercentage = book.lastLocationJson?.let { jsonString ->
        try {
            JSONObject(jsonString).optJSONObject("locations")?.let { locations ->
                val total = locations.optDouble("totalProgression", Double.NaN)
                val local = locations.optDouble("progression", Double.NaN)
                val normalized = when {
                    !total.isNaN() -> total
                    !local.isNaN() -> local
                    else -> book.progression
                }.coerceIn(0.0, 1.0)
                formatPercentage(normalized * 100.0)
            } ?: formatPercentage(book.progression.coerceIn(0.0, 1.0) * 100.0)
        } catch (_: Exception) {
            formatPercentage(book.progression.coerceIn(0.0, 1.0) * 100.0)
        }
    } ?: formatPercentage(book.progression.coerceIn(0.0, 1.0) * 100.0)

    Column(
        modifier = modifier
            .width(100.dp)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        val colors = LocalAppColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .border(1.dp, colors.onBg.copy(alpha = 0.5f))
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = File(book.coverPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = book.title,
                    modifier = Modifier.padding(12.dp).align(Alignment.Center),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = colors.onBg
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = com.nosferatu.launcher.R.string.read_percent_format, bookPercentage),
                fontSize = 12.sp,
                color = colors.onBg,
                fontWeight = FontWeight.Medium
            )
        }
    }
}