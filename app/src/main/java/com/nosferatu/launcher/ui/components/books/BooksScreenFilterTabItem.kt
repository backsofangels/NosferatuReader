package com.nosferatu.launcher.ui.components.books

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun BooksScreenFilterTabItem(label: String, active: Boolean) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = if (active) FontWeight.Companion.Bold else FontWeight.Companion.Normal,
        color = if (active) Color.Companion.Black else Color.Companion.Gray
    )
}