# PLAN.md

## 1. Objective
Describe in 3-6 lines what must be implemented and why.
Include the product goal, not only the technical task.

Example:
Implement bookmark navigation in the reader so users can save positions, open a bookmark list, and jump back quickly without introducing heavy search or performance regressions.

---

## 2. Scope
List what is included and what is explicitly excluded.

### In scope
- Feature A
- Feature B
- UI change C

### Out of scope
- Full redesign
- Search/indexing
- Backend changes
- Unrelated refactors

---

## 3. Product intent
Explain the UX or business logic the implementation must preserve.

Questions this section should answer:
- What user problem is being solved?
- What should the feature feel like?
- What should the user understand immediately?
- What tradeoffs are intentional?

---

## 4. Codebase context
Give the coding agent just enough architecture to work safely.

Include:
- relevant modules
- relevant files
- current flow of data/state
- existing design system/theme hooks
- persistence layer involved
- known patterns already used in repo

Example:
- Reader screen uses XML + Readium + Compose overlay
- Theme depends on `LibraryConfig.backgroundMode`
- `ReaderActivity.setTheme()` must be called before `setContentView()`

---

## 5. Constraints
List hard technical constraints, performance constraints, compatibility constraints, and team rules.

Examples:
- Must support old Android devices
- No expensive background processing
- No DB migration unless explicitly justified
- Preserve existing resume-reading behavior
- Must work in all themes

---

## 6. Guardrails
State the “do not break” rules.

Examples:
- Do not change scan behavior
- Do not alter persistence format without documenting it
- Do not remove existing logging
- Do not move theme initialization order
- Do not introduce heavy dependencies

---

## 7. Current state
Describe the current implementation and what is wrong with it.

Useful format:
- Current component:
- Current behavior:
- Problem:
- Impact:

This helps the agent avoid guessing.

---

## 8. Target behavior
Describe the final behavior from the user’s perspective.

Use short bullet points:
- When user taps X, Y opens
- If state is active, icon becomes filled
- If data is empty, show empty state
- After success, show lightweight feedback

This section should read like a functional contract.

---

## 9. Implementation strategy
Explain the preferred technical direction before task breakdown.

Include:
- suggested persistence choice
- suggested UI primitives
- data identity strategy
- preferred reuse points
- acceptable simplifications for V1

Example:
Use SharedPreferences for V1 bookmark persistence unless a Room model already exists and can be reused safely.

---

## 10. Files to inspect first
Give the agent the smallest set of entry points.

Example:
- `app/src/main/java/.../ReaderActivity.kt`
- `app/src/main/res/layout/activity_reader.xml`
- `app/src/main/java/.../LibraryConfig.kt`

This speeds up execution and reduces wandering.

---

## 11. Proposed file changes
Split into:
- files to modify
- files to create
- files not to touch

### Modify
- `...`

### Create
- `...`

### Do not touch unless necessary
- `...`

---

## 12. Data model / state model
If the feature introduces state or persistence, define it clearly.

Include:
- fields
- ownership
- lifetime
- storage location
- identity rules

Example:
Bookmark:
- bookId
- locatorJson
- href
- progression
- createdAt

---

## 13. UI model
Describe the UI pieces that should exist.

Example:
- `BookmarkToggleButton`
- `BookmarkListPanel`
- `TocPanel`
- `EmptyBookmarksState`

For each, specify:
- purpose
- state inputs
- outputs/callbacks
- theme expectations

---

## 14. Task breakdown
This is the most important section for coding agents.

Split work into small ordered tasks.
Each task should be independently completable and testable.

Recommended structure:

### Task 1 — [short title]
**Goal**
What this task accomplishes.

**Files**
- `...`

**Work**
- step 1
- step 2
- step 3

**Done when**
- observable outcome 1
- observable outcome 2

Repeat for each task.

---

## 15. Suggested implementation order
Give the exact sequence.

Example:
1. Add persistence layer
2. Add UI state lookup
3. Wire icon state
4. Add panel
5. Add navigation callbacks
6. Polish empty states

This prevents random-order edits.

---

## 16. Edge cases
List failure modes and expected behavior.

Examples:
- No TOC available
- No bookmarks yet
- Invalid saved locator
- Book file missing
- Bookmark target no longer navigable
- Theme change while panel is open

---

## 17. Performance requirements
Be explicit.

Examples:
- No full text indexing
- No O(n) scan on every page turn if avoidable
- No expensive EPUB reparsing once publication is open
- UI updates must remain lightweight

---

## 18. Accessibility requirements
Include only concrete items the agent can implement.

Examples:
- contentDescription on icon buttons
- 48dp tap targets
- selected state not conveyed only by color
- readable contrast in all themes

---

## 19. Testing plan
Define how the implementation should be validated.

### Build checks
- `./gradlew assembleDebug`

### Manual checks
- Open reader
- Tap chapter button
- Navigate to chapter
- Add bookmark
- Reopen reader
- Verify bookmark persists

### Regression checks
- Existing resume-reading still works
- Theme switching still works

---

## 20. Acceptance criteria
Write crisp yes/no conditions.

Examples:
- Left button opens TOC
- Right button opens bookmark list
- Top-right button toggles bookmark
- Bookmark persists across restart
- No search is introduced
- Build passes

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

## 23. Open questions
Only include if there are still unresolved decisions.

Examples:
- Should bookmarks be newest-first or location-first?
- Should TOC and bookmarks be separate panels or tabs?
- Should bookmark preview include snippet text?

If these are unresolved, the agent should stop and ask before proceeding.