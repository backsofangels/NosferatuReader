package com.nosferatu.launcher.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nosferatu.launcher.repository.LibraryRepository
import com.nosferatu.launcher.ui.states.LibraryUiState
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.ScreenSelectionTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class LibraryViewModel(
    private val repository: LibraryRepository
): ViewModel() {
    private val _tag = "LibraryViewModel"
    private val _isScanning = MutableStateFlow(false)
    private val _hasPermission = MutableStateFlow(false)
    private val _booksFilterTab = MutableStateFlow(LibraryFilterTab.ALL)
    private val _screenSelectionTab = MutableStateFlow(ScreenSelectionTab.Home)
    private val _error = MutableStateFlow<String?>(null)
    private val _expandedAuthors = MutableStateFlow<Set<String>>(emptySet())

    private val _filteredBooks = combine(
        repository.allBooks,
        _booksFilterTab,
        _hasPermission
    ) { books, filter, permission ->
        if (!permission) return@combine emptyList()

        when (filter) {
            LibraryFilterTab.ALL -> books.sortedByDescending { it.id }
            LibraryFilterTab.AUTHORS -> books.sortedBy { it.author ?: "Sconosciuto" }
        }
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _filteredBooks,
        _isScanning,
        _hasPermission,
        _screenSelectionTab,
        _booksFilterTab,
        _expandedAuthors,
        _error
    ) { books, scanning, permission, screenTab, filterTab, expanded, error ->
        LibraryUiState(
            books = books,
            isScanning = scanning,
            hasPermission = permission,
            screenSelectionTab = screenTab,
            booksFilterTab = filterTab,
            expandedAuthors = expanded,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        // Use Eagerly so StateFlow starts collecting immediately in JVM unit tests
        // (avoids tests needing to subscribe to observe uiState updates).
        started = SharingStarted.Eagerly,
        initialValue = LibraryUiState()
    )

    fun toggleAuthorExpansion(author: String) {
        val current = _expandedAuthors.value
        _expandedAuthors.value = if (current.contains(author)) {
            current - author
        } else {
            current + author
        }
    }

    // Testing helpers: synchronous accessors for unit tests to avoid relying on flow collection timing
    fun getExpandedAuthorsForTests(): Set<String> = _expandedAuthors.value
    fun getScreenSelectionTabForTests(): ScreenSelectionTab = _screenSelectionTab.value

    fun onFilterChange(filter: LibraryFilterTab) {
        _booksFilterTab.value = filter
    }

    fun selectScreenTab(tab: ScreenSelectionTab) {
        if (_screenSelectionTab.value == tab) return

        _screenSelectionTab.value = tab
        // ViewModel should not access UI resources; log the enum name instead of a string resource
        Log.d(_tag, "Switching to screen: ${tab.name}")

        if (tab == ScreenSelectionTab.MyBooks) {
            _booksFilterTab.value = LibraryFilterTab.ALL
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _hasPermission.value = isGranted
        if (isGranted) scanBooks()
    }

    fun saveBookPosition(bookId: Long, location: String, progression: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBookPosition(bookId, location, progression)
        }
    }

    //To be used for debug of parser only, to be removed after
    fun wipeAndScanBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBooks()
            scanBooks()
        }
    }

    fun scanBooks() {
        Log.d(_tag, "Scanning books")

        if (_isScanning.value) {
            Log.d(_tag, "Already scanning")
            return
        }

        viewModelScope.launch {
            _isScanning.value = true

            try {
                repository.syncLibrary()
                Log.d(_tag, "Library synced successfully")
            } catch (e: Exception) {
                Log.e(_tag, "Error syncing library", e)
                _error.value = e.toString()
            } finally {
                _isScanning.value = false
            }
        }
    }
}

