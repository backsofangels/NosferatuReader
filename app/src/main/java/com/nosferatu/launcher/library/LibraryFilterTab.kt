package com.nosferatu.launcher.library

sealed class LibraryFilterTab(val label: String) {
    object All : LibraryFilterTab("Libri")
    object Authors : LibraryFilterTab("Autori")
    object Series : LibraryFilterTab("Serie")
    object Collections : LibraryFilterTab("Raccolte")

    companion object {
        val all = listOf(All, Authors, Series, Collections)
    }
}