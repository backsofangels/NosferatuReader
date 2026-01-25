package com.nosferatu.launcher.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.ui.components.books.BooksScreenLibraryList
import com.nosferatu.launcher.ui.components.common.TopBar

@Composable
fun BooksScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (EbookEntity) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tag = "HomeScreen"

    Scaffold(
        topBar = { TopBar(viewModel, uiState.isScanning) },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isScanning) {
                Log.d(tag, "Scanning library")
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            } else {
                BooksScreenLibraryList(state = uiState, onOpenBook = { onOpenBook(it) })
            }
        }
    }
}