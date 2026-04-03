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
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.ui.LocalAppColors
import com.nosferatu.launcher.ui.bgColorFor
import com.nosferatu.launcher.ui.contentColorFor
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
                        SettingKey.FONT_SIZE, SettingKey.LINE_HEIGHT, SettingKey.BACKGROUND_COLOR,
                        SettingKey.FORCE_BOLD, SettingKey.VOLUME_KEYS ->
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
                    SettingKey.LINE_HEIGHT -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.line_height_title),
                        currentValue = libraryConfig.lineHeightFactor,
                        options = LineHeightOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateLineHeight(it.value) }
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
    // derive colors from current app background setting via library config value
    val bg = bgColorFor(currentValue)
    val contentColor = contentColorFor(currentValue)

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onBack() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(id = com.nosferatu.launcher.R.string.back_arrow), modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Black, color = contentColor)
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = contentColor)
        }
        HorizontalDivider(thickness = 1.dp, color = contentColor.copy(alpha = 0.2f))

        options.forEach { option ->
            val isSelected = abs(getValue(option) - currentValue) < 0.01f
            Column(modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(id = getLabelRes(option)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) contentColor else contentColor.copy(alpha = 0.7f)
                    )
                    if (isSelected) Text(text = stringResource(id = com.nosferatu.launcher.R.string.check_mark), fontWeight = FontWeight.Black, color = contentColor)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = contentColor.copy(alpha = 0.12f))
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
            modifier = Modifier.fillMaxWidth().clickable { onBack() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(id = com.nosferatu.launcher.R.string.back_arrow), modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Black, color = contentColor)
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = contentColor)
        }
        HorizontalDivider(thickness = 1.dp, color = contentColor.copy(alpha = 0.2f))

        options.forEach { (value, labelRes) ->
            val isSelected = value == currentValue
            Column(modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(id = labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) contentColor else contentColor.copy(alpha = 0.7f)
                    )
                    if (isSelected) Text(text = stringResource(id = com.nosferatu.launcher.R.string.check_mark), fontWeight = FontWeight.Black, color = contentColor)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = contentColor.copy(alpha = 0.12f))
        }
    }
}

@Composable
private fun SettingsMainList(onNavigate: (SettingKey) -> Unit, contentColor: Color) {
    val groupedSettings = settingsList.groupBy { it.categoryRes }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groupedSettings.forEach { (category, items) ->
            item {
                Text(
                    text = stringResource(id = category).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                    color = contentColor,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
                )
            }
            itemsIndexed(items) { index, setting ->
                Column(modifier = Modifier.fillMaxWidth().clickable { onNavigate(setting.key) }.padding(16.dp)) {
                    Text(text = stringResource(id = setting.titleRes), color = contentColor.copy(alpha = 0.87f))
                }
                if (index < items.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = contentColor.copy(alpha = 0.12f))
            }
        }
    }
}