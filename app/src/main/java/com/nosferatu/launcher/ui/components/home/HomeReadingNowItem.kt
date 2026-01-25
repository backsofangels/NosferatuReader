package com.nosferatu.launcher.ui.components.home

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
import coil.compose.AsyncImage
import com.nosferatu.launcher.data.EbookEntity
import java.io.File

@Composable
fun HomeReadingNowItem(
    modifier: Modifier = Modifier,
    book: EbookEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // Contenitore Copertina
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f) // Leggermente più largo per e-ink
                .border(0.5.dp, Color.Black)
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
                    color = Color.Black
                )
            }
        }

        // Dettagli sotto la copertina stile Kobo
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10% letto", // Placeholder percentuale
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}