package com.nosferatu.launcher.ui.components.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.library.LibraryFilterTab
import com.nosferatu.launcher.ui.states.LibraryUiState

@Composable
fun BooksScreenLibraryList(
    state: LibraryUiState,
    onOpenBook: (EbookEntity) -> Unit,
    onToggleAuthor: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        when (state.booksFilterTab) {
            LibraryFilterTab.AUTHORS -> {
                val groupedBooks = state.books.groupBy { it.author ?: "Autore Sconosciuto" }

                expandableGroup(
                    groups = groupedBooks,
                    expandedKeys = state.expandedAuthors,
                    onToggle = onToggleAuthor,
                    headerContent = { author, isExpanded ->
                        BooksScreenAuthorItem (
                            author = author,
                            isExpanded = isExpanded,
                            onToggle = { onToggleAuthor(author) }
                        )
                    },
                    itemContent = { book ->
                        BooksScreenBookItem(
                            book = book,
                            showAuthor = false,
                            onClick = { onOpenBook(book) }
                        )
                    }
                )
            }

            // TODO: Add further groups
            // LibraryFilterTab.FORMAT -> { ... } using same function expandableGroup

            else -> {
                items(state.books, key = { it.id }) { book ->
                    BooksScreenBookItem(
                        book = book,
                        showAuthor = true,
                        onClick = { onOpenBook(book) }
                    )
                    ListDivider()
                }
            }
        }
    }
}

// --- EXTENSIONS ---

/**
 * Handles the rendering logic for expandable groups (e.g., Authors, Genres, Formats).
 * Provides a reusable pattern for displaying headers with collapsible content within a LazyList.
 */
private fun <T> LazyListScope.expandableGroup(
    groups: Map<String, List<T>>,
    expandedKeys: Set<String>,
    onToggle: (String) -> Unit,
    headerContent: @Composable (String, Boolean) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    val groupEntries = groups.entries.toList()

    groupEntries.forEachIndexed { groupIndex, (key, items) ->

        item(key = "header_$key") {
            headerContent(key, expandedKeys.contains(key))
        }

        if (expandedKeys.contains(key)) {
            itemsIndexed(items) { itemIndex, item ->
                itemContent(item)
                if (itemIndex < items.size - 1) {
                    ListDivider()
                }
            }
        }

        if (groupIndex < groupEntries.size - 1) {
            item {
                ListDivider()
            }
        }
    }
}

@Composable
private fun ListDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center)
                .background(Color.Black),
            thickness = 0.5.dp
        )
    }
}