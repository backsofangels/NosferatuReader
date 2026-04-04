# UI Changes — 2026-04-04

Questo documento riepiloga le modifiche UI implementate nell'ambito del piano *UI Improvement* (P0/P1).

## Sommario rapido
- TOC: conversione da dialog a BottomSheet con RecyclerView gerarchico (header + item + icone). Implementazione in `ReaderActivity` + layout sotto `app/src/main/res/layout/`.
- Matching TOC→posizioni: nuove euristiche (`normalizeHrefForMatch`, `findBestPositionForHref`) per migliorare il mapping tra href e Locator.
- Segnalibri: persistence leggera (SharedPreferences) con `BookmarkStore` e UI (toggle top-right, panel segnalibri).
- Font runtime: `LibraryConfig.fontChoice` + risoluzione runtime di `literata_regular` in `MainActivity` e `ReaderActivity`.
- Settings polish: `SingleChoiceOptionRow`, `selectedRowBackground` semantic token in `AppColors` e `row_min_height` centralizzato.
- Accessibilità e polish: `contentDescription`, tooltip per i pulsanti, touch-target >= 48dp, animazioni leggere (toggle rotation, bottom-sheet expand), ripple/selectable backgrounds.

## Come verificare (Acceptance checklist)
1. Aggiungi un EPUB nella libreria e aprilo.
2. Apri il menu in lettura (tap centrale), poi premi il pulsante TOC (bottom-left): si apre un bottom-sheet gerarchico con header e voci.
3. Seleziona una voce TOC: il lettore dovrebbe navigare al capitolo scelto e chiudere il bottom-sheet.
4. Premi il pulsante segnalibro (top-right): l'icona passa da outline a filled; apri il pannello segnalibri (bottom-right) e verifica che il segnalibro sia presente.
5. Imposta `Literata` posizionando `literata_regular.ttf` in `app/src/main/res/font/` e riavvia l'app: le UI overlay e le TextView di header/footer adottano il Typeface (nota: Readium può non applicare il font al body dell'EPUB).

## File principali modificati
- `app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt` — TOC bottom-sheet, matcher, bookmark wiring, tooltips.
- `app/src/main/res/layout/bottom_sheet_toc.xml` — layout sheet container.
- `app/src/main/res/layout/item_toc.xml` — item row (icon + title).
- `app/src/main/res/layout/item_toc_header.xml` — header row style.
- `app/src/main/res/values/strings.xml` — nuove stringhe di accessibilità e messaggi.
- `app/src/main/java/com/nosferatu/launcher/library/LibraryConfig.kt` — `fontChoice` persistence.
- `app/src/main/java/com/nosferatu/launcher/ui/AppColors.kt` — `selectedRowBackground` token.
- `app/src/main/java/com/nosferatu/launcher/ui/components/common/SingleChoiceOptionRow.kt` — nuova composable (se presente).

(Altre modifiche rilevanti: `MainActivity.kt`, `SettingsScreen.kt`, `dimens.xml`, `BookmarkStore.kt`, `Bookmark.kt`.)

## Limitazioni note
- Readium non garantisce che `fontFamily` personalizzati siano applicati ai contenuti EPUB; per forzare l'uso di un font occorre valutare: 1) iniezione CSS con `@font-face`, o 2) embedding del font all'interno degli EPUB.
- Le euristiche di matching possono fallire su TOC con href molto particolari (URI con query complesse, riferimenti temporanei o id HTML non standard).
- Persistenza segnalibri: V1 basata su `SharedPreferences` (JSON). Valutare migrazione a Room se i segnalibri diventano numerosi o servono query avanzate.

## Prossimi passi consigliati
- Aggiungere comportamento collapsible per gruppi TOC (es. header espandibile) per una UX più chiara.
- Audit accessibilità completo (TalkBack, contrasto, navigazione tastiera).
- Migrazione opzionale store segnalibri a Room con migration path.

---

Se vuoi, posso generare un breve CHANGELOG in `CHANGELOG.md` o convertire questa pagina in un formato HTML per renderla consultabile direttamente nel repo.