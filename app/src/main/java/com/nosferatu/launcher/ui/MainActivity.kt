package com.nosferatu.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nosferatu.launcher.NosferatuApp
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.library.LibraryViewModelFactory
import com.nosferatu.launcher.reader.ReaderActivity
import com.nosferatu.launcher.ui.components.books.BooksFilterBar
import com.nosferatu.launcher.ui.components.books.BooksScreenLibraryList
import com.nosferatu.launcher.ui.components.common.BottomBar
import com.nosferatu.launcher.ui.components.common.TopBar
import com.nosferatu.launcher.ui.screens.BooksScreen
import com.nosferatu.launcher.ui.screens.HomeScreen

class MainActivity: AppCompatActivity() {
    private val _tag: String = "MainActivity"
    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory((application as NosferatuApp).repository)
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()


        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                checkAndRequestPermissions()
            }

            MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                Scaffold(
                    topBar = { TopBar(viewModel, uiState.isScanning) }, // La tua TopBar
                    bottomBar = {
                        BottomBar(
                            selectedTab = uiState.screenSelectionTab,
                            onTabSelected = {
                                tab -> viewModel.selectScreenTab(tab)
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (uiState.screenSelectionTab) {
                            ScreenSelectionTab.Home -> HomeScreen(uiState, onOpenBook = { openBook(it) })
                            ScreenSelectionTab.MyBooks -> {
                                Column {
                                    BooksFilterBar(uiState)
                                    BooksScreenLibraryList(uiState, onOpenBook = { openBook(it) })
                                }
                            }
                            ScreenSelectionTab.More -> Text("Impostazioni")
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val isGranted = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

        viewModel.onPermissionResult(isGranted)

        if (!isGranted) {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    private fun openBook(book: EbookEntity) {
        val intent = Intent(this, ReaderActivity::class.java).apply {
            putExtra("BOOK_PATH", book.filePath)
            putExtra("LAST_LOCATION_JSON", book.lastLocationJson)
            putExtra("BOOK_ID", book.id)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}