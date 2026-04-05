# PLAN.md

## 1. Objective
Implement a refactoring of TOC for the books imported in the NosferatuReader
app. The TOC must be calculated when the book is imported, leveraging on the
LibraryScanner class, ideally implementing a method to explore the book .epub
file, and retrieve the TOC from there. The TOCs retrieved must be then committed
into a Room table, which will have as primary key the book id, and the TOCs to be retrieved.
This is all to speed up the retrieval of TOCs in the book ReadingActivity, which on older
devices is quite underwhelming with performances, causing the app to crash.

---

## 2. Scope
List what is included and what is explicitly excluded.

### In scope
- New table creation with Room
- Update of database version
- Save TOCs and their position in the new Room table
- Eventually create new entities if needed
- Scanning of book TOCs to import them at first scan

### Out of scope
- Full redesign
- Unrelated refactors

---

## 3. Product intent
The implementation must resolve the need to calculate TOCs when the button is pressed.
Having them stored in the database locally, even if might be not-needed in the first step,
will improve the performances later when retrieving the TOCs to let the user navigate. The improvement
will be all under the hood, not modifying the UI at all.

---

## 4. Codebase context
The main points ideally would be `EbookEntity` class as the entity that saves Ebooks, 
`AppDatabase` as the class to set a higher database version, `BookDao` and `Ebook` as the 
as-is memory layer. Furthermore, the needed classes to apply the logic would be `LibraryScanner`, where the
main scanning logic resides.  Leverage also on `BookParser`, `EpubParser` and `OpfMetadata` as needed,
but ideally I would keep a separate class as `TocParser`. The `TocParser` needs to be expandable with further
strategies (see `ParserStrategy`) when they will be implemented

The flow would ideally be this

- On a first startup of the app, empty tables are created
- The user loads a book and presses the relevant button in the main screen
- The app scans the file system
- - Epub is found
  - Regular epub entry is created in database
  - Entity with TOC in separate table is created, storing name, location, and book

---

## 5. Constraints
Constraints are the followings
- DB migration should be needed, so apply it without fearing to break
- No expensive background processing, keep it simple
- Leverage on existing classes but keep responsibilities separated as needed

---

## 6. Guardrails
- Do not change scan behavior unless needed and documenting it, both in code and separate doc files
- Do not alter persistence format without documenting it
- Do not remove existing logging
- Do not introduce heavy dependencies

---

## 7. Current state
Currently, the TOC are calculated every time the relevant button is pressed in the `ReaderActivity` class.
This is to avoid, since it's heavy on the user's device.

---

## 8. Target behavior
- UI/UX of the TOC must remain the same
- UI/UX must be isofunctional to previous implementation
- All under the hood logic must be improved to reflect a better perofrmance

---

## 9. Implementation strategy
Use new RoomTable to store the TOCs.

---

## 10. Files to inspect first
Inspect the files under `com/nosferatu/launcher/library`, `com/nosferatu/launcher/parser` and
`com/nosferatu/launcher/repository`
---

## 12. Data model / state model
Proposed data model
TOC:
- bookId
- TocName
- locatorJson

---

## 16. Edge cases
Consider the following edge cases:
- No TOC available -> Null handling at runtime, no TOCs displayed

---

## 17. Performance requirements
- No full text indexing
- No expensive EPUB reparsing once publication is open
- UI updates must remain lightweight

--- 

## 19. Testing plan
The gradlew assembleDebug will be executed after every completed step.
Also, on business logic class, there will be unit tests.

### Build checks
- `./gradlew assembleDebug`
- 

---

## 21. Definition of Done
Broader than acceptance criteria.

Include:
- Code compiles
- Feature works
- No obvious regression
- Logs remain useful
- New files are documented
- No dead code introduced
- Any tradeoffs/limitations are documented [web:280][web:286]

---

## 22. Deliverables expected from the coding agent
Tell the agent what to return at the end.

