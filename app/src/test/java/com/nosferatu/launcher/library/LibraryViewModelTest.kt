package com.nosferatu.launcher.library

import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import kotlinx.coroutines.flow.MutableStateFlow
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.repository.LibraryRepository
import com.nosferatu.launcher.ui.ScreenSelectionTab
import com.nosferatu.launcher.data.BookDao
import com.nosferatu.launcher.library.CoverManager
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.LibraryScanner

class LibraryViewModelTest {
    @Test
    fun toggleAuthorExpansion_addsAndRemoves() {
        val bookDao: BookDao = mock()
        whenever(bookDao.getAllBooksFlow()).thenReturn(MutableStateFlow(emptyList()))
        val scanner: LibraryScanner = mock()
        val coverManager: CoverManager = mock()
        val libraryConfig: LibraryConfig = mock()
        val repo = LibraryRepository(bookDao, scanner, coverManager, libraryConfig)
        val vm = LibraryViewModel(repo)

        assertTrue(vm.getExpandedAuthorsForTests().isEmpty())
        vm.toggleAuthorExpansion("Autore")
        assertTrue(vm.getExpandedAuthorsForTests().contains("Autore"))
        vm.toggleAuthorExpansion("Autore")
        assertFalse(vm.getExpandedAuthorsForTests().contains("Autore"))
    }

    @Test
    fun selectScreenTab_setsMyBooks_resetsFilter() {
        val bookDao: BookDao = mock()
        whenever(bookDao.getAllBooksFlow()).thenReturn(MutableStateFlow(emptyList()))
        val scanner: LibraryScanner = mock()
        val coverManager: CoverManager = mock()
        val libraryConfig: LibraryConfig = mock()
        val repo = LibraryRepository(bookDao, scanner, coverManager, libraryConfig)
        val vm = LibraryViewModel(repo)

        vm.selectScreenTab(ScreenSelectionTab.MyBooks)
        assertEquals(ScreenSelectionTab.MyBooks, vm.getScreenSelectionTabForTests())
    }
}
