package com.nosferatu.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
// color handled via LocalAppColors
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.components.home.HomeReadingNowItem
import com.nosferatu.launcher.ui.components.home.HomeSection
import com.nosferatu.launcher.ui.components.home.TopActionBar
import com.nosferatu.launcher.ui.states.LibraryUiState
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun HomeScreen(
    uiState: LibraryUiState,
    onOpenBook: (EbookEntity) -> Unit,
    onSyncClick: () -> Unit
) {
    val readingNowItems = uiState.books
        .filter { it.lastLocationJson != null }
        .sortedByDescending { it.progression }
        .take(4)

    val bg = LocalAppColors.current.bg

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        item {
            TopActionBar(
                isScanning = uiState.isScanning,
                onSyncClick = onSyncClick
            )
        }

        if (readingNowItems.isNotEmpty()) {
            item {
                HomeSection(title = stringResource(id = com.nosferatu.launcher.R.string.in_reading)) {
                    // Render as simple 2-column grid using chunked rows so we don't nest lazy containers.
                    Column(modifier = Modifier.padding(horizontal = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16))) {
                        readingNowItems.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16))
                            ) {
                                row.forEach { book ->
                                    HomeReadingNowItem(
                                        book = book,
                                        onClick = { onOpenBook(book) }
                                    )
                                }
                                // single-item rows left as-is (no weight spacer to avoid internal API exposure)
                            }
                        }
                    }
                }
            }
        }
    }
}