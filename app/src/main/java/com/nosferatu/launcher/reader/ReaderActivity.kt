package com.nosferatu.launcher.reader

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nosferatu.launcher.NosferatuApp
import com.nosferatu.launcher.R
import com.nosferatu.launcher.library.LibraryViewModel
import com.nosferatu.launcher.library.LibraryViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity(), EpubNavigatorFragment.Listener {

    private val _tag = "ReaderActivity"
    private var publication: Publication? = null
    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory((application as NosferatuApp).repository)
    }
    private var _lastKnownLocator: Locator? = null
    private var bookId: Long = -1

    private val assetRetriever by lazy { AssetRetriever(contentResolver, DefaultHttpClient()) }
    private val publicationOpener by lazy {
        PublicationOpener(
            publicationParser = EpubParser(),
            contentProtections = emptyList()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reader)

        val bookPath = intent.getStringExtra("BOOK_PATH") ?: return finish()
        val lastLocationJson = intent.getStringExtra("LAST_LOCATION_JSON")
        bookId = intent.getLongExtra("BOOK_ID", -1)

        Log.d(_tag, "bookPath: ${bookPath}, lastLocationJson ${lastLocationJson}, bookId $bookId")

        lifecycleScope.launch {
            try {
                publication = openPublication(File(bookPath))

                val initialLocator = lastLocationJson?.let {
                    try { Locator.Companion.fromJSON(JSONObject(it)) } catch (e: Exception) { null }
                }

                val navigatorFactory = EpubNavigatorFactory(
                    publication = publication!!,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = 1.0
                        )
                    )
                ).createFragmentFactory(
                    initialLocator = initialLocator,
                    listener = this@ReaderActivity
                )

                supportFragmentManager.fragmentFactory = navigatorFactory

                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.reader_container,
                            EpubNavigatorFragment::class.java,
                            null,
                            "EpubNavigator")
                        .commitNow()
                }

                val navigator = supportFragmentManager
                    .findFragmentByTag("EpubNavigator") as? EpubNavigatorFragment

                if (navigator != null) {
                    launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            navigator.currentLocator.collect { locator ->
                                _lastKnownLocator = locator
                                Log.d(_tag, "Posizione aggiornata: ${locator.locations.progression}")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(_tag, "Errore fatale: ${e.message}")
                finish()
            }
        }
    }

    private fun saveLocation(bookId: Long, locator: Locator?) {
        if (bookId == -1L) return
        val jsonLocation = locator?.toJSON().toString()
        viewModel.saveBookPosition(bookId, jsonLocation)
        Log.d(_tag, "Saved into DB")
    }

    private suspend fun openPublication(file: File): Publication = withContext(Dispatchers.IO) {
        val asset = assetRetriever.retrieve(file).getOrElse {
            throw Exception("Asset fallito: ${it.message}")
        }
        publicationOpener.open(asset, allowUserInteraction = true).getOrElse {
            throw Exception("Opener fallito: ${it.message}")
        }
    }

    override fun onJumpToLocator(locator: Locator) {
        _lastKnownLocator = locator
    }

    override fun onDestroy() {
        publication?.close()
        super.onDestroy()
    }

    override fun onStop() {
        //TODO: Check if can be moved after super call
        saveLocation(bookId, _lastKnownLocator)
        super.onStop()
    }

    @ExperimentalReadiumApi
    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        Log.v(_tag, "doNothing()")
    }
}