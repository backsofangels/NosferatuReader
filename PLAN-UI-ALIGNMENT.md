PLAN: UI Alignment — Make MuPDF reader visually match `activity_reader`
=====================================================================

1. Objective
------------
Allineare la UI del modulo `lib` (MuPDF: `DocumentActivity`) alla UI dell'app principale definita in `app/src/main/res/layout/activity_reader.xml`, in modo che il reader fornito da MuPDF risulti, a livello grafico e di comportamento visuale, indistinguibile dall'`activity_reader` dell'app. Il lavoro deve preservare l'implementazione interna di MuPDF (rendering bitmap, gestione gestures in `PageView`) e minimizzare il coupling distruttivo tra `lib` e `app`.

2. Scope
--------
### In scope
- Tema (colori/tints) e mapping dei token di tema da `LibraryConfig.backgroundMode` a `lib` colors.
- Estrarre risorse (`colors`, `dimens`, `attrs`) nel modulo `lib` e usare attributi di tema con fallback a SharedPreferences.
- Aggiungere footer immersivo (`ProgressBar` + `TextView`) e mappare id per allineare i binding.
- Applicare supporto runtime per `fontChoice` (Typeface) nelle TextView rilevanti.
- Aggiornare drawables/buttons per supportare `tint` dinamico dal tema host.
- Mappare comportamenti di visibilità delle barre e fornire hook per animazioni/overlay.
- Fornire istruzioni e test manuali per validazione grafica e regressioni funzionali.

### Out of scope
- Riscrivere `PageView` (rendering/gestures) in Compose o cambiare il core rendering.
- Migrazioni DB o modifiche a `LibraryRepository`/parser.
- Aggiungere test automatizzati estesi (questo piano include come step opzionale la creazione di test manuali o unitari di binding minimi).

3. Product intent
-----------------
- L'utente deve percepire la stessa esperienza visiva e feed visivi (footer, progress bar, titoli, icone) quando apre un libro, indipendentemente dal fatto che il reader sia MuPDF (`:lib`) o il navigator Readium.
- Mantenere la stabilità delle gesture e delle performance; evitare operazioni che introducano lag nel rendering delle pagine.
- Rendere configurabile il reader `lib` sia tramite Intent extras (apertura) sia tramite `library_prefs` per fallback, favorendo Intent extras per la configurazione puntuale.

4. Codebase context
-------------------
- Modulo host: `app/` con `activity_reader.xml` (ConstraintLayout), Compose overlay (`font_controls_container`), `LibraryConfig` osservabile per `backgroundMode`, `fontChoice`.
- MuPDF module: `lib/` con `DocumentActivity.java`, `PageView.java`, layout `lib/src/main/res/layout/document_activity.xml`.
- Build: `settings.gradle` includes `:lib`, `app/build.gradle` depends on `project(':lib')`.
- Theme hooks: `app` uses `?attr/readerBackgroundColor`, etc.; `lib` currently hard-codes colori.
- Constraint: `DocumentActivity.setTheme()` behavior not applicable for `lib` (Activity uses full-screen with no theme attrs from host), but we can read `library_prefs` and apply colors programmatically or use attributes if host provides styled context.

5. Constraints
--------------
- Non cambiare il rendering/gestures di `PageView`.
- Non introdurre dipendenze Compose complete nel modulo `lib` (possono essere aggiunte solo come `ComposeView` bridge opzionale).
- Le modifiche devono compilare con l'attuale Gradle setup e non rompere il package `com.artifex.mupdf.mini` public API.
- Evitare modifiche breaking all'Intent contract (`ReturnToLibraryActivity`).

6. Guardrails
-------------
- Non modificare file di business logic non-UI in `lib` (parser, worker, doc opener).
- Non rimuovere la gestione di memoria/bitmap attuale (riciclare bitmap è obbligatorio).
- Non aggiungere risorse con nomi che possano collidere con l'app host senza prefisso `mupdf_` o `reader_` nel modulo `lib`.

