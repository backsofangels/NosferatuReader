package com.nosferatu.launcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.library.LibraryViewModelFactory
import com.nosferatu.launcher.reader.ReaderActivity
import com.nosferatu.launcher.ui.LibraryUiState
import java.io.File

class MainActivity: AppCompatActivity() {
    private val _tag: String = "MainActivity"
    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory((application as NosferatuApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                MainScreen(viewModel)
            }
        }
    }

    @Composable
    fun MainScreen(viewModel: LibraryViewModel) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        var hasPermission by remember {
            mutableStateOf(
                context.checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            )
        }

        val permissionRequestor = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            isGranted ->
                hasPermission = isGranted
                if (isGranted) Log.d(_tag, "Permission granted")
        }


        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                Log.d(_tag, "Refreshing library since permission is present")
                viewModel.scanBooks()
            } else {
                permissionRequestor.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        Scaffold(
            topBar = { NosferatuTopBar() },
            containerColor = Color.White
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (uiState.isScanning) {
                    Log.d(_tag, "Scanning library")
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                } else {
                    LibraryGrid(state = uiState, onOpenBook = { book ->
                        val intent = Intent(this@MainActivity, ReaderActivity::class.java).apply {
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
                    })
                }
            }
        }
    }

    @Composable
    fun LibraryGrid(state: LibraryUiState, onOpenBook: (EbookEntity) -> Unit) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            items(state.books) { book ->
                Log.d(_tag, "Rendering book: ${book.title}")
                BookItem(book = book, onClick = { onOpenBook(book) })
            }
        }
    }

    @Composable
    fun NosferatuTopBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BatteryStatus()
            ClockStatus()
        }
    }

    @Composable
    fun ClockStatus() {
        val currentTime by produceState(initialValue = System.currentTimeMillis()) {
            while (true) {
                value = System.currentTimeMillis()
                delay(60_000) // Updated every minute
            }
        }

        val sdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
        Text(text = sdf.format(java.util.Date(currentTime)), style = MaterialTheme.typography.bodyMedium)
    }

    @Composable
    fun BatteryStatus() {
        val context = LocalContext.current
        var batteryLevel by remember { mutableIntStateOf(0) }

        DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    batteryLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                }
            }
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        Text(text = "$batteryLevel%", style = MaterialTheme.typography.bodyMedium)
    }

    @Composable
    fun BookItem(book: EbookEntity, onClick: () -> Unit) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.66f)
                    .border(1.dp, Color.Black)
                    .background(Color.White)
            ) {
                if (book.coverPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(book.coverPath))
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Book title placeholder for the image
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}