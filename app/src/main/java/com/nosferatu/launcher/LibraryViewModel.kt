package com.nosferatu.launcher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nosferatu.launcher.repository.LibraryRepository
import com.nosferatu.launcher.ui.LibraryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
): ViewModel() {
    private val _tag = "LibraryViewModel"
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        observeBooks()
    }

    private fun observeBooks() {
        viewModelScope.launch {
            repository.allBooks.collect {
                _uiState.update { state ->
                    Log.d(_tag, "Got ${state.books.size} books from db")
                    state.copy(books = it)
                }
            }
        }
    }

    fun onPermissionGranted() {
        if (_uiState.value.books.isEmpty()) {
            scanBooks()
        }
    }

    fun scanBooks() {
        Log.d(_tag, "Scanning books")

        if (_uiState.value.isScanning) {
            Log.d(_tag, "Already scanning")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isScanning = true)
            }
            try {
                repository.syncLibrary()
                Log.d(_tag, "Library synced successfully")
            } catch (e: Exception) {
                Log.e(_tag, "Error syncing library", e)
            } finally {
                _uiState.update {
                    it.copy(isScanning = false)
                }
            }
        }
    }
}