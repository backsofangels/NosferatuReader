MuPDF Module - Integration & UI Notes
=====================================

Data: 2026-06-28

Scopo
-----
Documento tecnico di riferimento per il modulo `lib/` che contiene l'integrazione MuPDF (Java). Scopo: fornire una mappa rapida dei file, delle classi principali, delle risorse UI e delle linee guida pratiche per apportare modifiche alla UI del reader contenuto in `lib` in modo coerente con l'app principale.

Posizione del modulo
---------------------
- Modulo: `lib/`
- È incluso nel progetto root: `settings.gradle` contiene `include ':lib'`.
- `app/build.gradle` dichiara `api project(':lib')`.

AndroidManifest
----------------
- `lib/src/main/AndroidManifest.xml` dichiara l'`Activity` principale: `com.artifex.mupdf.mini.DocumentActivity`.

Build
-----
- Il modulo usa classico packaging Android (sorgenti Java + risorse XML). Modifiche alle risorse o al codice richiedono un rebuild di `:lib` (la build di `:app` lo include automaticamente quando `api project(':lib')` è dichiarato).

File e classi chiave
--------------------
- `lib/src/main/java/com/artifex/mupdf/mini/DocumentActivity.java`
  - Entry point UI del reader MuPDF fornito dal modulo.
  - Gestisce: apertura documento da `Uri` (intent data), ricerca, outline, zoom, layout text-size (`layoutEm`), paginazione, preferenze locali, e comunicazione con `PageView`.
  - Intent extras: legge l'extra `"<package>.ReturnToLibraryActivity"` per comportamento di ritorno.
  - Preferenze locali: usa `getPreferences(Context.MODE_PRIVATE)` per chiavi locali come `layoutEm`, `fitPage`, e salva pagina corrente con `prefs.getInt(key, 0)` dove `key` è l'URI string.
  - Metodi utili per estendere/modificare UI: `showSearch()`, `hideSearch()`, `toggleUI()`, `loadPage()`, `relayoutDocument()`, `applyInsets()`, `onPageViewSizeChanged()`.

- `lib/src/main/java/com/artifex/mupdf/mini/PageView.java`
  - Custom View che disegna la pagina (bitmap) e gestisce input touch/gestures (tap, long press, fling, pinch-zoom).
  - Espone `setActionListener(DocumentActivity)` e richiama callback su `actionListener` per azioni: `gotoURI()`, `gotoPage()`, `toggleUI()`, `goForward()`, `goBackward()`, `onPageViewSizeChanged(...)`, `onPageViewZoomChanged(...)`.
  - Punto centrale per modifiche del comportamento di navigazione tattile e rendering interattivo.

- `lib/src/main/java/com/artifex/mupdf/mini/OutlineActivity.java`
  - Attività semplice basata su `ListActivity` che mostra il sommario (outline) come lista.

- `lib/src/main/java/com/artifex/mupdf/mini/Worker.java`
  - Helper per eseguire task in background e pubblicare i risultati sull'UI thread.

Risorse (layout, menu, drawable, strings)
-----------------------------------------
- Layout principale: `lib/src/main/res/layout/document_activity.xml`
  - ID utili da conoscere per modifiche UI:
    - `@id/page_view` (tipo `com.artifex.mupdf.mini.PageView`) - area di rendering
    - `@id/background_layout`
    - `@id/top_bar`, `@id/action_bar`, `@id/search_bar`, `@id/bottom_bar`
    - `@id/title_label`, `@id/search_button`, `@id/search_text`, `@id/search_close_button`
    - `@id/zoom_button`, `@id/layout_button`, `@id/outline_button`
    - `@id/page_seekbar`, `@id/page_label`
  - Notare: molti elementi hanno `android:visibility="gone"` di default e vengono mostrati da `DocumentActivity`.

- Menu: `lib/src/main/res/menu/layout_menu.xml` (preset layout sizes 6pt..16pt)
- Stringhe: `lib/src/main/res/values/strings.xml` (hint ricerca, toast, password dialog)
- Drawable: icone e asset grafici in `lib/src/main/res/drawable/` (es. `ic_search_white_24dp.xml`, `button.xml`, `seek_thumb.xml`)

Come l'app host invoca il reader
--------------------------------
- L'apertura standard dal resto dell'app usa `MainActivity.openBook()` che crea un `Intent` indirizzato a `com.artifex.mupdf.mini.DocumentActivity` con `data = File(book.filePath).toUri()` e l'extra `"<package>.ReturnToLibraryActivity" = 1`.
- Questo significa che il flusso di produzione per l'apertura dei documenti passa al modulo `lib` e ad `DocumentActivity`.

Linee guida per modifiche UI nel modulo `lib`
--------------------------------------------
Queste raccomandazioni mirano a mantenere il modulo `lib` modificabile e ben integrato con l'app principale (`:app`) senza rompere l'integrazione.

1) Preferire risorse stilizzate
- Rimuovere colori hard-coded in `document_activity.xml` (es. `android:background="#505050"`) e spostarli in `lib/src/main/res/values/colors.xml` come nomi semantici (es. `reader_bg`, `reader_topbar_bg`, `reader_text_on_bg`).
- Usare attributi di tema (`?attr/readerBackground`) se prevedi sovrascrittura dal modulo host. Questo facilita l'adattamento dei temi Light/Dark/Crema dall'app principale.

