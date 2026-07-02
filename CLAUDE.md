# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Single-module Android app (`com.odom.seoulJobInfo`, display name "JobInfo") that lists Seoul city job postings fetched from the Seoul Open Data API (`openapi.seoul.go.kr`, `GetJobInfo` dataset). UI is built entirely in Jetpack Compose with Material 3. All user-facing strings are Korean.

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

There is no ViewModel/repository layer. Networking, state, and UI all live in `MainActivity.kt`. Data flow:

1. **`MainActivity.RetrofitClient.create()`** — builds the Retrofit `ApiService`. The base URL is constructed as `http://openapi.seoul.go.kr:8088/${BuildConfig.API_KEY}/json/GetJobInfo/`, so every endpoint path in `ApiService` is appended after `GetJobInfo/`. Cleartext HTTP is enabled in the manifest (`usesCleartextTraffic="true"`) because the API is HTTP-only.

2. **`ApiService`** (`ApiService.kt`) — Retrofit interface. `getCustomResult(EDUCATION, STYLE, LOCATION, CAREER)` filters postings via URL `@Path` segments. The sentinel `"%20"` means "no filter" for any segment — this string is a literal path value, not a normal blank.

3. **`SearchPref`** (`app/src/main/java/SearchPref.kt`, **default package — no package declaration**) — thin `SharedPreferences` wrapper persisting the four filter values (Education, Style, Location, Career). Each getter defaults to `"%20"`, matching the API's no-filter sentinel. `MainActivity` imports it as bare `import SearchPref`.

4. **`ApiResult`** (`ApiResult.kt`) — Gson models mirroring the API's deeply nested JSON (`ApiResult` → `GetJobInfo` → `row: List<JobInfo>`). `JobInfo` fields use `@SerializedName` to map the API's uppercase snake_case keys (e.g. `CMPNY_NM`) to camelCase Kotlin properties. When adding a field, copy the exact API key into `@SerializedName`.

5. **UI (`MainActivity.kt`)** — Compose tree: `JobContent` → `Toolbar` + `LocationButton` + `JobList` → `ExpandableCardView`. State is local `remember`/`mutableStateOf`; the job list is fetched in `LaunchedEffect`/coroutines straight from `RetrofitClient`. `ExpandableCardView` toggles between `ShortItem` and `LongItem`; long-press copies the posting text to the clipboard via `copyToClipboard`.

### Things to watch

- Network calls force-unwrap (`...row!!`) and run on `Dispatchers.Default`; an API error or null payload will crash. There is no loading/error UI.
- `MainActivity.onCreate` calls `setContent` nested inside another `setContent` — pre-existing quirk, leave unless intentionally refactoring.
- `LocationButton` only persists/triggers the **Location** filter; Education/Style/Career are read from prefs but never set anywhere in the current UI.
- Display name and all UI copy are Korean; keep new strings consistent.

## Release / signing

Signing material lives at the repo root (`jobInfo.jks`, `private_key.pepk`) for Play App Signing. The release build type currently has `minifyEnabled false` and no `signingConfig` wired into Gradle — release signing is done outside the build script. `versionCode`/`versionName` are bumped manually in `app/build.gradle` (current: 3 / "1.2").
