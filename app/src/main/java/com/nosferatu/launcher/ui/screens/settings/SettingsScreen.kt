package com.nosferatu.launcher.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.foundation.layout.heightIn
import com.nosferatu.launcher.ui.components.common.SectionHeader
import com.nosferatu.launcher.ui.components.common.SingleChoiceOptionRow
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.ui.LocalAppColors
import kotlin.math.abs

sealed class SettingsNavigation {
    object Main : SettingsNavigation()
    data class SubMenu(val key: SettingKey) : SettingsNavigation()
}

@Composable
fun SettingsScreen(libraryConfig: LibraryConfig) {
    var currentNav by remember { mutableStateOf<SettingsNavigation>(SettingsNavigation.Main) }

    val colors = LocalAppColors.current

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        when (val nav = currentNav) {
            is SettingsNavigation.Main -> {
                SettingsMainList(onNavigate = { key ->
                    when (key) {
                        SettingKey.FONT_CHOICE, SettingKey.FONT_SIZE, SettingKey.BACKGROUND_COLOR,
                        SettingKey.FORCE_BOLD, SettingKey.VOLUME_KEYS, SettingKey.INVERT_TOUCHES ->
                            currentNav = SettingsNavigation.SubMenu(key)
                        else -> Log.d("Settings", "Azione per $key")
                    }
                }, contentColor = colors.onBg)
            }

            is SettingsNavigation.SubMenu -> {
                when (nav.key) {
                    SettingKey.FONT_SIZE -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.font_size_title),
                        currentValue = libraryConfig.fontSizeScale,
                        options = FontSizeOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateFontSize(it.value) }
                    )
                    SettingKey.FONT_CHOICE -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_font_choice),
                        currentValue = libraryConfig.fontChoice,
                        options = FontChoiceOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateFontChoice(it.value) }
                    )
                    SettingKey.BACKGROUND_COLOR -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_background_color),
                        currentValue = libraryConfig.backgroundMode,
                        options = BackgroundColorOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateBackgroundMode(it.value) }
                    )
                    SettingKey.FORCE_BOLD -> BooleanSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.force_bold_title),
                        currentValue = libraryConfig.forceBold,
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateForceBold(it) }
                    )
                    SettingKey.VOLUME_KEYS -> BooleanSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_volume_keys),
                        currentValue = libraryConfig.volumeKeys,
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateVolumeKeys(it) }
                    )
                    SettingKey.INVERT_TOUCHES -> BooleanSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_invert_touches),
                        currentValue = libraryConfig.invertTouches,
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateInvertTouches(it) }
                    )
                    else -> currentNav = SettingsNavigation.Main
                }
            }
        }
    }
}

@Composable
fun <T> SelectionSubMenu(
    title: String,
    currentValue: Float,
    options: Array<T>,
    getLabelRes: (T) -> Int,
    getValue: (T) -> Float,
    onBack: () -> Unit,
    onSelect: (T) -> Unit
) {
    // use the actual app theme colours, not derived from the setting value
    val bg = LocalAppColors.current.bg
    val contentColor = LocalAppColors.current.onBg

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onBack() }
                .padding(dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = com.nosferatu.launcher.R.string.back_arrow),
                modifier = Modifier.padding(end = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)),
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = contentColor)
        }
        HorizontalDivider(thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_1), color = contentColor.copy(alpha = 0.2f))

        // select the option whose value is nearest to the saved/current value
        val nearestOption = options.minByOrNull { abs(getValue(it) - currentValue) }

        options.forEach { option ->
            val isSelected = option == nearestOption
            SingleChoiceOptionRow(
                label = stringResource(id = getLabelRes(option)),
                selected = isSelected,
                onClick = { onSelect(option) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)), thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin), color = contentColor.copy(alpha = 0.12f))
        }
    }
}

@Composable
fun BooleanSubMenu(
    title: String,
    currentValue: Boolean,
    onBack: () -> Unit,
    onSelect: (Boolean) -> Unit
) {
    val bg = LocalAppColors.current.bg
    val contentColor = LocalAppColors.current.onBg

    val options = listOf(
        true to com.nosferatu.launcher.R.string.option_enabled,
        false to com.nosferatu.launcher.R.string.option_disabled
    )

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onBack() }
                .padding(dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = com.nosferatu.launcher.R.string.back_arrow),
                modifier = Modifier.padding(end = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)),
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = contentColor)
        }
        HorizontalDivider(thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_1), color = contentColor.copy(alpha = 0.2f))

        options.forEach { (value, labelRes) ->
            val isSelected = value == currentValue
            SingleChoiceOptionRow(
                label = stringResource(id = labelRes),
                selected = isSelected,
                onClick = { onSelect(value) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)), thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin), color = contentColor.copy(alpha = 0.12f))
        }
    }
}

@Composable
private fun SettingsMainList(onNavigate: (SettingKey) -> Unit, contentColor: Color) {
    val groupedSettings = settingsList.groupBy { it.categoryRes }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groupedSettings.forEach { (category, items) ->
            item {
                SectionHeader(title = stringResource(id = category))
            }
            itemsIndexed(items) { index, setting ->
                Column(modifier = Modifier.fillMaxWidth().clickable { onNavigate(setting.key) }
                    .heightIn(min = dimensionResource(id = com.nosferatu.launcher.R.dimen.row_min_height))
                    .padding(dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16))) {
                    Text(text = stringResource(id = setting.titleRes), color = contentColor.copy(alpha = 0.87f))
                }
                if (index < items.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16)), thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin), color = contentColor.copy(alpha = 0.12f))
            }
        }
    }
}