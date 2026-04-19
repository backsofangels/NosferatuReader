package com.nosferatu.launcher.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.content.DialogInterface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nosferatu.launcher.R
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.library.ReaderConfig
import com.nosferatu.launcher.ui.LocalAppColors
import com.nosferatu.launcher.ui.appColorsFor
import com.nosferatu.launcher.ui.components.fontsettings.ReaderTextSettings
import com.nosferatu.launcher.ui.components.common.SingleChoiceOptionRow

class ReaderSettingsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.reader_settings_popup, container, false)
        val compose = root.findViewById<ComposeView>(R.id.reader_settings_compose)

        // Safe composition strategy
        try { compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow) } catch (_: Exception) {}

        compose.setContent {
            val act = activity as? ReaderActivity
            val ctx = requireContext()
            val libraryConfig = act?.provideLibraryConfig() ?: LibraryConfig(ctx)
            val readerConfig = act?.provideReaderConfig() ?: ReaderConfig(ctx)

            val selectionBgMode = if (readerConfig.readingBackgroundMode >= 0f) readerConfig.readingBackgroundMode.toInt() else libraryConfig.backgroundMode.toInt()
            val appColors = appColorsFor(libraryConfig.backgroundMode)
            val isDark = libraryConfig.backgroundMode.toInt() == 2
            val colorScheme = if (isDark) {
                darkColorScheme().copy(
                    background = appColors.bg,
                    surface = appColors.surface,
                    onBackground = appColors.onBg,
                    onSurface = appColors.onBg
                )
            } else {
                lightColorScheme(
                    background = appColors.bg,
                    surface = appColors.surface,
                    onBackground = appColors.onBg,
                    onSurface = appColors.onBg
                )
            }

            val baseTypography = Typography()

            MaterialTheme(colorScheme = colorScheme, typography = baseTypography) {
                CompositionLocalProvider(LocalAppColors provides appColors) {
                    Column {
                        ReaderTextSettings(
                            libraryConfig = libraryConfig,
                            onPreferenceChanged = { (activity as? ReaderActivity)?.applyReaderPreferences() }
                        )
                        val whiteLabel = stringResource(id = R.string.color_white)
                        val creamLabel = stringResource(id = R.string.color_cream)
                        val blackLabel = stringResource(id = R.string.color_black)

                        SingleChoiceOptionRow(label = whiteLabel, selected = (selectionBgMode == 0), onClick = {
                            readerConfig.updateReadingBackgroundMode(0f)
                            (activity as? ReaderActivity)?.applyReaderPreferences()
                            dismiss()
                        })
                        SingleChoiceOptionRow(label = creamLabel, selected = (selectionBgMode == 1), onClick = {
                            readerConfig.updateReadingBackgroundMode(1f)
                            (activity as? ReaderActivity)?.applyReaderPreferences()
                            dismiss()
                        })
                        SingleChoiceOptionRow(label = blackLabel, selected = (selectionBgMode == 2), onClick = {
                            readerConfig.updateReadingBackgroundMode(2f)
                            (activity as? ReaderActivity)?.applyReaderPreferences()
                            dismiss()
                        })
                    }
                }
            }
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        try {
            val dlg = dialog
            val bottomSheet = dlg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val lp = bottomSheet.layoutParams
                lp?.width = LayoutParams.MATCH_PARENT
                lp?.height = LayoutParams.WRAP_CONTENT
                bottomSheet.layoutParams = lp
                bottomSheet.requestLayout()

                // Apply theme background
                try {
                    val libraryConfig = LibraryConfig(requireContext())
                    bottomSheet.setBackgroundColor(appColorsFor(libraryConfig.backgroundMode).bg.toArgb())
                } catch (_: Exception) {}

                try {
                    val behavior = BottomSheetBehavior.from(bottomSheet)
                    try { behavior.isFitToContents = true } catch (_: Throwable) {}
                    try { behavior.isHideable = true } catch (_: Throwable) {}
                    try { behavior.skipCollapsed = false } catch (_: Throwable) {}

                    bottomSheet.post {
                        try {
                            val contentH = if (bottomSheet.height > 0) bottomSheet.height else bottomSheet.measuredHeight
                            if (contentH > 0) {
                                try { behavior.peekHeight = contentH } catch (_: Throwable) {}
                            }
                            try { behavior.state = BottomSheetBehavior.STATE_COLLAPSED } catch (_: Throwable) {}
                        } catch (_: Throwable) {}
                    }
                } catch (_: Exception) {}
                // Try to hide system status bar on the dialog's window so it doesn't show when the sheet appears
                try {
                    val dlgWindow = dlg?.window
                    if (dlgWindow != null) {
                        try { WindowCompat.setDecorFitsSystemWindows(dlgWindow, false) } catch (_: Throwable) {}
                        val dlgController = WindowCompat.getInsetsController(dlgWindow, dlgWindow.decorView)
                        dlgController?.let {
                            try { it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } catch (_: Throwable) {}
                            try { it.hide(WindowInsetsCompat.Type.statusBars()) } catch (_: Throwable) {}
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Ensure the activity re-applies immersive/hide-status behavior when the sheet is dismissed
        try {
            val act = activity ?: return
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            controller?.let {
                try {
                    it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    it.hide(WindowInsetsCompat.Type.statusBars())
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
