package com.nosferatu.launcher.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Locator

/**
 * Minimal fragment used for instrumented tests to simulate navigator behavior.
 * Exposes a tiny API that mirrors parts of `EpubNavigatorFragment` used by the activity:
 * - `currentLocator` flow
 * - `submitPreferences(...)`
 * - `go(...)`, `goForward()`, `goBackward()`
 * - `setPositions(...)` to pre-populate a list of locators
 */
class TestNavigatorFragment : Fragment() {
    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator.asStateFlow()

    // Simple positions list to simulate pagination/positions
    private var positions: List<Locator> = emptyList()
    private var currentIndex: Int = 0

    // Last preferences submitted (for assertions)
    var lastSubmittedPreferences: EpubPreferences? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = View(inflater.context)
        v.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return v
    }

    /** Simulate navigator submitting new preferences. */
    fun submitPreferences(preferences: EpubPreferences) {
        lastSubmittedPreferences = preferences
    }

    /** Directly set the current locator. */
    fun setCurrentLocator(locator: Locator?) {
        _currentLocator.value = locator
    }

    /** Convenience: set current locator from a JSON string. */
    fun setCurrentLocatorFromJson(json: String) {
        try {
            val loc = Locator.fromJSON(JSONObject(json))
            _currentLocator.value = loc
        } catch (t: Throwable) {
            // ignore parse errors in tests
        }
    }

    /** Set a list of positions used by `goForward`/`goBackward`. */
    fun setPositions(list: List<Locator>) {
        positions = list
        currentIndex = 0
        _currentLocator.value = positions.getOrNull(0)
    }

    fun go(locator: Locator, animated: Boolean) {
        // Map to the closest index if positions exist
        val idx = positions.indexOfFirst { it == locator }
        if (idx >= 0) currentIndex = idx
        _currentLocator.value = locator
    }

    fun goForward(animated: Boolean) {
        if (positions.isEmpty()) return
        if (currentIndex < positions.lastIndex) currentIndex++
        _currentLocator.value = positions.getOrNull(currentIndex)
    }

    fun goBackward(animated: Boolean) {
        if (positions.isEmpty()) return
        if (currentIndex > 0) currentIndex--
        _currentLocator.value = positions.getOrNull(currentIndex)
    }
}

