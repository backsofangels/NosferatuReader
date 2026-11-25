# TECH-STATUS

Data: 2026-06-28

Questo documento fornisce un'analisi tecnica estensiva dello stato attuale dell'applicazione `nosferatu-launcher`, basata sul codice sorgente e sulla documentazione presente nella repo.

## Executive summary

- Progetto single-module Android (Kotlin + Jetpack Compose + Room). Il flusso di apertura dei documenti in produzione utilizza MuPDF (modulo `:lib`) tramite `com.artifex.mupdf.mini.DocumentActivity`; il codice include anche componenti Readium per funzionalità EPUB avanzate.
- Architettura semplice e manuale: `NosferatuApp` costruisce i componenti (Room, repository, scanner, parser); non è presente DI framework (no Hilt/Koin).
- Implementazione robusta per EPUB: parsing metadati, gestione copertine, persistenza TOC (nuova). Il reader principale è MuPDF (modulo `:lib`) e `ReaderActivity` fornisce un'integrazione Readium alternativa/avanzata (navigator, CSS injection).
- Rischi principali: migrazioni Room distruttive (fallbackToDestructiveMigration), gestione permessi storage complessa, font/compatibilità (in particolare per l'approccio di iniezione CSS), mancanza di completezza dei parser per PDF/CBZ.

## Architettura e flusso dati

- Entrypoint runtime: `NosferatuApp` costruisce `LibraryRepository`. Le Activity creano `LibraryViewModel` tramite `LibraryViewModelFactory`.
- Sync library: `LibraryViewModel.scanBooks()` → `LibraryRepository.syncLibrary()` → `LibraryScanner.scanDirectory()` → `BookParser.parseMetadata()` → `BookDao.insertBook()` → opzionalmente `TocParser.parse()` → `TocDao.replaceForBook()`.
-- Reader: l'apertura standard dalla UI (`MainActivity.openBook()`) avvia MuPDF `DocumentActivity` (`com.artifex.mupdf.mini.DocumentActivity`) dal modulo `:lib`. Il progetto contiene anche `ReaderActivity` che inizializza un navigator Readium (`EpubNavigatorFragment`) e fornisce funzionalità avanzate per EPUB (percorsi alternativi o test).
- Configurazioni utente: `LibraryConfig` (SharedPreferences `library_prefs`) espone stato osservabile (`mutableFloatStateOf`, `mutableStateOf`) per Compose.

## Moduli principali

- app (UI shell + settings): `MainActivity.kt`, composables per libreria e impostazioni.
- Library sync: `LibraryRepository.kt`, `LibraryScanner.kt`, `LibraryViewModel.kt`.
- Parser: `BookParser.kt` (strategy-based), `ParserStrategy` implementazioni (EPUB è mappato), `TocParser.kt` per TOC extraction.
- Reader: `ReaderActivity.kt`, `EpubNavigatorFactory` + `EpubPreferences`, CSS injector e fallback per iniezione font.
 - MuPDF module: `:lib` (incluso come `project(':lib')` in `app/build.gradle`) che fornisce `com.artifex.mupdf.mini.DocumentActivity` ed è usato come reader principale per l'apertura dei documenti.
- Persistence: Room (`AppDatabase.kt`), entità `EbookEntity` (ora con `tocImported`), `TocEntryEntity`, DAO `BookDao`, `TocDao`.
- UI theme: `AppColors.kt`, `LocalAppColors`, gestione `backgroundMode` (0: light, 1: sepia, 2: dark), `SingleChoiceOptionRow`/`SelectionSubMenu`.

## Implementazione chiave e osservazioni

- `LibraryConfig` (app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt)
  - Conserva `backgroundMode`, `fontSizeScale` (default 0.85f), `fontChoice`, `forceBold`, `volumeKeys`, `invertTouches`.
  - Espone API `updateBackgroundMode()`, `updateFontChoice()`, ecc. Usa SharedPreferences e state osservabile per Compose.

- Theming: `AppColors.kt` mappa il `backgroundMode` su palette Compose e fornisce `selectedRowBackground` per le righe selezionate.

- `MainActivity` (app/src/main/java/.../MainActivity.kt)
  - UI Compose app-wide; costruisce dinamicamente la `Typography` tentando di risolvere una risorsa font `literata_regular` in `res/font`.
  - Richiede permessi storage (gestione Android 11+ con `MANAGE_EXTERNAL_STORAGE` fallback).
  - Nota: crea localmente un `LibraryConfig` via `remember { LibraryConfig(this) }` — possibile incongruenza rispetto all'idea di usare l'istanza applicazione-scoped (verificare coerenza con `NosferatuApp`).
  - `openBook()` avvia MuPDF `DocumentActivity` (`com.artifex.mupdf.mini.DocumentActivity`) passando un file URI: questo rende il flusso principale di apertura dei libri basato sul modulo `:lib`.

- `ReaderActivity` (app/src/main/java/.../reader/ReaderActivity.kt)
  - Chiama `setTheme()` prima di `setContentView()` (corretto) e inizializza Readium.
  - Applica ottimizzazioni per dispositivi a bassa memoria (disabilita publisher styles, riduce fontWeight, ecc.).
  - Implementa un meccanismo robusto di iniezione CSS/font: 1) cerca asset `assets/fonts/literata_regular.ttf`; 2) prova `res/font/literata_regular`; 3) scrive su `files/` e riferisce con file://; 4) fallback log e tentativi via reflection per chiamare `evaluateJavascript` sul fragment/navigator.
  - TOC: preferisce caricare TOC da DB (persisted `TocEntryEntity`) e ha fallback runtime se DB vuoto.

