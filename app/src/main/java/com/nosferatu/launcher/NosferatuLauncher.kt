package com.nosferatu.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nosferatu.launcher.data.AppDatabase
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.library.LibraryManager
import com.nosferatu.launcher.library.LibraryScanner
import com.nosferatu.launcher.parser.BookParser
import com.nosferatu.launcher.reader.native.ReaderActivityNative
import com.nosferatu.launcher.repository.LibraryRepository
import com.nosferatu.launcher.ui.adapter.BookAdapter
import kotlinx.coroutines.launch

class NosferatuLauncher : AppCompatActivity() {
    private val TAG = "NosferatuLauncher"

    private lateinit var libraryManager: LibraryManager
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var bookAdapter: BookAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView

    private lateinit var buttonSync: ImageButton
    private lateinit var textViewClock: TextView

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            findViewById<TextView>(R.id.textViewBattery).text = "$level%"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDependencies()
        setupView()
        setupRecyclerView()
        updateClock()

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            loadAndDisplayBooks()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        updateClock()
        loadAndDisplayBooks()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }



    private fun setupDependencies() {
        val database = AppDatabase.getDatabase(this)
        val bookDao = database.bookDao()

        val bookParser = BookParser()
        val scanner = LibraryScanner(bookParser)

        libraryRepository = LibraryRepository(this)
        libraryManager = LibraryManager(scanner, libraryRepository)
    }

    private fun setupView() {
        recyclerView = findViewById(R.id.recyclerViewBooks)
        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.textViewStatus)
        buttonSync = findViewById(R.id.buttonSync)
        textViewClock = findViewById(R.id.textViewClock)

        buttonSync.setOnClickListener {
            loadAndDisplayBooks()
        }
    }

    private fun updateClock() {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        textViewClock.text = sdf.format(java.util.Date())
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        bookAdapter = BookAdapter(emptyList()) { selectedBook ->
            openBook(selectedBook)
        }
        recyclerView.adapter = bookAdapter
    }

    private fun loadAndDisplayBooks() {
        lifecycleScope.launch {
            showLoading(true)

            libraryManager.syncLibrary()

            val allBooks = libraryRepository.getAllBooks()

            showLoading(false)
            updateUiWithBooks(allBooks)
        }
    }

    private fun updateUiWithBooks(books: List<EbookEntity>) {
        if (books.isEmpty()) {
            recyclerView.visibility = View.GONE
            statusTextView.visibility = View.VISIBLE
            statusTextView.text = "Library is empty"
        } else {
            recyclerView.visibility = View.VISIBLE
            statusTextView.visibility = View.GONE
            bookAdapter.updateBooks(books)
        }
    }

    private fun openBook(book: EbookEntity) {
        val intent = Intent(this, ReaderActivityNative::class.java).apply {
            putExtra("FILE_PATH", book.filePath)
        }
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            recyclerView.visibility = View.GONE
            statusTextView.visibility = View.GONE
        }
    }
}