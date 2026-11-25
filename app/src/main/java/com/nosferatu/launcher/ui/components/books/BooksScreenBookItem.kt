package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.dimensionResource
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
            .padding(dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(dimensionResource(id = com.nosferatu.launcher.R.dimen.cover_width))
                .aspectRatio(2f / 3f)
                .border(dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin), LocalAppColors.current.onBg.copy(alpha = 0.4f))
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = File(book.coverPath),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalAppColors.current.accent)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_md))
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LocalAppColors.current.onBg,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showAuthor) {
                if (book.author == null) {
                    Text(
                        text = stringResource(id = com.nosferatu.launcher.R.string.unknown_author),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.onBgMuted,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.onBgMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val isNew = book.lastLocationJson == null
            val isReading = !isNew
            Text(
                text = if (isNew) stringResource(id = com.nosferatu.launcher.R.string.status_unread) else stringResource(id = com.nosferatu.launcher.R.string.status_reading),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isReading) LocalAppColors.current.accent else LocalAppColors.current.onBg
            )
            Text(text = book.format, fontSize = 11.sp, color = LocalAppColors.current.onBgFaint)
        }
    }
}