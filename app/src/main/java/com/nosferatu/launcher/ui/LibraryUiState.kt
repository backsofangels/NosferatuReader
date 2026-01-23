package com.nosferatu.launcher.ui

import com.nosferatu.launcher.data.EbookEntity

data class LibraryUiState (
    val books: List<EbookEntity> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null
)