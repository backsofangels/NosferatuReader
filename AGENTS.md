# AGENTS Guide - nosferatu-launcher

## Scope
- Single-module Android app (`:app`) with Gradle Kotlin/Compose + Room + Readium.
- Main goal: local EPUB library sync + distraction-free reader UI.

## Project map (start here)
- `app/src/main/java/com/nosferatu/launcher/NosferatuApp.kt`: app-level wiring (Room DB, parser strategies, scanner, repository).
- `app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt`: sync flow between filesystem and Room.
- `app/src/main/java/com/nosferatu/launcher/library/LibraryViewModel.kt`: UI state orchestration via Flows.
- `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt`: all persisted user settings (SharedPreferences `library_prefs`): root path, font size, line height, page margins, **background mode**. Note: recently extended to also persist `fontChoice` (float) and expose `updateFontChoice()`.
- `app/src/main/java/com/nosferatu/launcher/ui/AppColors.kt`: app-wide color theme helpers — `AppColors`, `LocalAppColors`, `bgColorFor()`, `contentColorFor()`, `appColorsFor()` (currently a minimal two-color model; there's an accompanying UI plan to expand it to more semantic tokens).
- `app/src/main/java/com/nosferatu/launcher/ui/MainActivity.kt`: Compose home/library/settings shell + storage permission flow + background-mode theme wiring.
- `app/src/main/java/com/nosferatu/launcher/ui/MainActivity.kt`: Compose home/library/settings shell + storage permission flow + background-mode theme wiring. `MainActivity` was updated to reuse the application `libraryConfig` instance instead of creating a local one.
- `app/src/main/java/com/nosferatu/launcher/ui/screens/settings/SettingsChoices.kt`: settings composables including `BackgroundColorOption` (writes to `LibraryConfig.backgroundMode`).
- `app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt`: Readium reader + locator persistence + background theme application.
- `app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt`: Readium reader + locator persistence + background theme application. `ReaderActivity` now uses the application-level `libraryConfig` instance so in-reader preference changes propagate to the rest of the app.
- `app/src/main/java/com/nosferatu/launcher/ui/components/fontsettings/TextSettingsOverlay.kt`: in-reader font size / line-height panel (`ReaderTextSettings`, `SliderRow`).
- `app/src/main/java/com/nosferatu/launcher/data/Ebook.kt`: data model and `EbookFormat` enum (EPUB, PDF, CBZ) + `CoverImage` wrapper.
- `app/src/main/java/com/nosferatu/launcher/parser/BookParser.kt`: parsing orchestration, detailed logging, and fallback behaviour when a parser strategy is missing.
- `PLAN-UI-IMPROVEMENT.md`: repository-level UI improvement plan with technical appendix and implementation to-do list.
- `app/src/main/java/com/nosferatu/launcher/parser/BookParser.kt`: parsing orchestration, detailed logging, and fallback behaviour when a parser strategy is missing.

## Architecture and data flow
- Runtime DI is manual (no Hilt/Koin): `NosferatuApp` constructs `LibraryRepository`; activities create `LibraryViewModel` via `LibraryViewModelFactory`. Note: `NosferatuApp.libraryConfig` is exposed as an application-scoped `val` and reused by activities.
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
- Readium preferences are applied via `EpubPreferences(fontSize, lineHeight, theme)` — both at `EpubNavigatorFactory.createFragmentFactory(initialPreferences = …)` and at runtime via `navigator.submitPreferences(…)` inside `applyReaderPreferences()`. Note: `fontFamily`/custom-font support is not guaranteed by the navigator; see `PLAN-UI-IMPROVEMENT.md` for options (CSS injection, embedding fonts) and next steps.
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

## Recent UI & Settings updates (2026-04-04)

Summary of recent changes you should know about when editing UI or settings:

- `LibraryConfig` now persists `fontChoice` (float) and exposes `updateFontChoice()` so font selection is observable app-wide. See `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt`.
- Runtime font handling: `MainActivity` and `ReaderActivity` were extended to resolve a runtime font resource named `literata_regular` in `app/src/main/res/font/`. When `fontChoice` selects Literata the app attempts to build a `Typography`/`Typeface` using that resource; if missing the code falls back to system defaults. Place custom fonts under `app/src/main/res/font/` (NOT `res/fonts`) and name them accordingly for the current wiring.
- Reader UI: `ReaderActivity` now applies the selected Typeface to its native TextViews (header/footer/menu) via `applyTypefaceToReaderUI()` and also applies the runtime Typography to the overlay Compose controls.
- Selection rows (Settings): The right-side checkmark was replaced with a new, reusable `SingleChoiceOptionRow` composable (`app/src/main/java/com/nosferatu/launcher/ui/components/common/SingleChoiceOptionRow.kt`). Visual pattern:
	- subtle selected-row background (semantic token `selectedRowBackground` in `AppColors`)
	- 8dp left accent dot (uses `LocalAppColors.current.accent`)
	- selected label displayed semibold
	- full-row click target preserved and `semantics.selected` set for accessibility
- `AppColors` extended with `selectedRowBackground` (see `app/src/main/java/com/nosferatu/launcher/ui/AppColors.kt`). Use `LocalAppColors.current.selectedRowBackground` for subtle tints that adapt to Light/Cream/Dark themes.
- `SelectionSubMenu` (in `SettingsScreen.kt`) now preselects the option whose numeric value is nearest to the saved `currentValue` (nearest-floating logic), so custom in-reader adjustments map to the closest preset row on app start.
- `res/values/dimens.xml` has been added/expanded with tokens used by settings and reader UI (notable: `row_min_height = 56dp`, spacing tokens). Prefer `dimensionResource()` instead of hardcoded dp in Compose where practical.
- UI polish: `SectionHeader` composable exists for section titles and many settings rows now respect min-height, spacing tokens, and semantic colors.

Build notes and gotchas:
- If you encounter the resource merger error "file name must end with .xml" when adding fonts, ensure the font file is placed in `res/font/` (font resources) and not `res/fonts/` (a mistaken folder that can trigger the error). The live repo fixes moved a misplaced font into `app/src/main/res/font/`.
- Readium/EPUB content: Readium's navigator does not guarantee that a custom `fontFamily` will be respected for EPUB body text. If you need EPUB-level font enforcement, plan for one of:
	1. Injecting CSS with an `@font-face` + applying it to the publication views (requires navigator support or fragment-level injection), or
	2. Embedding fonts inside the EPUB (editorial work per-publication).

Guardrails for agents (added):
- When changing single-choice settings rows, use `SingleChoiceOptionRow` and the `selectedRowBackground` semantic token; do not restore trailing checkmarks or radio buttons without product sign-off.
- Do not hardcode color overlays per-theme; use `AppColors.selectedRowBackground` so the tint adapts to Light/Cream/Dark.
- When adding fonts: place files under `app/src/main/res/font/` and reference by resource name (current code expects `literata_regular`). If you change the resource name, update `MainActivity` and `ReaderActivity` runtime resolution accordingly.
- When touching `ReaderActivity.onCreate`, remember `setTheme()` must run before `setContentView()`.
- If you modify `SelectionSubMenu` selection logic, preserve the nearest-option behavior that maps custom user values to the closest preset.

Quick file map for the recent edits:
- `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt` — added `fontChoice` persistence.
- `app/src/main/java/com/nosferatu/launcher/ui/AppColors.kt` — added `selectedRowBackground` and theme mappings.
- `app/src/main/java/com/nosferatu/launcher/ui/components/common/SingleChoiceOptionRow.kt` — new reusable row composable.
- `app/src/main/java/com/nosferatu/launcher/ui/screens/settings/SettingsScreen.kt` — replaced trailing checkmarks with `SingleChoiceOptionRow`; added nearest-option selection logic.
- `app/src/main/java/com/nosferatu/launcher/ui/MainActivity.kt` — dynamic Typography resolution for `fontChoice`.
- `app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt` — `applyTypefaceToReaderUI()` + overlay typography sync.
- `app/src/main/res/values/dimens.xml` — new tokens used throughout the UI.

Acceptance test checklist for reviewers:
1. `.\\gradlew.bat clean assembleDebug` should succeed.
2. Open Settings -> `Colore Sfondo`: the selected row shows a subtle background, left accent dot, semibold label, and no trailing checkmark.
3. Open Settings -> `Dimensione Testo`: ensure the option nearest to the saved custom reader font size is selected on app start.
4. Toggle background modes (Default/Cream/Dark) and confirm the selected-row tint adapts and remains subtle.
5. If `Literata` is selected and `app/src/main/res/font/literata_regular.ttf` exists, the Compose UI and reader overlay should use the custom font; header/footer TextViews in `ReaderActivity` should also switch to the Typeface.

