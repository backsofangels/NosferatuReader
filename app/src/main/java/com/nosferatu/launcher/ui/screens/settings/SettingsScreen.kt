@file:OptIn(kotlin.ExperimentalStdlibApi::class)
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.nosferatu.launcher.ui.components.common.SectionHeader
import com.nosferatu.launcher.ui.components.common.SingleChoiceOptionRow
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.ui.LocalAppColors
import com.nosferatu.launcher.R
import kotlin.math.abs

sealed class SettingsNavigation {
    object Main : SettingsNavigation()
    data class Category(val categoryRes: Int) : SettingsNavigation()
    data class SubMenu(val key: SettingKey, val parentCategoryRes: Int? = null) : SettingsNavigation()
}

@Composable
fun SettingsScreen(libraryConfig: LibraryConfig) {
    var currentNav by remember { mutableStateOf<SettingsNavigation>(SettingsNavigation.Main) }

    val colors = LocalAppColors.current

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        when (val nav = currentNav) {
            is SettingsNavigation.Main -> {
                SettingsMainList(onOpenCategory = { categoryRes ->
                    currentNav = SettingsNavigation.Category(categoryRes)
                }, contentColor = colors.onBg)
            }

            is SettingsNavigation.Category -> {
                val items = settingsList.filter { it.categoryRes == nav.categoryRes }
                CategorySubMenu(
                    title = stringResource(id = nav.categoryRes),
                    items = items,
                    onBack = { currentNav = SettingsNavigation.Main },
                    onOpenSetting = { key ->
                        when (key) {
                            SettingKey.FONT_CHOICE, SettingKey.FONT_SIZE, SettingKey.BACKGROUND_COLOR,
                            SettingKey.FORCE_BOLD, SettingKey.VOLUME_KEYS, SettingKey.INVERT_TOUCHES ->
                                currentNav = SettingsNavigation.SubMenu(key, parentCategoryRes = nav.categoryRes)
                            else -> Log.d("Settings", "Azione per $key")
                        }
                    }
                )
            }

            is SettingsNavigation.SubMenu -> {
                when (nav.key) {
                    SettingKey.FONT_SIZE -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.font_size_title),
                        currentValue = libraryConfig.fontSizeScale,
                        options = FontSizeOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = if (nav.parentCategoryRes != null) SettingsNavigation.Category(nav.parentCategoryRes) else SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateFontSize(it.value) }
                    )
                    SettingKey.FONT_CHOICE -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_font_choice),
                        currentValue = libraryConfig.fontChoice,
                        options = FontChoiceOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = if (nav.parentCategoryRes != null) SettingsNavigation.Category(nav.parentCategoryRes) else SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateFontChoice(it.value) }
                    )
                    SettingKey.BACKGROUND_COLOR -> SelectionSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_background_color),
                        currentValue = libraryConfig.backgroundMode,
                        options = BackgroundColorOption.entries.toTypedArray(),
                        getLabelRes = { it.labelRes },
                        getValue = { it.value },
                        onBack = { currentNav = if (nav.parentCategoryRes != null) SettingsNavigation.Category(nav.parentCategoryRes) else SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateBackgroundMode(it.value) }
                    )
                    SettingKey.FORCE_BOLD -> BooleanSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.force_bold_title),
                        currentValue = libraryConfig.forceBold,
                        onBack = { currentNav = if (nav.parentCategoryRes != null) SettingsNavigation.Category(nav.parentCategoryRes) else SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateForceBold(it) }
                    )
                    SettingKey.VOLUME_KEYS -> BooleanSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_volume_keys),
                        currentValue = libraryConfig.volumeKeys,
                        onBack = { currentNav = if (nav.parentCategoryRes != null) SettingsNavigation.Category(nav.parentCategoryRes) else SettingsNavigation.Main },
                        onSelect = { libraryConfig.updateVolumeKeys(it) }
                    )
                    SettingKey.INVERT_TOUCHES -> BooleanSubMenu(
                        title = stringResource(id = com.nosferatu.launcher.R.string.setting_invert_touches),
                        currentValue = libraryConfig.invertTouches,
                        onBack = { currentNav = if (nav.parentCategoryRes != null) SettingsNavigation.Category(nav.parentCategoryRes) else SettingsNavigation.Main },
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
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(id = R.dimen.spacing_16)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(dimensionResource(id = R.dimen.touch_target_min))) {
                Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = stringResource(id = R.string.cd_back), tint = contentColor)
            }
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
            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_16)), thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin), color = contentColor.copy(alpha = 0.12f))
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
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(id = R.dimen.spacing_16)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = stringResource(id = R.string.cd_back), tint = contentColor)
            }
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = contentColor)
        }
        HorizontalDivider(thickness = dimensionResource(id = R.dimen.divider_1), color = contentColor.copy(alpha = 0.2f))

        options.forEach { (value, labelRes) ->
            val isSelected = value == currentValue
            SingleChoiceOptionRow(
                label = stringResource(id = labelRes),
                selected = isSelected,
                onClick = { onSelect(value) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_16)), thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin), color = contentColor.copy(alpha = 0.12f))
        }
    }
}

@Composable
fun CategorySubMenu(
    title: String,
    items: List<SettingItem>,
    onBack: () -> Unit,
    onOpenSetting: (SettingKey) -> Unit
) {
    val bg = LocalAppColors.current.bg
    val contentColor = LocalAppColors.current.onBg

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(id = R.dimen.spacing_16)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = stringResource(id = R.string.cd_back), tint = contentColor)
            }
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = contentColor)
        }
        HorizontalDivider(thickness = dimensionResource(id = R.dimen.divider_1), color = contentColor.copy(alpha = 0.2f))

        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSetting(item.key) }
                    .heightIn(min = dimensionResource(id = R.dimen.row_min_height))
                    .padding(dimensionResource(id = R.dimen.spacing_16)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(id = item.titleRes), color = contentColor.copy(alpha = 0.87f), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "›", color = contentColor.copy(alpha = 0.4f), fontSize = 20.sp)
            }

            if (index < items.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_16)), thickness = dimensionResource(id = R.dimen.divider_thin), color = contentColor.copy(alpha = 0.12f))
            }
        }
    }
}


@Composable
private fun SettingsMainList(onOpenCategory: (Int) -> Unit, contentColor: Color) {
    val groupedSettings = settingsList.groupBy { it.categoryRes }.entries.toList()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // top title row, clickable to open first category
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (groupedSettings.isNotEmpty()) onOpenCategory(groupedSettings[0].key) }
                    .padding(dimensionResource(id = R.dimen.spacing_16)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(id = R.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = contentColor)
            }
        }

        groupedSettings.forEachIndexed { index, entry ->
            val category = entry.key

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategory(category) }
                        .heightIn(min = dimensionResource(id = R.dimen.row_min_height))
                        .padding(dimensionResource(id = R.dimen.spacing_16)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = category),
                        color = contentColor.copy(alpha = 0.87f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "›", color = contentColor.copy(alpha = 0.4f))
                }
            }

            if (index < groupedSettings.size - 1) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_16)),
                        thickness = dimensionResource(id = R.dimen.divider_thin),
                        color = contentColor.copy(alpha = 0.12f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_16)))
            }
        }
    }
}