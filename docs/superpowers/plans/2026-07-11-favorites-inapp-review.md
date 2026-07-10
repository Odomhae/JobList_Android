# 즐겨찾기 & 인앱 리뷰 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 각 공고 카드에 별표(★/☆) 버튼을 추가해 즐겨찾기 저장·조회하고, 3번째 별표 추가 시 Google Play 인앱 리뷰를 요청한다.

**Architecture:** `FavoritePref`(default 패키지, SharedPreferences + Gson)가 저장소를 담당한다. `JobContent`는 `selectedTab`과 `favoriteVersion` 상태를 보유하고, `ExpandableCardView`로부터 `onFavoriteChanged(added: Boolean)` 콜백을 받아 목록 갱신과 인앱 리뷰 트리거를 처리한다.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Gson(기존), Google Play In-App Review API (`play:review-ktx:2.0.1`)

## Global Constraints

- `compileSdk`/`targetSdk` 35, `minSdk` 24, Kotlin 1.7.20, Compose BOM 2022.10.00
- default 패키지 파일(`FavoritePref.kt`)은 package 선언 없이 작성 (`SearchPref.kt`와 동일)
- 모든 UI 문자열은 한국어
- ViewModel/repository 도입 금지 — 상태는 `remember`/`mutableStateOf`로만 관리
- 기존 파일 변경 없음: `ApiService.kt`, `ApiResult.kt`, `SearchPref.kt`, `AndroidManifest.xml`

---

### Task 1: FavoritePref 저장소 + 의존성 추가

**Files:**
- Create: `app/src/main/java/FavoritePref.kt`
- Modify: `app/build.gradle` (review-ktx 의존성 추가)

**Interfaces:**
- Produces:
  - `FavoritePref(context: Context)`
  - `fun add(job: JobInfo)`
  - `fun remove(joReqstNo: String)`
  - `fun isFavorite(joReqstNo: String): Boolean`
  - `fun getAll(): List<JobInfo>`
  - `fun incrementAndGetAddCount(): Int`

- [ ] **Step 1: `app/build.gradle`에 의존성 추가**

`dependencies` 블록 안 retrofit 줄 아래에 추가:

```gradle
implementation 'com.google.android.play:review-ktx:2.0.1'
```

- [ ] **Step 2: `FavoritePref.kt` 작성**

`app/src/main/java/FavoritePref.kt` 신규 생성 (package 선언 없음):

```kotlin
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.odom.seoulJobInfo.JobInfo

class FavoritePref(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("favoritePreferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun add(job: JobInfo) {
        val id = job.joReqstNo ?: return
        val current = loadRawSet().toMutableSet()
        current.removeAll { gson.fromJson(it, JobInfo::class.java).joReqstNo == id }
        current.add(gson.toJson(job))
        prefs.edit().putStringSet("favorites", current).apply()
    }

    fun remove(joReqstNo: String) {
        val current = loadRawSet().toMutableSet()
        current.removeAll { gson.fromJson(it, JobInfo::class.java).joReqstNo == joReqstNo }
        prefs.edit().putStringSet("favorites", current).apply()
    }

    fun isFavorite(joReqstNo: String): Boolean =
        loadRawSet().any { gson.fromJson(it, JobInfo::class.java).joReqstNo == joReqstNo }

    fun getAll(): List<JobInfo> =
        loadRawSet().mapNotNull { runCatching { gson.fromJson(it, JobInfo::class.java) }.getOrNull() }

    fun incrementAndGetAddCount(): Int {
        val next = prefs.getInt("favoritesAddCount", 0) + 1
        prefs.edit().putInt("favoritesAddCount", next).apply()
        return next
    }

    private fun loadRawSet(): Set<String> =
        prefs.getStringSet("favorites", emptySet()) ?: emptySet()
}
```

- [ ] **Step 3: 빌드 확인**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```powershell
git add app/src/main/java/FavoritePref.kt app/build.gradle
git commit -m "feat: add FavoritePref storage and play review dependency"
```

---

### Task 2: TabRow UI + 즐겨찾기 탭 콘텐츠

