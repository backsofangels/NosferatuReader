package com.nosferatu.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.ui.components.home.HomeReadingNowItem
import com.nosferatu.launcher.ui.components.home.TopActionBar
import com.nosferatu.launcher.ui.states.LibraryUiState

@Composable
fun HomeScreen(
    uiState: LibraryUiState,
    onOpenBook: (EbookEntity) -> Unit,
    onSyncClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        TopActionBar (
            isScanning = uiState.isScanning,
            onSyncClick = onSyncClick
        )
        // Sezione "In Lettura" (I due libri in alto nell'immagine)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            uiState.books.filter { it.lastLocationJson != null }.take(2).forEach { book ->
                HomeReadingNowItem(
                    book = book,
                    onClick = { onOpenBook(book) }
                )
            }
        }
    }
}