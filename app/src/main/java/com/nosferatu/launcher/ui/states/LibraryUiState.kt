package com.nosferatu.launcher.ui.states

import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.ui.ScreenSelectionTab

data class LibraryUiState (
    val books: List<EbookEntity> = emptyList(),
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val screenSelectionTab: ScreenSelectionTab = ScreenSelectionTab.Home,
    val error: String? = null
)