Example:
- summary of files changed
- summary of architectural choices
- known limitations
- commands run
- follow-up recommendations

---

## Plan: TOC Persistence Implementation

TL;DR
Parse EPUB table-of-contents during initial library scan, persist TOC entries in a new Room table, and make the reader load TOCs from the DB with a runtime fallback. Keep changes minimal, reuse existing parser utilities, and prefer a simple destructive migration for the first iteration (document trade-offs).

**Steps**
1. Add a Room entity for TOC entries
- Create a new entity `TocEntryEntity` with fields: `id` (auto-generated PK), `bookId` (indexed, Long), `title` (String), `locatorJson` (String), and `position` (Int). Optionally include `href` (String) and `createdAt` timestamp.
- File to add: [app/src/main/java/com/nosferatu/launcher/data/TocEntryEntity.kt](app/src/main/java/com/nosferatu/launcher/data/TocEntryEntity.kt)

2. Add a DAO to manage TOC rows
- Add a new `TocDao` with methods: `insertAll(entries: List<TocEntryEntity>)`, `getForBook(bookId: Long): List<TocEntryEntity>`, and `deleteForBook(bookId: Long)`. Mark insert as `@Transaction` when performing replace semantics.
- File to add: [app/src/main/java/com/nosferatu/launcher/data/TocDao.kt](app/src/main/java/com/nosferatu/launcher/data/TocDao.kt)

3. Bump DB schema and wire the new entity
- Update [app/src/main/java/com/nosferatu/launcher/data/database/AppDatabase.kt](app/src/main/java/com/nosferatu/launcher/data/database/AppDatabase.kt) to include `TocEntryEntity` in the `@Database(entities = [...])` list and increment the database `version`.
- Note: the current project builds the DB with `.fallbackToDestructiveMigration(true)`; document the data-loss risk. If preserving existing databases is required, implement a specific Room Migration that creates the table.

4. Implement a TocParser
- Add a `TocParser` in the parser package to extract TOC entries from EPUBs. Reuse logic from `EpubParser`/`OpfParser` to locate `nav.xhtml` or NCX and extract ordered entries (label + href). Return a lightweight domain model `TocEntry(title, href, position, locatorJson?)`.
- File to add: [app/src/main/java/com/nosferatu/launcher/parser/TocParser.kt](app/src/main/java/com/nosferatu/launcher/parser/TocParser.kt)

5. Persist TOCs during library scan
- Modify [app/src/main/java/com/nosferatu/launcher/library/LibraryScanner.kt](app/src/main/java/com/nosferatu/launcher/library/LibraryScanner.kt) to call `TocParser` for EPUB files during `extractMetadata(file)` or immediately after `BookParser.parseMetadata` returns an `Ebook`.
- Have `LibraryRepository.syncLibrary()` (in [app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt](app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt)) persist TOC entries via `TocDao`. Use transaction semantics: remove existing TOC rows for the book then insert the new list.

6. Reader: prefer DB-stored TOCs with fallback
- Update [app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt](app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt) to load TOC rows from `TocDao.getForBook(bookId)` and map them to the navigation UI (reuse existing mapping to publication positions). If no DB TOCs are present, fall back to the current runtime TOC extraction path.

7. Tests and CI
- Add unit tests:
  - `TocParserTest` parsing sample EPUBs (happy / missing-nav / NCX fallback).
  - `TocDaoTest` in-memory Room tests for insert/get/delete.
  - `LibraryRepositoryTest` integration test verifying TOCs are saved during `syncLibrary()`.
- Run `.`\gradlew.bat clean assembleDebug` and `.`\gradlew.bat test` locally as verification steps.

8. Performance & operational choices
- Option A (first iteration, recommended): parse TOC synchronously during scan for immediate availability.
- Option B (future improvement): offload TOC extraction to a small background queue and mark `EbookEntity` with a `tocImported` flag to avoid reprocessing. Add instrumentation (timings/logging) to measure cost before moving to async.

