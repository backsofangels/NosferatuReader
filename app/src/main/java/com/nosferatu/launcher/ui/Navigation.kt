package com.nosferatu.launcher.ui

import com.nosferatu.launcher.R

enum class ScreenSelectionTab(val label: String, val iconRes: Int) {
    Home("Home", R.drawable.ic_home),
    MyBooks("Miei Libri", R.drawable.ic_books),
    More("Altro", R.drawable.ic_more);

    companion object {
        val all = entries
    }
}