- `BookParser` e strategie
  - `BookParser.parseMetadata` determina `EbookFormat` da estensione e invoca `ParserStrategy` corrispondente. Se non esiste strategy, ritorna un `Ebook` minimo e logga.
  - `EbookFormat.fromExtension` può lanciare eccezione su estensioni sconosciute — `LibraryScanner.extractMetadata` cattura eccezioni e ritorna null per saltare file non-parsabili.

- Persistence / DB
  - Room con `fallbackToDestructiveMigration(true)` (AppDatabase) — DB version 5 attuale (tocImported + toc_entries aggiunti).
  - Nuova tabella `toc_entries` + `TocDao` per velocizzare l'apertura TOC nel reader.

## Test e quality

- Ci sono test unitari aggiunti per `TocDao` e `TocParser`. Verificare `app/src/test` per copertura.
- Non sono presenti test strumentati estesi; mancano integrazione end-to-end per `syncLibrary()` ↔ reader.
- Raccomandazione: aggiungere test di integrazione che eseguono `LibraryRepository.syncLibrary()` su fixture EPUB e validano persistenza TOC + `tocImported`.

## Known issues, rischi e technical debt

1. Migrazioni DB distruttive: `fallbackToDestructiveMigration(true)` implica perdita dati su aggiornamenti di schema. Se si vuole preservare dati reali, è necessario aggiungere `Migration` da v4→v5.
2. Permessi storage: uso di `MANAGE_EXTERNAL_STORAGE` e logica Android 11+ aumenta la superficie di rischio e richiede gestione UX chiara per gli utenti.
3. Font/Readium: Readium potrebbe non rispettare `fontFamily` per il body; iniezione CSS è fragile e usa reflection ed heuristics — fonte di fragilità e di bug su diversi dispositivi/vers. navigator.
4. Parser incompleti: `EbookFormat` include PDF/CBZ, ma le `ParserStrategy` potrebbero non essere registrate per questi formati — scanner può scoprire file ma parsing non garantito.
5. Hardcoded strings in Kotlin (italiano) limitano localizzazione.
6. Costruzione `LibraryConfig` duplicata: `MainActivity` crea un'istanza locale invece di riutilizzare potenzialmente `NosferatuApp.libraryConfig` (incoerenza da verificare).
7. Iniezioni JS/CSS complesse nella `ReaderActivity` (reflection) — difficile da testare e soggetta a regressioni.
8. Gestione memory pressure: ci sono ottimizzazioni (fontSize default 0.85, disable publisher styles) ma servono test su dispositivi reali per validarne l'efficacia.
9. Build fragility su Windows: file-locking `R.jar` noto; istruzioni esistenti ma attenzione in CI Windows.

