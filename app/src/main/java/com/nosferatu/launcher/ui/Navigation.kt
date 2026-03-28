package com.nosferatu.launcher.ui

import com.nosferatu.launcher.R

enum class ScreenSelectionTab(val labelRes: Int, val iconRes: Int) {
    Home(com.nosferatu.launcher.R.string.tab_home, R.drawable.ic_home),
    MyBooks(com.nosferatu.launcher.R.string.tab_my_books, R.drawable.ic_books),
    More(com.nosferatu.launcher.R.string.tab_more, R.drawable.ic_more);

    companion object {
        val all = entries
    }
}