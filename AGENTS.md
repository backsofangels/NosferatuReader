# AGENTS Guide - nosferatu-launcher

## Scope
- Single-module Android app (`:app`) with Gradle Kotlin/Compose + Room + Readium.
- Main goal: local EPUB library sync + distraction-free reader UI.

## Project map (start here)
- `app/src/main/java/com/nosferatu/launcher/NosferatuApp.kt`: app-level wiring (Room DB, parser strategies, scanner, repository).
- `app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt`: sync flow between filesystem and Room.
- `app/src/main/java/com/nosferatu/launcher/library/LibraryViewModel.kt`: UI state orchestration via Flows.
- `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt`: all persisted user settings (SharedPreferences `library_prefs`): root path, font size, line height, page margins, **background mode**.
- `app/src/main/java/com/nosferatu/launcher/ui/AppColors.kt`: app-wide color theme helpers — `AppColors`, `LocalAppColors`, `bgColorFor()`, `contentColorFor()`, `appColorsFor()`.
- `app/src/main/java/com/nosferatu/launcher/ui/MainActivity.kt`: Compose home/library/settings shell + storage permission flow + background-mode theme wiring.
- `app/src/main/java/com/nosferatu/launcher/ui/screens/settings/SettingsChoices.kt`: settings composables including `BackgroundColorOption` (writes to `LibraryConfig.backgroundMode`).
- `app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt`: Readium reader + locator persistence + background theme application.
- `app/src/main/java/com/nosferatu/launcher/ui/components/fontsettings/TextSettingsOverlay.kt`: in-reader font size / line-height panel (`ReaderTextSettings`, `SliderRow`).
- `app/src/main/java/com/nosferatu/launcher/data/Ebook.kt`: data model and `EbookFormat` enum (EPUB, PDF, CBZ) + `CoverImage` wrapper.
- `app/src/main/java/com/nosferatu/launcher/parser/BookParser.kt`: parsing orchestration, detailed logging, and fallback behaviour when a parser strategy is missing.

## Architecture and data flow
- Runtime DI is manual (no Hilt/Koin): `NosferatuApp` constructs `LibraryRepository`; activities create `LibraryViewModel` via `LibraryViewModelFactory`.
- Library sync path: `LibraryViewModel.scanBooks()` -> `LibraryRepository.syncLibrary()` -> `LibraryScanner.scanDirectory()` -> `BookParser.parseMetadata()` -> `BookDao.insertBook()`.
- Scanner constraints are intentional: `walkTopDown().maxDepth(2)` and skips dot folders + `Android` (`LibraryScanner.kt`).
- Metadata parser is strategy-based by extension (`BookParser.kt` + `ParserStrategy.kt`); currently only `EbookFormat.EPUB` is mapped in app wiring.
- Reader path: `MainActivity.openBook()` passes `BOOK_PATH`, `LAST_LOCATION_JSON`, `BOOK_ID`; `ReaderActivity` opens publication via Readium and persists locator in `onStop()`.

## Background theme system
The app supports three background modes, stored as a Float in SharedPreferences (`library_prefs` / `background_mode`):

| Mode value | Name    | Hex color  | Readium `Theme`   |
|-----------|---------|------------|-------------------|
| 0         | Default | `#FFFFFF`  | `Theme.LIGHT`     |
| 1         | Cream   | `#FFF8E1`  | `Theme.SEPIA`     |
| 2         | Dark    | `#222222`  | `Theme.DARK`      |

- **`LibraryConfig.backgroundMode`**: observable `mutableFloatStateOf` backed by SharedPreferences; updated via `updateBackgroundMode()`.
- **`AppColors.kt`**: `bgColorFor(mode)` / `contentColorFor(mode)` / `appColorsFor(mode)` map the Float to Compose `Color`s. `LocalAppColors` is the `CompositionLocal` used everywhere in the Compose tree.
- **`MainActivity`**: provides `LocalAppColors` via `CompositionLocalProvider`; reads `background_mode` on startup to set the initial Compose theme.
- **`ReaderActivity`**: reads `background_mode` in `onCreate` to call `setTheme()` with the matching XML style (`Theme_NosferatuReader`, `Theme_NosferatuReader_Cream`, `Theme_NosferatuReader_Dark`) before `setContentView`; also provides `LocalAppColors` inside `setupSettingsPanel()`.
- **Readium rendering**: `EpubPreferences.theme` is set to the corresponding `Theme` enum value both at navigator creation and in `applyReaderPreferences()`, so the epub content background follows the user's choice.

