package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun BooksScreenFilterTabItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit) {
    ClickableText(
        text = AnnotatedString(label),
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Color.Black else Color.Gray
        ),
        onClick = { onClick() }
    )
}