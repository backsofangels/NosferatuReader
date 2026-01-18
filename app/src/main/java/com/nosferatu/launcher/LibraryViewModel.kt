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
        scanBooks()
        observeBooks()
        _uiState.value.books.forEach {
            Log.d(_tag, "Book: ${it.title}")
        }
    }

    private fun observeBooks() {
        viewModelScope.launch {
            repository.allBooks.collect {
                _uiState.update { state ->
                    state.copy(books = it)
                }
            }
        }
    }

    fun scanBooks() {
        if (_uiState.value.isScanning) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isScanning = true)
            }

            repository.syncLibrary()

            _uiState.update {
                it.copy(isScanning = false)
            }
        }
    }
}