**Files:**
- Modify: `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt`
  - `JobContent`: `selectedTab`, `favoriteVersion`, `favoritePref` 상태 추가; TabRow 추가; 탭에 따라 필터/안내문구 show/hide; 즐겨찾기 탭 콘텐츠 렌더링
  - `JobList`: `listLabel` 파라미터 추가, `favoritePref` + `onFavoriteChanged` 파라미터 추가

**Interfaces:**
- Consumes (Task 1): `FavoritePref(context)`, `getAll()`, `isFavorite()`, `add()`, `remove()`, `incrementAndGetAddCount()`
- Produces:
  - `JobContent` 수정본 — `selectedTab: Int`, `favoriteVersion: Int`, `favoritePref: FavoritePref` 상태 보유
  - `JobList(infos, listLabel, favoritePref, onFavoriteChanged)` 수정 시그니처

- [ ] **Step 1: 임포트 추가**

`MainActivity.kt` 임포트 블록에 추가:

```kotlin
import FavoritePref
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.google.android.play.core.review.ReviewManagerFactory
```

- [ ] **Step 2: `JobContent`에 상태 및 TabRow 추가**

`JobContent` 함수에서 기존 상태 선언 아래에 추가:

```kotlin
var selectedTab by remember { mutableStateOf(0) }
var favoriteVersion by remember { mutableStateOf(0) }
val favoritePref = remember { FavoritePref(context) }
```

인앱 리뷰 트리거 함수를 `loadInterstitial()` 선언 아래에 추가:

```kotlin
fun triggerInAppReview() {
    val activity = context as? Activity ?: return
    val reviewManager = ReviewManagerFactory.create(context)
    reviewManager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            reviewManager.launchReviewFlow(activity, task.result)
        }
    }
}
```

즐겨찾기 변경 핸들러를 `triggerInAppReview()` 아래에 추가:

```kotlin
fun onFavoriteChanged(added: Boolean) {
    favoriteVersion++
    if (added) {
        val count = favoritePref.incrementAndGetAddCount()
        if (count == 3) triggerInAppReview()
    }
}
```

- [ ] **Step 3: `Surface` 안의 `Column`을 수정 — Toolbar 아래에 TabRow 삽입**

기존:
```kotlin
Surface(color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize()) {
        Toolbar()
        Text(
            text = "글자를 길게 누르면 복사가 됩니다",
```

교체:
```kotlin
Surface(color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize()) {
        Toolbar()
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("전체") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("★ 즐겨찾기") }
            )
        }
        if (selectedTab == 0) {
            Text(
                text = "글자를 길게 누르면 복사가 됩니다",
```

- [ ] **Step 4: 전체 탭 콘텐츠 블록 닫기 + 즐겨찾기 탭 블록 추가**

`if (selectedTab == 0)` 블록은 기존 힌트 문구부터 하단 배너 `AndroidView` 직전까지를 감싼다. 하단 배너는 두 탭 모두에서 표시하므로 if 블록 밖에 둔다.

기존 `FilterRow(...)` 호출의 `onFilterChanged` 람다 안 콜백에 `favoritePref`와 `onFavoriteChanged`를 아직 전달하지 않아도 됨 (Task 3에서 수정).

`if (selectedTab == 0) { ... }` 닫는 괄호 직후, 하단 배너 `AndroidView` 직전에 추가:

```kotlin
if (selectedTab == 1) {
    val favorites = remember(favoriteVersion) { favoritePref.getAll() }
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (favorites.isEmpty()) {
            Text(
                text = "저장된 공고가 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            JobList(
                infos = favorites,
                listLabel = "저장된 공고",
                favoritePref = favoritePref,
                onFavoriteChanged = ::onFavoriteChanged
            )
        }
    }
}
```

- [ ] **Step 5: `JobList` 시그니처 수정**

기존:
```kotlin
@Composable
fun JobList(infos: List<JobInfo>) {
    Column {
        Text(
            text = "검색결과 : ${infos.size}개",
```

교체:
```kotlin
@Composable
fun JobList(
    infos: List<JobInfo>,
    listLabel: String = "검색결과",
    favoritePref: FavoritePref,
    onFavoriteChanged: (added: Boolean) -> Unit
) {
    Column {
        Text(
            text = "$listLabel : ${infos.size}개",
```

