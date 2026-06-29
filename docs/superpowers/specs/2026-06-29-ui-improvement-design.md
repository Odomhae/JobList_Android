# UI Improvement Design — Seoul JobInfo App

**Date:** 2026-06-29
**Status:** Approved (design phase)
**Scope:** Visual refresh + targeted structural cleanup of the existing Compose UI. No architecture rewrite (no ViewModel/repository layer).

## Goal

Improve the app's UI on two axes the user identified:

- **A — Look:** the app looks dated/plain. The stock Material 3 purple theme is overridden by hardcoded `Color.Yellow` (toolbar), `Color.Black` (system bars), and `Color.DarkGray` (10dp dividers), so the screens fight their own theme.
- **D — Layout/structure:** the layout is awkward. `JobList` is rendered twice (once in `JobContent`, once inside `LocationButton`) from two competing `jobList` states, `onCreate` nests `setContent` inside `setContent`, and the toolbar title is misused as an instruction banner.

Chosen approach: **polish + targeted structural cleanup** — fix the root layout problems while restyling, staying single-module with state hoisted into composables (no ViewModel).

## Non-Goals (out of scope)

- No ViewModel/repository/architecture refactor.
- No full network error-recovery flow (no retry UI, no error banner). The existing `row!!` force-unwrap path is replaced only to the extent that a null/empty payload resolves to the `Empty` state instead of crashing.
- No change to the API, `ApiService` paths, `SearchPref` keys, or the `"%20"` no-filter sentinel.
- No change to displayed field content or Korean copy in the cards — styling only.
- Dynamic color is turned off by default but kept behind a flag (not removed).

## Section 1 — Visual System

**Color** (`ui/theme/Color.kt`, `ui/theme/Theme.kt`):
- Replace the purple template with a neutral, modern scheme built on a single restrained brand color: a calm **slate-blue** as `primary`, used for the top bar, filter control, and accents.
- Neutral greys for surface/background. Cards sit on a subtly tinted surface with a hairline outline — replacing the heavy `Color.DarkGray` 10dp divider.
- Define a proper **light and dark** `colorScheme`. Dark mode is currently broken in practice because screens hardcode colors; after this change both follow the theme.
- **Turn dynamic color off by default** (`dynamicColor = false`) so the app keeps a consistent identity instead of repainting from the wallpaper. Keep the flag so it can be re-enabled later.
- Stop hardcoding system bar color to `Color.Black` in `MainActivity`; let the theme drive system bars (the accompanist `systemuicontroller` call is reconciled with the theme's `primary`).

**Typography** (`ui/theme/Type.kt`):
- Define a real hierarchy: a clear **title** style (top bar / screen title), a **medium-weight label** style for field names (기업명칭, 마감일…), and a comfortable **body** style with generous line-height for dense Korean text.
- This replaces the ad-hoc inline `SpanStyle`/`fontSize` sizing scattered through `ShortItem`/`LongItem`.

## Section 2 — Layout & State Structure

**Single source of truth for the job list.** Remove the duplicate `jobList` state. Today it exists in `JobContent` (≈line 100) and again in `LocationButton` (≈line 175), with `JobList` rendered in both. Hoist a single `jobList` + UI state into `JobContent`; pass data down. `LocationButton` becomes a pure control: pick location → `SearchPref.saveLocation` → invoke a reload callback. It no longer renders a list.

**Single screen layout.** `JobContent` owns the structure top-to-bottom:
1. Top bar
2. Filter row (location control)
3. Content area — exactly **one** `JobList`, driven by UI state.

**Introduce `UiState`.** A simple sealed type held in `JobContent` via `remember`/`mutableStateOf`:
- `Loading`
- `Empty`
- `Success(jobs: List<JobInfo>)`

The fetch (currently the `LaunchedEffect` + `customSearch` coroutine) sets this state. A null/empty payload resolves to `Empty` rather than crashing.

**Clean up entry point.** Collapse the nested `setContent { JobInfoTheme { setContent { JobContent() } } }` in `onCreate` (≈lines 85–91) to a single `setContent`.

## Section 3 — Component Redesign

Uses the Section 1 visual system. Behavior and displayed content unchanged unless noted.

- **Top bar:** shows the app/screen title (e.g. "서울 일자리") in the brand color — no longer used as an instruction banner. The "글자를 길게 누르면 복사가 됩니다" hint moves to a slim helper line below the bar.
- **Filter row:** the location dropdown restyled as an outlined filter button/chip with the slate accent and a dropdown-arrow icon, clearly reading as "tap to choose 근무지." Same 25-구 list and selection behavior.
- **Result count:** "검색결과 : N개" kept, restyled as a calm section label (medium weight, secondary color, consistent padding) instead of bold 20sp.
- **Job card:** rounded `Card` on the tinted surface with hairline outline and gentle elevation; normal list spacing replaces the 10dp `DarkGray` divider. Field labels use the label style, values use body style — applied consistently across `ShortItem` and the expanded `LongItem` (currently inconsistent). Expand/collapse (펼치기/접기) becomes a quiet text button with a chevron. Long-press-to-copy and its toast are unchanged.
- `ShortItem`/`LongItem` and their `*Text` string builders keep the same fields and copy; only styling and the shared label/value treatment change.

## Section 4 — Loading & Empty States

Driven by `UiState`; the content area always shows something:

- **Loading:** centered circular progress indicator (brand color) during fetch — replaces today's blank screen on first open and after changing location.
- **Empty:** centered muted message ("검색 결과가 없습니다") when a search returns zero rows.
- **Success:** the styled `JobList` from Section 3.

**Flow:** changing location → `Loading` → fetch → resolve to `Success` or `Empty`. Null/empty payload → `Empty` (the only error hardening in scope). No new dependencies; all within the existing Compose tree.

## Affected Files

- `app/src/main/java/com/odom/seoulJobInfo/ui/theme/Color.kt` — new neutral palette.
- `app/src/main/java/com/odom/seoulJobInfo/ui/theme/Theme.kt` — light/dark schemes, dynamic color off by default, system-bar handling.
- `app/src/main/java/com/odom/seoulJobInfo/ui/theme/Type.kt` — typography hierarchy.
- `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt` — single `setContent`, hoisted `UiState`/`jobList`, single `JobList`, `LocationButton` as pure control, restyled composables, loading/empty UI.

No changes to `ApiService.kt`, `ApiResult.kt`, `SearchPref.kt`, `AndroidManifest.xml`, or `app/build.gradle`.

## Testing / Verification

- Build: `.\gradlew assembleDebug` succeeds.
- Manual on device/emulator (`.\gradlew installDebug`):
  - First launch shows a loading indicator, then either the styled list or the empty message.
  - Selecting each location reloads: loading → results/empty; the list renders **once** (no duplicate/competing list).
  - Expand/collapse works; long-press copies text and shows the toast.
  - Light and dark mode both follow the new theme (no stray yellow/black/dark-gray).
- Existing unit/instrumented tests still pass (`.\gradlew test`); no new tests required for a styling/structure change of this size.
