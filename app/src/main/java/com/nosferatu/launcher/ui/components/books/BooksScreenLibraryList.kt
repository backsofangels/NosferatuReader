package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.states.LibraryUiState

@Composable
fun BooksScreenLibraryList(state: LibraryUiState, onOpenBook: (EbookEntity) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.books) { book ->
            BooksScreenBookItem(book = book, onClick = { onOpenBook(book) })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.LightGray)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}