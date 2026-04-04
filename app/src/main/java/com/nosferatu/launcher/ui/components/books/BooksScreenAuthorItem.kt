package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import com.nosferatu.launcher.R
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun BooksScreenAuthorItem(
    author: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = author.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = LocalAppColors.current.onBg
        )
        Icon(
            painter = painterResource(
                id = if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            ),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = com.nosferatu.launcher.R.dimen.icon_small)),
            tint = LocalAppColors.current.onBg
        )
    }
}