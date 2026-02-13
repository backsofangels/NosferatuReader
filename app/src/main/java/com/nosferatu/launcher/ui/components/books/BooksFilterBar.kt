package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nosferatu.launcher.library.LibraryFilterTab
import com.nosferatu.launcher.ui.states.LibraryUiState

@Composable
fun BooksFilterBar(
    uiState: LibraryUiState,
    filter: LibraryFilterTab,
    onFilterChange: (LibraryFilterTab) -> Unit
) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BooksScreenFilterTabItem("Libri", filter == LibraryFilterTab.ALL, onClick = { onFilterChange(LibraryFilterTab.ALL) })
            BooksScreenFilterTabItem("Autori", filter == LibraryFilterTab.AUTHORS, onClick = { onFilterChange(LibraryFilterTab.AUTHORS) })
            //TODO: add series and collections
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
    }
}