2) Esporre opzioni via Intent extras
- Per meglio integrare `LibraryConfig` (impostazioni dell'app host), aggiungi supporto a Intent extras opzionali per controllare: `bg_mode` (int), `show_top_bar` (boolean), `initial_page` (int), `return_to_library` (boolean). DocumentActivity deve verificare prima gli extras e poi eventualmente cadere sul valore di default.
- Esempio di lettura preferenza condivisa (se non fornito nell'intent):
  SharedPreferences appPrefs = getSharedPreferences("library_prefs", Context.MODE_PRIVATE);
  float bgMode = appPrefs.getFloat("background_mode", 0f);

3) Evitare duplicazione delle preferenze
- Attualmente `DocumentActivity` usa `getPreferences(Context.MODE_PRIVATE)` per preferenze locali. Se vuoi che il reader segua le stesse preferenze dell'app (es. sfondo, fontChoice), leggi `library_prefs` (shared preferences pubbliche dell'app) oppure accetta tali valori tramite intent extras. Questo mantiene il modulo `lib` indipendente ma configurabile.

4) Separare layout e logica
- Evita di introdurre logica UI complessa direttamente in `DocumentActivity`: estrai helper/utility in nuove classi (es. `ReaderUiController`) per facilitare unit testing e mantenimento.
- Mantieni `PageView` concentrata sul rendering e sugli eventi tattili; la manipolazione dello stato visuale (mostra/nascondi barre, animazioni) può essere delegata.

5) Integrazione con temi dell'app host
- Se l'app host usa `LibraryConfig.backgroundMode`, considera di fornire un mapping tra i valori (0/1/2) e i colori del modulo `lib`.
- Implementazione minimal-friendly: leggere `library_prefs` al `onCreate()` di `DocumentActivity` e applicare i colori corrispondenti ai container (`backgroundLayout.setBackgroundColor(...)`, `topBar.setBackgroundColor(...)`).

6) Test e regressioni
- Poiché `PageView` gestisce gesti e rendering su bitmap, qualsiasi modifica alla misura dei container o padding può influire sulla navigazione; testare:
  - pinch/zoom
  - tap left/center/right per avanti/indietro/toggle UI
  - scrolling/fling
  - ricerca e highlighting (search hits)

7) Icone e risorse
- Per aggiornare le icone nelle barre, sostituisci i drawable in `lib/src/main/res/drawable/`. Se vuoi condividere le stesse icone dell'app host, considera di usare la stessa resource qualifier e nomi concordati oppure passare le risorse via Intent (meno comune).

8) Accessibilità e dimensioni
- `document_activity.xml` usa misure fisse (48dp per barra, 16/24sp text size). Se il prodotto richiede adattamento dinamico per dimensione testo o preferenze accessibilità, esponi `layoutEm` e/o dimensioni come risorse o preferenze facilmente sovrascrivibili.

Esempi pratici (snippet)
-------------------------
- Leggere `background_mode` condiviso dall'app host:

```
SharedPreferences appPrefs = getSharedPreferences("library_prefs", Context.MODE_PRIVATE);
int bgMode = (int) appPrefs.getFloat("background_mode", 0f);
switch (bgMode) {
  case 1: backgroundLayout.setBackgroundColor(Color.parseColor("#FFF8E1")); break; // crema
  case 2: backgroundLayout.setBackgroundColor(Color.parseColor("#222222")); break; // dark
  default: backgroundLayout.setBackgroundColor(Color.WHITE); break;
}
```

- Esporre extra intent per showTopBar:
```
boolean showTopBar = getIntent().getBooleanExtra("SHOW_TOP_BAR", false);
if (showTopBar) topBar.setVisibility(View.VISIBLE);
```

Punti di attenzione
-------------------
- `PageView` manipola direttamente bitmaps: evitare memory leak (riciclare bitmap esistenti come già fatto). Se aggiungi embedding di fonts o immagini grandi, segui le stesse precauzioni di memoria.
- L'app host può comunicare comportamento al modulo `lib` in due modi: 1) tramite `Intent` extras all'apertura, 2) tramite `SharedPreferences` comuni (`library_prefs`). Scegli uno stile e documentalo in entrambe le parti.
- Se vuoi spostare il modulo verso Compose, valuta un wrapper che continui a esporre la `DocumentActivity` per compatibilità; la migrazione completa di `PageView` e rendering bitmap richiederà lavoro significativo.

Testing rapido
--------------
- Per provare le modifiche al modulo `lib`:

```powershell
# dalla root del repo
.\gradlew.bat :app:assembleDebug
# Installare sul device/emulator e aprire un libro dalla UI
```

- Controllare i log con tag `MuPDF`, `MuPDF-Worker` per diagnostica.

Risorse utili nel codice
------------------------
- `lib/src/main/java/com/artifex/mupdf/mini/DocumentActivity.java` (logica UI principale)
- `lib/src/main/java/com/artifex/mupdf/mini/PageView.java` (rendering & gestures)
- `lib/src/main/java/com/artifex/mupdf/mini/OutlineActivity.java`
- `lib/src/main/res/layout/document_activity.xml`
- `lib/src/main/res/menu/layout_menu.xml`
- `lib/src/main/res/values/strings.xml`
- `lib/src/main/res/drawable/*` (icone + stili bottoni)

Note finali
-----------
Questo documento è pensato come guida pratica per modifiche UI al modulo `lib` in modo che rimangano coerenti con il resto dell'app (`:app`). Se vuoi, posso:
- creare un esempio patch che estrae i colori hardcoded in `colors.xml` e legge `background_mode` dalle `library_prefs` per applicare il tema;
- aggiungere Intent-extras formali e aggiornare `MainActivity.openBook()` per passare `bg_mode` e `SHOW_TOP_BAR`.