7. Current state
----------------
- Current component: `lib/src/main/res/layout/document_activity.xml` (RelativeLayout) + `DocumentActivity` (Java) + `PageView`.
- Current behavior: interactive PDF/EPUB rendering via MuPDF, top/bottom bars toggled, search, outline, page seek.
- Problem: visual styles (colori, dimensions, footer, tint, font) non allineati col `activity_reader` e non leggono le preferenze `LibraryConfig` dell'app host.
- Impact: UX incoerente quando l'app apre libri (muPdF sembra visivamente diverso), più costi di QA e di manutenzione.

8. Target behavior
------------------
- Quando l'app apre un libro con `MainActivity.openBook()`, la UI di `DocumentActivity` appare visivamente identica a `activity_reader`: stesso background, stessa tipografia per header/footer, stesso footer con progress bar e testo di posizione, icone e tints coerenti con il tema.
- Il comportamento di navigazione e gestures non cambia.
- L'app host può forzare il tema per `lib` inviando Intent extras (es. `bg_mode`, `SHOW_TOP_BAR`) o tramite `library_prefs` per default persistente.

9. Implementation strategy
--------------------------
- Minimal invasive strategy: introdurre risorse e hook nel modulo `lib` per leggere attributi di tema e preferenze condivise; applicare i cambiamenti programmaticamente in `DocumentActivity` subito dopo `setContentView()`.
- Preferire Intent extras per configurazioni runtime (miglior isolamento), con fallback su `library_prefs` per retrocompatibilità.
- Mantieni `PageView` intatto; delega UI visiva e animazioni a nuovi helper `ReaderUiController` integrato in `DocumentActivity`.

10. Files to inspect first
--------------------------
- `lib/src/main/java/com/artifex/mupdf/mini/DocumentActivity.java`
- `lib/src/main/res/layout/document_activity.xml`
- `lib/src/main/java/com/artifex/mupdf/mini/PageView.java`
- `app/src/main/res/layout/activity_reader.xml`
- `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt`

11. Proposed file changes
-------------------------
### Modify
- `lib/src/main/res/layout/document_activity.xml` (use attr refs, add footer views, rename/add ids)
- `lib/src/main/java/com/artifex/mupdf/mini/DocumentActivity.java` (bind new ids, read Intent extras / library_prefs, apply colors/fonts/tint, update progress updates)
- `lib/src/main/res/drawable/*` (update button backgrounds to allow tint)
- `lib/src/main/res/values/strings.xml` (add any missing labels if needed)

### Create
- `lib/src/main/res/values/colors.xml` (reader_bg, reader_topbar_bg, reader_text_on_bg, reader_progress_bg, ...) 
- `lib/src/main/res/values/attrs.xml` (declare `readerBackgroundColor`, `readerTextColor`, `readerProgressBackgroundColor`)
- `lib/src/main/res/values/dimens.xml` (reader_icon_size, reader_icon_padding, reader_progress_height, spacing tokens)
- `lib/src/main/java/com/artifex/mupdf/mini/ReaderUiController.java` (helper to encapsulate UI bindings and animations)

### Do not touch unless necessary
- `lib/src/main/java/com/artifex/mupdf/mini/PageView.java` (rendering/gestures)
- `lib/src/main/java/com/artifex/mupdf/mini/Worker.java` (background queue)

12. Data model / state model
---------------------------
No new persistence required. Configuration sources:
- Intent extras (transient per open): `bg_mode` (int), `SHOW_TOP_BAR` (boolean), `initial_page` (int), `font_choice` (int)
- SharedPreferences fallback: `library_prefs` keys `background_mode`, `font_choice` (existing)

