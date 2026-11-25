package com.nosferatu.launcher.ui

import com.nosferatu.launcher.R

enum class ScreenSelectionTab(val labelRes: Int, val iconRes: Int) {
    Home(R.string.tab_home, R.drawable.ic_home),
    MyBooks(R.string.tab_library, R.drawable.ic_books),
    More(R.string.tab_settings, R.drawable.ic_settings_icon);

    companion object {
        val all = listOf(Home, MyBooks, More)
    }
}