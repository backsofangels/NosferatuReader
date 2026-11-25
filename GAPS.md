GAPS: Confronto UI e passaggi per allineare MuPDF a `activity_reader`
=====================================================================

Data: 2026-06-28

Obiettivo
---------
Identificare le differenze grafiche e funzionali tra la UI reader dell'app (`app/src/main/res/layout/activity_reader.xml`) e la UI del modulo MuPDF (`lib/src/main/res/layout/document_activity.xml`), e definire i passaggi concreti necessari per rendere l'`Activity` di MuPDF visivamente equivalente alla `activity_reader`, mantenendo l'architettura e le scelte implementative di MuPDF (senza riscriverne il rendering bitmap/gestures).

Sintesi rapida
---------------
- `activity_reader.xml` (app) usa ConstraintLayout, supporta attributi di tema (`?attr/...`), un footer immersivo con progress bar e testo, status bar composable, ComposeView per i controlli font e token di tema condivisi.
- `document_activity.xml` (lib/MuPDF) è RelativeLayout con `PageView` custom e top/bottom bar XML, usa colori hard-coded e view IDs diversi; gestisce direttamente gestures e rendering in `PageView`.

Principali gap grafici e semantici
---------------------------------
1) Theming/colori
- activity_reader: usa attributi `?attr/readerBackgroundColor`, `?attr/readerTextColor`, `?attr/readerProgressBackgroundColor` (tema applicazione). Colori non hard-coded.
- document_activity: sfondi e barre con colori hard-coded (`#505050`, `#303030`, `#FFFFFF`).

2) Struttura top/bottom bars e footer
- activity_reader ha `menu_top_bar`, `menu_bottom_bar`, `immersive_footer` con `ProgressBar` + `TextView` separati e visibilità controllata; usa constraint layout quindi il posizionamento è consistente su diversi dispositivi.
- document_activity usa `top_bar`, `action_bar`, `bottom_bar` all'interno di RelativeLayout con visibilità `gone` ed elementi con dimensioni e posizionamenti differenti. Mancano la `immersive_footer` e alcune view di menu presenti in `activity_reader`.

3) Seek/progress styling
- activity_reader usa `ProgressBar` con tint via attribute e `SeekBar` con `?attr/readerTextColor`.
- document_activity ha `SeekBar` e `TextView` ma stili e drawable dei thumb/line sono differenti (seppur presenti). Il posizionamento del label non corrisponde al layout moderno.

4) Iconografia e dimensioni
- activity_reader usa icone e dimensioni da `@dimen/` e tints via attr; document_activity incorpora drawables bianchi e bottoni con background `@drawable/button` senza supporto automatico per tint dinamico dal tema host.

5) Fonts e tipografia
- activity_reader applica runtime Typeface/`Typography` (via `LibraryConfig.fontChoice`) e usa Compose overlay per le impostazioni; document_activity usa TextView con font system default, non legge `library_prefs` per `fontChoice` nativamente.

6) Insets & safe area
- Entrambe gestiscono insets ma in modo differente: activity_reader usa ConstraintLayout + attr; document_activity applica `onApplyWindowInsetsListener` su `topBar` e `bottomBar`.

7) Compose integration
- activity_reader integra una `ComposeView` per i controlli font in overlay; MuPDF rimane interamente XML/Java e non prevede Compose nella sua UI.

8) IDs e punti di estensione
- Gli ID non corrispondono (es. `menu_page_text` vs `page_label`) — serve una mappatura per allineare comportamento e styling.

Passaggi consigliati per allineamento grafico (mantendo MuPDF internals)
------------------------------------------------------------------------
Di seguito una lista operativa, ordinata per priorità e con suggerimenti implementativi specifici.

Fase 1 — Preparazione risorse e tematizzazione (basso rischio)
- A1. Estrarre colori hard-coded da `lib/src/main/res/layout/document_activity.xml` in `lib/src/main/res/values/colors.xml` e sostituire con riferimenti semantici (es. `@color/reader_bg`, `@color/reader_topbar_bg`, `@color/reader_text_on_bg`).
- A2. Aggiungere in `lib/src/main/res/values/attrs.xml` attributi personalizzati opzionali per il reader (es. `readerBackgroundColor`, `readerTextColor`, `readerProgressBackgroundColor`).
- A3. Modificare `document_activity.xml` per usare `?attr/readerBackgroundColor`/`?attr/readerTextColor` dove possibile (fall-back a `@color/reader_bg` se attr non fornito).
- A4. DocumentActivity: leggere `library_prefs` (SharedPreferences `library_prefs`) in `onCreate()` e applicare colori dinamicamente se l'app host non imposta gli attrs (compatibilità retrograda).

