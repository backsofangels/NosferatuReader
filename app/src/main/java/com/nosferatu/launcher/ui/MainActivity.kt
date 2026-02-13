package com.nosferatu.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.nosferatu.launcher.ui.components.common.CustomStatusBar
import com.nosferatu.launcher.ui.screens.HomeScreen
import androidx.core.net.toUri
import com.nosferatu.launcher.library.LibraryFilterTab

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
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    topBar = { CustomStatusBar() },
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
                            ScreenSelectionTab.Home -> HomeScreen(
                                uiState,
                                onOpenBook = { openBook(it) },
                                onSyncClick = { viewModel.scanBooks() }
                            )
                            ScreenSelectionTab.MyBooks -> {
                                Column {
                                    BooksFilterBar(
                                        uiState = uiState,
                                        filter = uiState.booksFilterTab,
                                        onFilterChange = { newFilter ->
                                            viewModel.onFilterChange(newFilter)
                                        }
                                    )
                                    BooksScreenLibraryList(
                                        uiState,
                                        onOpenBook = { openBook(it) },
                                        onToggleAuthor = { author ->
                                            viewModel.toggleAuthorExpansion(author)
                                        }
                                    )
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
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ logic
            Environment.isExternalStorageManager()
        } else {
            // Android 10 and below logic
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }

        viewModel.onPermissionResult(isGranted)

        if (!isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Send user to the system settings page for your app
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:${packageName}".toUri()
                    }
                    // Use a new launcher for result (see step 2)
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            } else {
                // Traditional popup
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // When user returns from settings, re-run the check
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.onPermissionResult(isGranted)
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