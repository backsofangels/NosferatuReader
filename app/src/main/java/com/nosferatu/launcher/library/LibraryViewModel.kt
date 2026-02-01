package com.nosferatu.launcher.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nosferatu.launcher.repository.LibraryRepository
import com.nosferatu.launcher.ui.states.LibraryUiState
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.ScreenSelectionTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
): ViewModel() {
    private val _tag = "LibraryViewModel"
    private val _isScanning = MutableStateFlow(false)
    private val _hasPermission = MutableStateFlow(false)
    private val _booksFilterTab = MutableStateFlow<LibraryFilterTab>(LibraryFilterTab.All)
    private val _screenSelectionTab = MutableStateFlow<ScreenSelectionTab>(ScreenSelectionTab.Home)
    private val _error = MutableStateFlow<String?>(null)

    private val _filteredBooks = combine(
        repository.allBooks,
        _booksFilterTab,
        _hasPermission
    ) { books, filter, permission ->
        if (!permission) return@combine emptyList()

        when (filter) {
            LibraryFilterTab.All -> books.sortedByDescending { it.id }
            LibraryFilterTab.Authors -> books.groupBy { it.author }.values.flatten().sortedByDescending { it.id }
            //LibraryFilterTab.Series -> books.groupBy { it.series }.values.flatten().sortedByDescending { it.id }
            //LibraryFilterTab.Collections -> books
            else -> books
        }
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _filteredBooks,
        _isScanning,
        _hasPermission,
        _screenSelectionTab,
        _error
    ) { books, scanning, permission, screenSelectionTab, error ->

        LibraryUiState(
            books = books,
            isScanning = scanning,
            hasPermission = permission,
            screenSelectionTab = screenSelectionTab,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun selectScreenTab(tab: ScreenSelectionTab) {
        if (_screenSelectionTab.value == tab) return

        _screenSelectionTab.value = tab
        Log.d(_tag, "Switching to screen: ${tab.label}")

        if (tab == ScreenSelectionTab.MyBooks) {
            _booksFilterTab.value = LibraryFilterTab.All
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _hasPermission.value = isGranted
        if (isGranted) scanBooks()
    }

    fun saveBookPosition(bookId: Long, location: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBookPosition(bookId, location, 0.0)
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