13. UI model
------------
- `ReaderUiController` responsibilities:
  - Bind views: topBar, actionBar, immersiveFooter (progress + text), menuBottomBar, pageSeekbar, pageLabel.
  - Apply theme tokens (colors, tints) and fonts.
  - Provide `show()`/`hide()`/`toggle()` with optional animations respecting `PageView` gestures.
  - Update progress and page label on page changes.

14. Task breakdown
------------------
### Task 1 — Extract colors & attrs (A1, A2, A3)
Goal
- Remove hard-coded colors and expose theme attrs and color resources.
Files
- `lib/src/main/res/layout/document_activity.xml`
- create `lib/src/main/res/values/colors.xml`
- create `lib/src/main/res/values/attrs.xml`
Work
- Add `colors.xml` with semantic color names.
- Add `attrs.xml` declaring `readerBackgroundColor`, `readerTextColor`, `readerProgressBackgroundColor`.
- Update `document_activity.xml` to reference `?attr/...` where feasible and fallback to `@color/reader_bg`.
Done when
- `DocumentActivity` shows UI using colors from resources/attrs rather than hard-coded values.

### Task 2 — Add dimens & drawables adjustments (C3)
Goal
- Harmonize dimensions and make button drawables tintable.
Files
- `lib/src/main/res/values/dimens.xml`
- `lib/src/main/res/drawable/button.xml` and icons
Work
- Create `dimens.xml` with sizes matching `app` tokens.
- Update `button.xml` to use tintable shapes (use `android:tint` support or `tint` attribute in ImageButton usage).
Done when
- Buttons and icon sizes match application style and respond to tint color.

### Task 3 — Add immersive footer and id mapping (B1, B2, B3)
Goal
- Add `immersive_footer` with `immersive_progress_bar` and `immersive_footer_text`. Map ids to match `activity_reader`.
Files
- `lib/src/main/res/layout/document_activity.xml`
- `lib/src/main/java/com/artifex/mupdf/mini/DocumentActivity.java`
Work
- Insert footer layout at bottom of `document_activity.xml` using existing RelativeLayout anchors.
- After `setContentView()`, `findViewById()` for new ids and wire progress updates from page load code.
- Ensure `page_seekbar` uses same drawables and max/progress logic as app seek bar.
Done when
- Footer appears and updates during navigation with the same layout as `activity_reader`.

### Task 4 — Implement ReaderUiController and bind in DocumentActivity (4.X)
Goal
- Encapsulate UI logic into `ReaderUiController` to keep `DocumentActivity` focused.
Files
- `lib/src/main/java/com/artifex/mupdf/mini/ReaderUiController.java`
- Update `DocumentActivity.java` to construct and use `ReaderUiController`.
Work
- Implement methods: `applyTheme(int bgMode, int fontChoice)`, `updateProgress(int page, int pageCount)`, `toggleUI()`.
- Ensure any show/hide respects `PageView` state and does not disable gesture regions.
Done when
- UI code is moved into controller and `DocumentActivity` calls controller methods on page events.

### Task 5 — FontChoice support (C1)
Goal
- Read `font_choice` from Intent extras or `library_prefs` and apply Typeface to relevant TextViews.
Files
- `DocumentActivity.java`, `ReaderUiController.java`
Work
- Use `ResourcesCompat.getFont(context, resId)` if `literata` packaged; fallback to default Typeface.
- Apply to `titleLabel`, `pageLabel`, `menu_book_title`, `menu_book_author`, `immersive_footer_text`.
Done when
- On fontChoice change, the UI text reflects chosen font.

### Task 6 — Tint and icons (C2)
Goal
- Apply `readerTextColor` as tint to icon ImageViews
Files
- `DocumentActivity.java`, drawables modifications
Work
- Set `imageView.setColorFilter(readerTextColor)` in controller or use `android:tint` in XML where supported.
Done when
- Icons match theme color at runtime.