## Build and developer workflows
- Canonical commands (from `README.md`):
```bash
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
```
- Windows shell equivalent in this repo:
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```
- If `processDebugResources` throws an `IOException` about `R.jar`, run `.\gradlew.bat clean assembleDebug` (file-locking issue on Windows).
- No test sources are present under `app/src/test` or `app/src/androidTest`; validate changes at least with `assembleDebug`.

## Codebase-specific conventions
- UI is mixed by design: app shell uses Compose, while reader screen uses XML layout + `EpubNavigatorFragment` + a `ComposeView` settings panel (`activity_reader.xml`, `ReaderActivity.kt`).
- Persisted user settings live in `LibraryConfig` (`SharedPreferences` `library_prefs`): root path, font size, line height, page margins, background mode. All numeric settings use `mutableFloatStateOf` for Compose observability.
- DB uses Room with `fallbackToDestructiveMigration(true)` (`AppDatabase.kt`): schema changes can wipe local data.
- Many UI strings are hardcoded Italian literals in Kotlin (not fully resource-driven), e.g. settings/filter labels.
- Logging is pervasive (`Log.d/e`) and used for parser/sync diagnosis; keep tags and diagnostics when touching these paths.
- The in-reader font/line-height overlay (`TextSettingsOverlay.kt` / `ReaderTextSettings`) uses `LocalAppColors` so it adapts to the active background mode. Buttons intentionally have no border decoration.

## Integrations and boundaries
- External storage access is central (`READ/WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`, legacy external storage flags in `AndroidManifest.xml`).
- Readium stack: `readium-shared`, `readium-streamer`, `readium-navigator`, `readium-adapter-pdfium` (see `app/build.gradle`, `libs.versions.toml`).
- Readium preferences are applied via `EpubPreferences(fontSize, lineHeight, theme)` — both at `EpubNavigatorFactory.createFragmentFactory(initialPreferences = …)` and at runtime via `navigator.submitPreferences(…)` inside `applyReaderPreferences()`.
- EPUB metadata/covers are extracted with `epublib` + zip/OPF parsing; covers are resized and written under app internal `files/covers` (`CoverManager.kt`).
- Additional repositories beyond Google/MavenCentral are required (`settings.gradle`): psiegman GitHub Maven repo + JitPack.

## Guardrails for agents
- Do not assume formats beyond EPUB are enabled until strategy wiring is added in `NosferatuApp.kt`.
- If changing scan behavior, preserve current exclusion/depth semantics unless explicitly requested.
- If changing DB entities/DAO, call out data-loss impact due to destructive migration.
- When touching `ReaderActivity.onCreate`, remember that `setTheme()` **must** be called before `setContentView()` for XML attribute resolution to work correctly.
- When adding new reading preferences, update both the initial `EpubPreferences` block in `onCreate` and `applyReaderPreferences()` to keep them in sync.
- Note: license text is inconsistent in repo (`README.md` says Apache-2.0, source header in `NosferatuApp.kt` references AGPL); ask before normalizing.
- Note: `EbookFormat` (in `data/Ebook.kt`) now contains PDF and CBZ values. These formats exist in the data model but are only discoverable by the scanner if a corresponding ParserStrategy is wired in `NosferatuApp.kt` (scanner filters by `parser.supportedFormats`).
- Implementation detail: `BookParser.parseMetadata` calls `EbookFormat.fromExtension` to map file extensions to the enum; that method throws on unknown extensions. `LibraryScanner.extractMetadata` catches parsing exceptions and returns null, but if you add callers, be aware of the potential IllegalArgumentException from `fromExtension`.