## Raccomandazioni prioritarie (breve)

1. Aggiungere Room `Migration` non-distruttiva da v4→v5 oppure fornire script di migrazione e avviso agli utenti.
2. Consolidare `LibraryConfig` come singola istanza condivisa (application-scoped) e usare quella in `MainActivity`/`ReaderActivity` per garantire consistenza runtime.
3. Externalizzare stringhe testuali nelle risorse `strings.xml` per localizzazione.
4. Limitare l'uso di reflection per la CSS injection; investigare API ufficiali del navigator o richiedere feature upstream a Readium.
5. Creare test di integrazione per `LibraryRepository.syncLibrary()` con fixture EPUB e per `TocParser`.
6. Documentare chiaramente le aspettative sui font (nome risorsa `literata_regular`, percorso `res/font` o `assets/fonts`) e semplificare l'installazione per QA.
7. Decidere policy su supporto PDF/CBZ: 1) implementare ParserStrategy, o 2) filtrare scanner per EPUB-only.
8. Aggiungere CI (es. GitHub Actions) che lanci `./gradlew assembleDebug` e i test unitari su Linux; aggiungere matrix Windows solo se necessario.

## Next steps operativi suggeriti

- (A) Implementare Migration Room v4→v5 (alto impatto, alto priorità).
- (B) Aggiungere integrazione test per sync+TOC (medio impatto).
- (C) Consolidare `LibraryConfig` e rimuovere istanze duplicate (basso impatto ma migliora coerenza).
- (D) Rivedere font-embedding: prefer asset o `res/font` con dimensione limitata; evitare embed base64 se > 800KB.
- (E) Audit permessi storage e UX di richiesta permessi (Android 11+).

## Quick checklist per validazione manuale

- Build: 

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat test --console=plain
```

- Test funzionale minimo:
  - Effettuare `scanBooks()` da UI o chiamare `LibraryViewModel.scanBooks()`; verificare che `toc_entries` sia popolata per EPUB con `nav.xhtml`.
  - Aprire libro dalla UI: l'apertura standard usa MuPDF `DocumentActivity`; verificare anche `ReaderActivity` (Readium) per percorsi alternativi e che la TOC (quando persistita) carichi dal DB e la navigazione salti alle posizioni attese.
  - Controllare i log con tag `LibraryRepository`, `ReaderActivity`, `BookParser` per messaggi diagnostici.

## File rilevanti (mappa rapida)

- `app/src/main/java/com/nosferatu/launcher/NosferatuApp.kt` — wiring app-level
- `app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt` — sync + TOC persistence
- `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt` — preferenze utente osservabili
- `app/src/main/java/com/nosferatu/launcher/parser/BookParser.kt` — parsing metadata
- `app/src/main/java/com/nosferatu/launcher/parser/TocParser.kt` — TOC extraction
- `app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt` — Readium integration + CSS/font injection
- `app/src/main/java/com/nosferatu/launcher/ui/AppColors.kt` — tema & semantic tokens
 - `lib/` — MuPDF module (project `:lib`) che fornisce `com.artifex.mupdf.mini.DocumentActivity`, usato per l'apertura predefinita dei documenti dalla UI.
- `app/src/main/java/com/nosferatu/launcher/data/*` — entità Room, DAO, `Ebook.kt`, `EbookEntity.kt`

---

Se vuoi, aggiorno subito la checklist per i task prioritari (Migration, test d'integrazione) e creo le issue o le PR template per procedere. In alternativa posso generare un piano di implementazione dettagliato per il punto (A) Migration.
