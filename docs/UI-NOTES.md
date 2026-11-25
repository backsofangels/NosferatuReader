# UI Implementation Notes — 2026-04-04

Queste sono note tecniche per manutentori e sviluppatori che lavorano sulle modifiche UI implementate.

## TOC
- L'estrazione del TOC usa reflection per accomodare possibili varianti dell'API Readium (`getTableOfContents`, `getToc`, ecc.).
- La mappatura TOC→posizioni usa `publication.positions()` (suspend). Calcolo eseguito in `lifecycleScope.launch` per evitare blocchi UI.
- Funzioni chiave in `ReaderActivity.kt`:
  - `extractToc(pub: Publication?)` — recupera struttura TOC in un modello `TocNode(title, href, children)`.
  - `flattenToc(...)` — crea lista indentata per RecyclerView.
  - `normalizeHrefForMatch(href)` — rimuove fragment/query, decodifica percent-encoding, rimuove estensioni comuni e normalizza.
  - `findBestPositionForHref(href, positions)` — euristiche: exact, endsWith, contains, last-segment.

## Performance & sicurezza
- `publication.positions()` può essere pesante per alcuni EPUB: la chiamata è effettuata una volta per mappare TOC verso posizioni; considerare caching se necessario.
- Evitare di chiamare `positions()` su ogni singola navigazione; la strategia corrente tenta mappe precompute.

## Font runtime
- L'app prova a risolvere la risorsa `literata_regular` con `resources.getIdentifier("literata_regular", "font", packageName)`.
- Posizionare il file `literata_regular.ttf` in `app/src/main/res/font/` e usare il nome risorsa `literata_regular`.
- Nota: per applicare il font al body dell'EPUB potrebbe essere necessario iniettare CSS o modificare il publication rendering layer.

## Reader lifecycle
- IMPORTANT: `setTheme()` deve essere chiamato prima di `setContentView()` per garantire che gli attributi XML siano risolti correttamente.
- `EpubNavigatorFragment.currentLocator` è un Flow; l'UI osserva e aggiorna header/footer, progress bar, icona segnalibro e la selezione TOC quando il bottom-sheet è aperto.

## Bookmark store (V1)
- Implementazione attuale: `BookmarkStore` usa `SharedPreferences` per salvare JSON per libro.
- Strategia di identità: `locatorJson` (chiave), `href`, `progression`, `chapterTitle`, `createdAt`.
- Rimozioni e aperture segnalibri avvengono su AlertDialog/BottomSheet a seconda del flusso.

## Accessibilità
- Tutti i pulsanti critici hanno `contentDescription` e tooltip via `ViewCompat.setTooltipText(...)`.
- Ridotto uso di colori per stato: selezione ha sia background tint che semibold per non dipendere solo dal colore.
- Rispettare `row_min_height` in `dimens.xml` per touch target.

## Consigli per sviluppo futuro
- Migrare `BookmarkStore` a Room se servono query o sincronizzazione cloud.
- Aggiungere test end-to-end per: TOC navigation, bookmark add/remove, font selection fallback.
- Aggiungere eventi di logging per misurare latenza di `positions()` su EPUB grandi.

## Comandi utili per sviluppatori
- Build & assemble (Windows PowerShell):

```powershell
.\gradlew.bat clean assembleDebug --console=plain
```

- To run the app locally, use Android Studio or `adb install` on the generated apk under `app/build/outputs/apk/debug/`.

---

Se vuoi, inserisco queste note anche in `app/README-UI.md` con link rapidi ai posti nel codice dove intervenire.