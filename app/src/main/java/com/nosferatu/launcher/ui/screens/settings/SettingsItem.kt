package com.nosferatu.launcher.ui.screens.settings

data class SettingItem(
    val key: SettingKey,
    val titleRes: Int,
    val categoryRes: Int
)

val settingsList = listOf(
    SettingItem(SettingKey.FONT_CHOICE, com.nosferatu.launcher.R.string.setting_font_choice, com.nosferatu.launcher.R.string.category_appearance),
    SettingItem(SettingKey.FONT_SIZE, com.nosferatu.launcher.R.string.setting_font_size, com.nosferatu.launcher.R.string.category_appearance),
    SettingItem(SettingKey.LINE_HEIGHT, com.nosferatu.launcher.R.string.setting_line_height, com.nosferatu.launcher.R.string.category_appearance),
    SettingItem(SettingKey.PAGE_BORDERS, com.nosferatu.launcher.R.string.setting_page_borders, com.nosferatu.launcher.R.string.category_appearance),
    SettingItem(SettingKey.FORCE_BOLD, com.nosferatu.launcher.R.string.setting_force_bold, com.nosferatu.launcher.R.string.category_appearance),
    SettingItem(SettingKey.INVERT_TOUCHES, com.nosferatu.launcher.R.string.setting_invert_touches, com.nosferatu.launcher.R.string.category_navigation),
    SettingItem(SettingKey.VOLUME_KEYS, com.nosferatu.launcher.R.string.setting_volume_keys, com.nosferatu.launcher.R.string.category_navigation),
    SettingItem(SettingKey.AUTOMATIC_SCAN, com.nosferatu.launcher.R.string.setting_automatic_scan, com.nosferatu.launcher.R.string.category_library),
    SettingItem(SettingKey.EXPORT_DATABASE, com.nosferatu.launcher.R.string.setting_export_database, com.nosferatu.launcher.R.string.category_maintenance),
    SettingItem(SettingKey.BACKGROUND_COLOR, com.nosferatu.launcher.R.string.setting_background_color, com.nosferatu.launcher.R.string.category_appearance),
    SettingItem(SettingKey.WIPE_LIBRARY, com.nosferatu.launcher.R.string.setting_wipe_library, com.nosferatu.launcher.R.string.category_maintenance)
)