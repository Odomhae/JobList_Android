# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Single-module Android app (`com.odom.seoulJobInfo`, display name "JobInfo") that lists Seoul-area job postings fetched from the Seoul Open Data API (`openapi.seoul.go.kr`, `recMntList` service — 서울시 일자리포털 채용정보, dataset OA-23047; sourced from 고용24, covers 서울·경기·인천). UI is built entirely in Jetpack Compose with Material 3 (light-only theme). Users can favorite postings, adjust text size, and copy posting text. Monetized with AdMob banners and prompts for Play Store reviews. All user-facing strings are Korean.

## Build & Run

Gradle wrapper is committed; use it for all commands (PowerShell on Windows):

```powershell
.\gradlew assembleDebug          # build debug APK
.\gradlew installDebug           # build + install to connected device/emulator
.\gradlew test                   # JVM unit tests (app/src/test)
.\gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
.\gradlew lint                   # Android lint
```

Run a single unit test class/method:

```powershell
.\gradlew test --tests "com.odom.seoulJobInfo.ExampleUnitTest.addition_isCorrect"
```

### Required: `local.properties` API key

The build **will fail** without an `API_KEY` entry in `local.properties` (read at configuration time in `app/build.gradle` and injected as `BuildConfig.API_KEY`). `local.properties` is git-ignored and must be created locally:

```properties
API_KEY=<seoul-open-data-api-key>
```

The key is embedded directly into the Retrofit base URL path, not sent as a header.

## Toolchain versions

- Android Gradle Plugin 8.5.0, Kotlin 1.7.20, Compose Compiler extension 1.3.2 (Compose BOM 2022.10.00)
- `compileSdk`/`targetSdk` 35, `minSdk` 24, Java/JVM 17
- Note: AGP/Kotlin/Compose versions are pinned and interdependent — bumping one usually requires bumping the others together.

## Architecture

There is no ViewModel/repository layer. Networking, state, and UI all live in `MainActivity.kt` (~700 lines). Data flow:

1. **`MainActivity.RetrofitClient.create()`** — builds the Retrofit `ApiService`. The base URL is constructed as `http://openapi.seoul.go.kr:8088/${BuildConfig.API_KEY}/json/recMntList/`, so every endpoint path in `ApiService` is appended after `recMntList/`. Cleartext HTTP is enabled in the manifest (`usesCleartextTraffic="true"`) because the API is HTTP-only.

2. **`ApiService`** (`ApiService.kt`) — Retrofit interface with a single call `getResult(START_INDEX, END_INDEX)` → `{START}/{END}` path segments. The `recMntList` service has **no server-side filter parameters** — only a paging window (max 1000 rows/request). `loadJobs()` requests `1/900`. Any filtering must be done client-side on the returned list.

3. **`ApiResult`** (`ApiResult.kt`) — Gson models mirroring the API's nested JSON (`ApiResult` → `recMntList: RecMntList` → `row: List<JobInfo>`). `JobInfo` has ~38 fields whose `@SerializedName` maps the API's uppercase keys (e.g. `COMPANY`, `TITLE`, `CLOSE_DT`) to camelCase properties; when adding a field, copy the exact API key. **The response has no unique posting ID**, so `JobInfo.favoriteKey` builds a composite (`COMPANY|TITLE|REG_DT|CLOSE_DT`) used as the favorites identity.

4. **State & data loading (`MainActivity.kt`)** — A `UiState` sealed interface (`Loading` / `Empty` / `Success(jobs)`) drives the list. The top-level `suspend fun loadJobs(context)` reads the filters from `SearchPref`, calls `getCustomResult`, and returns `Empty` on any exception or null payload (`getJobInfo?.row.orEmpty()`) — errors are swallowed, not crashed. Called from `LaunchedEffect` on first load and from a coroutine after each filter change. All state is local `remember`/`mutableStateOf` in `JobContent`.

5. **UI tree (`MainActivity.kt`)** — `MainActivity.onCreate` wraps `JobContent` in `JobInfoTheme`. `JobContent` renders `Toolbar` (with a Settings action) + a two-tab `TabRow` ("전체" / "★ 즐겨찾기") + a bottom banner ad. The "전체" tab shows the full job list; the "즐겨찾기" tab shows saved postings from `FavoritePref`. Both lists render via `JobList` → `ExpandableCardView`, which toggles between `ShortItem` (compact) and `LongItem` (full detail), has a favorite star, and long-press copies the posting text via `copyToClipboard` (using the parallel plain-text builders `shortItemText`/`longItemText`). There is no filter UI — the `recMntList` service does not support server-side filtering.

### SharedPreferences wrappers (default package)

Two thin wrappers live directly under `app/src/main/java/` with **no package declaration** (default package), imported bare (e.g. `import FavoritePref`). Each uses its own prefs file:

- **`FavoritePref`** (`favoritePreferences`) — favorited `JobInfo` objects, serialized to a `StringSet` via Gson and de-duplicated by `JobInfo.favoriteKey` (a `COMPANY|TITLE|REG_DT|CLOSE_DT` composite, since the API has no unique posting ID). Also tracks `favoritesAddCount` via `incrementAndGetAddCount()`.
- **`SettingsPref`** (`settingsPreferences`) — the user text-scale factor (default `1.0f`), applied app-wide through the `LocalTextScale` `CompositionLocal` and multiplied into font sizes in `ShortItem`/`LongItem`.

### Ads & reviews

- AdMob is initialized in `onCreate`. Ad unit IDs come from `strings.xml`: `TEST_Admob_*` (Google's test IDs, currently wired into the code) and `REAL_Admob_*` (production). **The code currently references the `TEST_` IDs everywhere** — swap to `REAL_` before a production release. The app ID in `AndroidManifest.xml` points at `TEST_Admob_APP_ID`.
- Only banner ads are live: one at the bottom and one inside the exit-confirmation dialog (`ExitDialog`, triggered by `BackHandler`). The interstitial ad was removed when the district filter (its only trigger) was dropped — `TEST_Admob_FULLSCREEN_ID`/`REAL_Admob_FULLSCREEN_ID` are currently unused.
- In-app review (Play Core `ReviewManagerFactory`) is triggered from the Settings sheet and automatically after the user's 3rd favorite add.

### Things to watch

- Retrofit `suspend` calls run off the main thread automatically; `loadJobs` catches all exceptions and degrades to `UiState.Empty` (no error message distinct from "no results").
- Display name and all UI copy are Korean; keep new strings consistent.

## Release / signing

Signing material lives at the repo root (`jobInfo.jks`, `private_key.pepk`) for Play App Signing. The release build type currently has `minifyEnabled false` and no `signingConfig` wired into Gradle — release signing is done outside the build script. `versionCode`/`versionName` are bumped manually in `app/build.gradle` (current: 4 / "1.3"). A prebuilt bundle is committed at `app/release/app-release.aab`.