Fase 2 — Allineamento struttura e view (moderato rischio)
- B1. Aggiungere alla layout `document_activity.xml` una sezione `immersive_footer` compatibile al footer di `activity_reader`: inserire `ProgressBar` orizzontale e `TextView` (id: `immersive_progress_bar`, `immersive_footer_text`) posizionati sopra il `bottom_bar` o al bottom della root. Usare gli stessi id per facilitarne il binding.
- B2. Rinominare o aggiungere alias id per view chiave per mappare i nomi usati nell'app host (es. mantenere `page_label` ma aggiungere `menu_page_text` come alias o aggiornare `DocumentActivity` per scrivere anche su `menu_page_text`).
- B3. Aggiornare `page_seekbar` styling: usare gli stessi drawable (o mappare a `@drawable/seek_line`/`seek_thumb` già presenti) e applicare `progressTint`/`thumbTint` via attributi.

Fase 3 — Tipografia, icone e dimensioni (moderato/alto rischio)
- C1. Applicare supporto `fontChoice`: in `DocumentActivity.onCreate()` leggere `library_prefs.getFloat("font_choice", 0f)` e, quando è selezionata la font "literata" (o altra), usare `ResourcesCompat.getFont()` per costruire un `Typeface` e applicarla alle TextView `title_label`, `page_label`, `menu_book_title`, `menu_book_author`, `immersive_footer_text`.
- C2. Aggiornare i bottoni top/bottom (ImageButton/ImageView) per usare `android:tint="?attr/readerTextColor"` o applicare programmaticamente il tint al runtime in `onCreate()`.
- C3. Sostituire misure hard-coded con dimensioni da `lib/src/main/res/values/dimens.xml` (es. `reader_icon_size`, `reader_icon_padding`, `reader_progress_height`) per parità con `app`.

Fase 4 — Comportamento visibilità e animazioni (alto rischio)
- D1. Allineare le regole di show/hide delle barre: modificare `DocumentActivity.toggleUI()` per rispettare la stessa logica di `activity_reader` (es. animazioni, visibilità del footer immersivo, status bar clock). Assicurarsi che la logica rispetti ancora input/tap/gestures implementati in `PageView`.
- D2. Mappare eventi navigatori (bookmark toggle / settings) alle stesse callback dell'app host dove applicabile (es. inviare broadcast/Intent o usare `ReturnToLibraryActivity` behavior esistente).

Mappatura id / view raccomandata
--------------------------------
- document_activity.xml `page_seekbar` → `menu_seek_bar` (allineare comportamenti)
- `page_label` → creare alias `menu_page_text` o aggiornare DocumentActivity a impostare entrambi
- Aggiungere `immersive_progress_bar` e `immersive_footer_text` (id identici a `activity_reader`) per render indistinguibile il footer.

Modifiche Java consigliate (sintesi)
-----------------------------------
- Leggere `library_prefs` e `fontChoice` in `onCreate()` e applicare colore/font.
- Dopo il `setContentView()` eseguire `findViewById()` per i nuovi id aggiunti e collegare la logica già presente (es. aggiornare `pageLabel` e `pageSeekbar` con i nuovi riferimenti).
- Quando si disegna/aggiorna pagina, update anche `immersive_progress_bar` e `immersive_footer_text` per indicare progress/posizione corrente.
- Applicare tint su ImageView/ImageButton con `setColorFilter(readerTextColor)` quando attr non gestiti dal tema.

Test e verifiche
-----------------
- Testare su dispositivi con density e dimensioni diverse: verifica riconciliazione di insets, safe-area e padding della `immersive_footer`.
- Verificare gesti (`PageView`) per non introdurre lag dopo l'aggiunta di View extra o animazioni.
- Verificare memory profile (bitmap allocations) su low-memory device.
- Verificare che `MainActivity.openBook()` e l'extra `ReturnToLibraryActivity` non vengano alterati.

Rischi e trade-off
------------------
- Spostare logica di theming e font nel modulo `lib` aumenta il coupling con l'app host. Soluzione: leggere preferenze da `library_prefs` ma mantenere il comportamento per default indipendente.
- Aggiungere Compose alla UI di `lib` richiede dipendenza su AndroidX Compose nel modulo `lib` — preferibile usare solo una `ComposeView` come bridge (minimo impatto) oppure mantenere tutto in XML.

Esempio sequenza di lavoro consigliata
-------------------------------------
1. A1, A2, A3: estrazione colori + attrs + fallback via `library_prefs` (giorno 1).
2. B1, B2, B3: aggiunta `immersive_footer` e mappatura id; aggiornare `DocumentActivity` per bind (giorno 2).
3. C1..C3: tipografia, tint, dimens (giorno 3).
4. D1..D2: affinamento della visibilità e comportamento (giorno 4).
5. E1: (opzionale) add `ComposeView` per `ReaderTextSettings` e integrare preferenze.

Deliverable di esempio che posso preparare
-----------------------------------------
- Patch che estrae colori hard-coded in `lib/src/main/res/values/colors.xml` e modifica `document_activity.xml` per usare `?attr/readerBackgroundColor` (con fallback a `@color/reader_bg`).
- Patch che aggiunge `immersive_footer` (layout+ids) e wiring minimo in `DocumentActivity` per aggiornare il progress bar.

Se vuoi, applico la patch iniziale (A1+A2+A3) ora e poi procediamo con le fasi successive a step controllati.