### Task 7 — Behavior parity and animations (D1, D2)
Goal
- Align toggle/show-hide behavior and hook bookmark/settings actions to host flows where applicable.
Files
- `DocumentActivity.java`, `ReaderUiController.java`
Work
- Update `toggleUI()` to mirror the host animations/visibility states; wire `btn_bookmark_toggle` to existing actions (if host expects broadcast or repo updates, provide hooks).
Done when
- UI toggles visually like `activity_reader` and bookmark/settings buttons trigger appropriate callbacks.

### Task 8 — Optional Compose overlay (E1)
Goal
- Add a `ComposeView` for `ReaderTextSettings` if desired.
Files
- `document_activity.xml` (add ComposeView), possible small Gradle changes
Work
- Add `ComposeView` with id `font_controls_container` and populate with `ReaderTextSettings` via `setContent` using the same `LibraryConfig` (read via fallback or passed as configuration).
Done when
- Compose overlay shows and modifies preferences used by `DocumentActivity`.

15. Suggested implementation order
---------------------------------
1. Task 1 (A1-A3): Colors & attrs
2. Task 2: Dimens & drawables
3. Task 3: Footer & id mapping
4. Task 4: ReaderUiController & wiring
5. Task 5: FontChoice
6. Task 6: Icon tint
7. Task 7: Behavior parity and polish
8. Task 8: Optional Compose overlay

16. Edge cases
--------------
- No font resource found for chosen `fontChoice` → fallback to default system Typeface.
- Host does not provide `library_prefs` (rare in test contexts) → use resource defaults.
- Large document opening memory pressure → avoid allocating large temporary bitmaps in UI-binding code.
- Intent with `file://` URIs not allowed by platform → respect existing DocumentActivity behavior and error messages.

17. Performance requirements
----------------------------
- Do not allocate large objects on UI thread. All page decoding remains in `Worker` as today.
- UI updates (text, progress) must be O(1) per page change and not trigger full re-layouts.

18. Accessibility requirements
------------------------------
- Ensure `contentDescription` present for icon buttons (use existing strings or add new ones to `strings.xml`).
- Maintain 48dp touch targets for ImageViews (use dimens tokens).
- Ensure color contrast sufficient across themes.

19. Testing plan
----------------
### Build checks
- `.\gradlew.bat :app:assembleDebug`

### Manual checks
- Open a sample document via UI. Verify visual parity of background, header/footer, progress bar and icon tints compared to `activity_reader`.
- Test tap/gesture navigation: left tap -> backward, middle -> toggle UI, right -> forward.
- Change `library_prefs.background_mode` and reopen document; verify colors applied.
- Switch `fontChoice` and verify Typeface applied.

### Regression checks
- Ensure `PageView` gestures unaffected.
- Ensure `ReturnToLibraryActivity` behavior unchanged.
- Verify memory usage during page navigation on low-memory emulator.

20. Acceptance criteria
----------------------
- Visual parity: Background, header/footer, progress bar and icon tints match `activity_reader` in all three `background_mode` values.
- No regression in gestures or navigation performance.
- `MainActivity.openBook()` still works with existing Intent contract.
- Font choice and theme color apply at open-time and after preference changes.

21. Definition of Done
----------------------
- Code compiles and app assembles.
- Manual acceptance tests pass.
- Changes limited to `lib` resources and `DocumentActivity` helper with non-invasive approach.
- New resources documented in `MuPDF-DOCS.md` and change summary included in a PR description.

22. Deliverables expected from the coding agent
----------------------------------------------
- Patch/branch implementing Task 1 (colors/attrs) and Task 3 (immersive footer) as an initial PR.
- `PLAN-UI-ALIGNMENT.md` (this document) included in repo.
- Short README snippet in PR describing how to validate visually.

23. Open questions
------------------
- Prefer Intent extras or `library_prefs` as primary configuration channel? Recommendation: prefer Intent extras (explicit, per-open) and use `library_prefs` as fallback.
- Do you want the optional Compose overlay (Task 8) included in v1 or postponed?