`JobList` 내부의 `items(infos)` 블록도 수정:

기존:
```kotlin
items(infos) { info ->
    ExpandableCardView(info)
}
```

교체:
```kotlin
items(infos) { info ->
    ExpandableCardView(
        job = info,
        favoritePref = favoritePref,
        onFavoriteChanged = onFavoriteChanged
    )
}
```

- [ ] **Step 6: 전체 탭의 `JobList` 호출에도 파라미터 전달**

기존 (`is UiState.Success -> JobList(state.jobs)` 부분):
```kotlin
is UiState.Success -> JobList(state.jobs)
```

교체:
```kotlin
is UiState.Success -> JobList(
    infos = state.jobs,
    favoritePref = favoritePref,
    onFavoriteChanged = ::onFavoriteChanged
)
```

- [ ] **Step 7: 빌드 확인 (ExpandableCardView 시그니처 불일치로 에러 예상 — 다음 Task에서 수정)**

```powershell
.\gradlew assembleDebug 2>&1 | Select-String "error:"
```

Expected: `ExpandableCardView` 관련 unresolved reference 에러. Task 3에서 수정한다.

---

### Task 3: 별표 버튼 + ExpandableCardView 수정

**Files:**
- Modify: `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt`
  - `ExpandableCardView`: 파라미터 추가, 하단 Row에 별표 `IconButton` 추가
  - 임포트: `Icons.Filled.Star`

**Interfaces:**
- Consumes (Task 1): `FavoritePref.isFavorite()`, `FavoritePref.add()`, `FavoritePref.remove()`
- Consumes (Task 2): `onFavoriteChanged(added: Boolean)` 콜백 시그니처

- [ ] **Step 1: `Icons.Filled.Star` 임포트 추가**

```kotlin
import androidx.compose.material.icons.filled.Star
```

- [ ] **Step 2: `ExpandableCardView` 시그니처 수정**

기존:
```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableCardView(job: JobInfo) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
```

교체:
```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableCardView(
    job: JobInfo,
    favoritePref: FavoritePref,
    onFavoriteChanged: (added: Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(favoritePref.isFavorite(job.joReqstNo ?: "")) }
    val context = LocalContext.current
```

- [ ] **Step 3: 카드 하단 Row를 별표 버튼 + 펼치기/접기 배치로 교체**

`ExpandableCardView` 안의 기존 `TextButton` 블록:

```kotlin
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = if (isExpanded) "접기" else "펼치기")
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
```

교체:

```kotlin
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val nowFavorite = !isFavorite
                    if (nowFavorite) favoritePref.add(job) else favoritePref.remove(job.joReqstNo ?: "")
                    isFavorite = nowFavorite
                    onFavoriteChanged(nowFavorite)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.outline
                    )
                }
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(text = if (isExpanded) "접기" else "펼치기")
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
```

- [ ] **Step 4: `IconButton` 임포트 추가**

```kotlin
import androidx.compose.material3.IconButton
```

- [ ] **Step 5: 빌드 확인**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 기기/에뮬레이터 수동 검증**

```powershell
.\gradlew installDebug
```

체크리스트:
- [ ] 카드 하단 왼쪽에 ☆(회색) 아이콘 표시
- [ ] 탭 시 ★(파란색)로 바뀌고 "★ 즐겨찾기" 탭에 해당 공고 나타남
- [ ] 앱 재시작 후 즐겨찾기 탭에 저장된 공고 유지
- [ ] 즐겨찾기 탭에서 ★ 탭 시 즉시 목록에서 사라짐
- [ ] 즐겨찾기 탭에서 필터 드롭다운과 안내 문구 숨겨짐
- [ ] "저장된 공고 : N개" 레이블 정상 표시
- [ ] 3번째 별표 추가 시 인앱 리뷰 요청 발화 (실기기 Play Store 환경)
- [ ] 기존 기능 회귀 없음: 펼치기/접기, 길게 눌러 복사, 전면광고, 뒤로가기 다이얼로그

- [ ] **Step 7: 커밋**

```powershell
git add app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt
git commit -m "feat: add favorites tab with star button and in-app review trigger"
```
