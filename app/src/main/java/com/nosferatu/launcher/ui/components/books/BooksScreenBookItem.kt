package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nosferatu.launcher.data.EbookEntity
import java.io.File

@Composable
fun BooksScreenBookItem(book: EbookEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Box(
            modifier = Modifier.Companion
                .width(40.dp)
                .aspectRatio(0.75f)
                .border(0.5.dp, Color.Companion.Black)
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = File(book.coverPath),
                    contentDescription = null,
                    contentScale = ContentScale.Companion.Crop
                )
            }
        }

        Column(
            modifier = Modifier.Companion
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Companion.Bold),
                maxLines = 1,
                overflow = TextOverflow.Companion.Ellipsis
            )

            if (book.author == null) {
                Text(
                    text = "Autore Sconosciuto",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Companion.Gray
                )
            } else {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Companion.Gray
                )
            }
        }

        Column(horizontalAlignment = Alignment.Companion.End) {
            val isNew = book.lastLocationJson == null
            Text(
                text = if (isNew) "NON LETTO" else "IN LETTURA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Companion.ExtraBold,
                color = if (isNew) Color.Companion.Black else Color.Companion.DarkGray
            )
            Text(text = book.format, fontSize = 10.sp, color = Color.Companion.LightGray)
        }
    }
}