package com.nosferatu.launcher.ui.screens.settings

data class SettingItem(
    val key: SettingKey,
    val title: String,
    val category: String
)

val settingsList = listOf(
    SettingItem(SettingKey.FONT_CHOICE, "Carattere Predefinito", "Aspetto e Tipografia"),
    SettingItem(SettingKey.FONT_SIZE, "Dimensioni Testo", "Aspetto e Tipografia"),
    SettingItem(SettingKey.LINE_HEIGHT, "Interlinea", "Aspetto e Tipografia"),
    SettingItem(SettingKey.PAGE_BORDERS, "Margini Pagina", "Aspetto e Tipografia"),
    SettingItem(SettingKey.FORCE_BOLD, "Forza Grassetto", "Aspetto e Tipografia"),
    SettingItem(SettingKey.INVERT_TOUCHES, "Modalità Inversione Tocchi", "Comportamento e Navigazione"),
    SettingItem(SettingKey.VOLUME_KEYS, "Tasti Volume", "Comportamento e Navigazione"),
    SettingItem(SettingKey.AUTOMATIC_SCAN, "Scansione Automatica", "Gestione Libreria"),
    SettingItem(SettingKey.EXPORT_DATABASE, "Esporta Database", "Manutenzione"),
    SettingItem(SettingKey.WIPE_LIBRARY, "Reset Libreria", "Manutenzione")
)