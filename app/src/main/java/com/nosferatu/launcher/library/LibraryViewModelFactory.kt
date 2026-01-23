package com.nosferatu.launcher.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nosferatu.launcher.repository.LibraryRepository

class LibraryViewModelFactory(
    private val repository: LibraryRepository
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}