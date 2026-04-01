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
import androidx.compose.ui.res.stringResource
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
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun BooksScreenBookItem(
    book: EbookEntity,
    showAuthor: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(55.dp)
                .aspectRatio(0.75f)
                .border(0.5.dp, LocalAppColors.current.onBg.copy(alpha = 0.4f))
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = File(book.coverPath),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showAuthor) {
                if (book.author == null) {
                    Text(
                        text = stringResource(id = com.nosferatu.launcher.R.string.unknown_author),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val isNew = book.lastLocationJson == null
            Text(
                text = if (isNew) stringResource(id = com.nosferatu.launcher.R.string.status_unread) else stringResource(id = com.nosferatu.launcher.R.string.status_reading),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isNew) LocalAppColors.current.onBg else LocalAppColors.current.onBg.copy(alpha = 0.6f)
            )
            Text(text = book.format, fontSize = 10.sp, color = LocalAppColors.current.onBg.copy(alpha = 0.4f))
        }
    }
}