**Relevant files**
- [app/src/main/java/com/nosferatu/launcher/data/database/AppDatabase.kt](app/src/main/java/com/nosferatu/launcher/data/database/AppDatabase.kt) — bump version, add `TocEntryEntity`.
- [app/src/main/java/com/nosferatu/launcher/data/BookDao.kt](app/src/main/java/com/nosferatu/launcher/data/BookDao.kt) — existing DAO (no change required unless you prefer adding TOC methods here).
- [app/src/main/java/com/nosferatu/launcher/data/EbookEntity.kt](app/src/main/java/com/nosferatu/launcher/data/EbookEntity.kt) — existing book entity; consider adding `tocImported: Boolean` (optional).
- [app/src/main/java/com/nosferatu/launcher/data/TocEntryEntity.kt](app/src/main/java/com/nosferatu/launcher/data/TocEntryEntity.kt) — new entity to add.
- [app/src/main/java/com/nosferatu/launcher/data/TocDao.kt](app/src/main/java/com/nosferatu/launcher/data/TocDao.kt) — new DAO to add.
- [app/src/main/java/com/nosferatu/launcher/parser/TocParser.kt](app/src/main/java/com/nosferatu/launcher/parser/TocParser.kt) — new parser to add.
- [app/src/main/java/com/nosferatu/launcher/parser/BookParser.kt](app/src/main/java/com/nosferatu/launcher/parser/BookParser.kt) — reuse for wiring / supportedFormats check.
- [app/src/main/java/com/nosferatu/launcher/parser/EpubParser.kt](app/src/main/java/com/nosferatu/launcher/parser/EpubParser.kt) — reuse for OPF/nav discovery.
- [app/src/main/java/com/nosferatu/launcher/library/LibraryScanner.kt](app/src/main/java/com/nosferatu/launcher/library/LibraryScanner.kt) — call TocParser during scan.
- [app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt](app/src/main/java/com/nosferatu/launcher/repository/LibraryRepository.kt) — persist TOCs.
- [app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt](app/src/main/java/com/nosferatu/launcher/reader/ReaderActivity.kt) — read DB-first with fallback.
- tests: add `TocParserTest`, `TocDaoTest`, and `LibraryRepositoryTest` under `app/src/test/java`.

**Verification**
1. Build: `.`\gradlew.bat clean assembleDebug` should succeed.
2. Unit tests: `.`\gradlew.bat test` should pass for new tests.
3. Manual acceptance:
- Import or place a sample EPUB under the watched root.
- Run library sync; verify DB contains TOC rows (use debug logs or in-app inspection).
- Open Reader TOC: it should appear immediately from DB; if not, fallback runtime TOC should still work.

**Decisions & Trade-offs**
- Use destructive migration (current default) initially to minimize development friction; document risk and add a proper Room Migration if data preservation is required.
- Persist `locatorJson` as an opaque JSON string to avoid complex schema mappings and to preserve navigator locators across app versions.
- Keep TOC parsing logic parser-specific and extensible via a `TocParser` so other strategies (PDF/CBZ) can be added later.

**Further considerations**
1. Add `tocImported` boolean to `EbookEntity` to avoid reprocessing on repeated scans.
2. If TOC-to-position mapping is slow at runtime, consider precomputing publication positions when importing TOC (store locatorJson already aligned to publication positions) — may require Readium access at import time.
3. Add logging and simple metrics (parse time per book) to decide whether to move TOC parsing to background workers.

**Next steps (implementation checklist)**
1. Add `TocEntryEntity` + `TocDao` and bump DB version.
2. Implement `TocParser` with EPUB nav/NCX extraction and unit tests.
3. Wire scanner + repository to persist TOCs during `syncLibrary()`.
4. Update `ReaderActivity` to load DB-first and keep fallback.
5. Run build/tests; validate with sample EPUBs; tune sync vs async behavior based on timings.
Certo