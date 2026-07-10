# 설정 (글자 크기 · 앱 리뷰) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** TopAppBar 우측 설정(⚙) 버튼과 설정 바텀시트(글자 크기 슬라이더 + 앱 리뷰 버튼)를 추가하고, 글자 크기를 SharedPreferences에 영속화한다.

**Architecture:** `SettingsPref`(default 패키지)가 글자 크기를 저장한다. `JobContent`가 `textScale`/`showSettings` 상태를 보유하고 `CompositionLocalProvider(LocalTextScale)`로 스케일을 카드에 전달한다. 바텀시트는 Material3 1.0.x에 `ModalBottomSheet`가 없으므로 `Dialog(usePlatformDefaultWidth = false)` + 하단 정렬 `Surface`로 구현한다.

**Tech Stack:** Kotlin 1.7.20, Jetpack Compose (BOM 2022.10.00, material3 1.0.x), Google Play In-App Review (이미 의존성 있음)

## Global Constraints

- `compileSdk`/`targetSdk` 35, `minSdk` 24, Kotlin 1.7.20, Compose BOM 2022.10.00 — **의존성 추가/변경 금지** (review-ktx는 이미 포함됨)
- default 패키지 파일(`SettingsPref.kt`)은 package 선언 없이 작성 (`SearchPref.kt`와 동일)
- 모든 UI 문자열은 한국어
- ViewModel/repository 도입 금지 — 상태는 `remember`/`mutableStateOf`로만 관리
- 글자 크기 적용 범위는 카드 본문(`ShortItem`/`LongItem`)만 — 툴바/탭/필터는 불변
- 슬라이더: `valueRange = 0.8f..1.6f`, `steps = 3` (0.8 / 1.0 / 1.2 / 1.4 / 1.6 5단계 스냅), 기본값 `1.0f`
- **커밋은 만들지 않는다 — 사용자가 요청할 때만 커밋** (이 프로젝트 사용자 선호)
- 기존 파일 변경 없음: `ApiService.kt`, `ApiResult.kt`, `SearchPref.kt`, `FavoritePref.kt`, `AndroidManifest.xml`, `app/build.gradle`
- 빌드 검증 시 이 터미널의 기본 JDK(Java 24)로는 Gradle이 실패한다. 빌드 전 `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` 설정 필요 (경로가 없으면 `Get-ChildItem "C:\Program Files\Android"`로 실제 설치 경로 확인)

---

### Task 1: SettingsPref 저장소

**Files:**
- Create: `app/src/main/java/SettingsPref.kt`

**Interfaces:**
- Produces:
  - `SettingsPref(context: Context)`
  - `fun saveTextScale(value: Float)`
  - `fun getTextScale(): Float` — 기본값 `1.0f`

- [ ] **Step 1: `SettingsPref.kt` 작성**

`app/src/main/java/SettingsPref.kt` 신규 생성 (package 선언 없음):

```kotlin
import android.content.Context
import android.content.SharedPreferences

class SettingsPref(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settingsPreferences", Context.MODE_PRIVATE)

    fun saveTextScale(value: Float) {
        prefs.edit().putFloat("textScale", value).apply()
    }

    fun getTextScale(): Float {
        return prefs.getFloat("textScale", 1.0f)
    }
}
```

- [ ] **Step 2: 빌드 확인**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

---

### Task 2: Toolbar 설정 버튼 + LocalTextScale + 카드 스케일 적용

**Files:**
- Modify: `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt`
  - 임포트 추가, `LocalTextScale` 정의, `Toolbar` 시그니처 변경, `JobContent` 상태 추가 및 `CompositionLocalProvider` 적용, `ShortItem`/`LongItem` 스케일 반영

**Interfaces:**
- Consumes (Task 1): `SettingsPref(context)`, `getTextScale()`, `saveTextScale(Float)`
- Produces:
  - `val LocalTextScale: ProvidableCompositionLocal<Float>` (top-level)
  - `Toolbar(onSettingsClick: () -> Unit)` 시그니처
  - `JobContent`의 상태: `showSettings: Boolean`, `textScale: Float`, `settingsPref: SettingsPref` — Task 3의 `SettingsSheet`가 사용

- [ ] **Step 1: 임포트 추가**

`MainActivity.kt` 임포트 블록에 추가:

```kotlin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
```

- [ ] **Step 2: `LocalTextScale` 정의**

`sealed interface UiState` 선언 위에 top-level로 추가:

```kotlin
val LocalTextScale = compositionLocalOf { 1.0f }
```

- [ ] **Step 3: `Toolbar`에 설정 버튼 추가**

기존:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar() {
    TopAppBar(
        title = { Text(text = "서울 일자리") },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
```

교체:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = "서울 일자리") },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "설정",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    )
}
```

- [ ] **Step 4: `JobContent`에 상태 추가**

`val favoritePref = remember { FavoritePref(context) }` 줄 아래에 추가:

```kotlin
val settingsPref = remember { SettingsPref(context) }
var textScale by remember { mutableStateOf(settingsPref.getTextScale()) }
var showSettings by remember { mutableStateOf(false) }
```

- [ ] **Step 5: `Toolbar()` 호출부 수정 + 콘텐츠를 CompositionLocalProvider로 감싸기**

기존:
```kotlin
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Toolbar()
```

교체:
```kotlin
    CompositionLocalProvider(LocalTextScale provides textScale) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Toolbar(onSettingsClick = { showSettings = true })
```

`Surface { ... }` 블록의 닫는 괄호(하단 배너 `AndroidView` 뒤, `JobContent` 함수 끝)에 짝이 되는 `}` 하나를 추가해 `CompositionLocalProvider` 블록을 닫는다:

기존 (JobContent 끝부분):
```kotlin
            // 하단 배너 광고 (두 탭 공통)
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = ctx.getString(R.string.TEST_Admob_BANNER_ID).trim()
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

교체:
```kotlin
            // 하단 배너 광고 (두 탭 공통)
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = ctx.getString(R.string.TEST_Admob_BANNER_ID).trim()
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    }
}
```

- [ ] **Step 6: `ShortItem`에 스케일 적용**

기존 (`ShortItem` 첫 두 줄):
```kotlin
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle().copy(fontWeight = FontWeight.SemiBold)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
```

교체:
```kotlin
    val scale = LocalTextScale.current
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle()
        .copy(fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.labelLarge.fontSize * scale)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
        .copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale)
```

- [ ] **Step 7: `LongItem`에 동일하게 적용**

`LongItem`의 첫 두 줄도 Step 6과 동일한 코드로 교체 (같은 두 줄이 두 함수에 각각 있음):

```kotlin
    val scale = LocalTextScale.current
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle()
        .copy(fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.labelLarge.fontSize * scale)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
        .copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale)
```

- [ ] **Step 8: 빌드 확인 (SettingsSheet 미구현이지만 이 시점까지는 컴파일 가능해야 함)**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` — `showSettings`는 아직 사용처가 없어 unused 경고만 발생 가능 (에러 아님)

---

### Task 3: SettingsSheet 바텀시트

**Files:**
- Modify: `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt`
  - `SettingsSheet` 컴포저블 신규 추가, `JobContent`에서 조건부 호출

**Interfaces:**
- Consumes (Task 2): `showSettings`, `textScale`, `settingsPref` 상태; 기존 `triggerInAppReview()` 함수
- Produces: `SettingsSheet(textScale, onTextScaleChange, onReviewClick, onDismiss)` 컴포저블

- [ ] **Step 1: 임포트 추가**

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.material3.Slider
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
```

- [ ] **Step 2: `SettingsSheet` 컴포저블 추가**

`ExitDialog` 컴포저블 아래에 추가:

```kotlin
@Composable
fun SettingsSheet(
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
    onReviewClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 전체 화면을 덮고, 시트 밖 탭 시 닫힘
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 시트 내부 탭은 닫히지 않도록 소비
                    ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "설정", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "글자 크기", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "A", fontSize = 12.sp)
                        Slider(
                            value = textScale,
                            onValueChange = onTextScaleChange,
                            valueRange = 0.8f..1.6f,
                            steps = 3,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(text = "A", fontSize = 20.sp)
                    }
                    Text(
                        text = "기업명칭: 미리보기",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * textScale
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onReviewClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "★ 앱 리뷰 남기기")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: `JobContent`에서 조건부 호출**

기존 `if (showExitDialog) { ... }` 블록 아래에 추가:

```kotlin
    if (showSettings) {
        SettingsSheet(
            textScale = textScale,
            onTextScaleChange = { value ->
                textScale = value
                settingsPref.saveTextScale(value)
            },
            onReviewClick = { triggerInAppReview() },
            onDismiss = { showSettings = false }
        )
    }
```

- [ ] **Step 4: 빌드 확인**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 기기/에뮬레이터 수동 검증**

```powershell
.\gradlew installDebug
```

체크리스트:
- [ ] TopAppBar 우측 끝에 ⚙(흰색) 아이콘 표시
- [ ] ⚙ 탭 시 하단에서 설정 시트 표시 (둥근 상단 모서리)
- [ ] 슬라이더 5단계 스냅 (0.8/1.0/1.2/1.4/1.6), 조절 시 "기업명칭: 미리보기"와 뒤의 카드 글자가 즉시 커지고 작아짐
- [ ] 앱 완전 종료 후 재실행: 조절한 글자 크기 유지
- [ ] "★ 앱 리뷰 남기기" 탭 시 크래시 없음 (Play Store 설치 환경에서만 팝업)
- [ ] 시트 바깥 영역 탭 시 닫힘, 시트 내부 탭 시 닫히지 않음
- [ ] 툴바/탭/필터 버튼 글자 크기는 변하지 않음
- [ ] 기존 기능 회귀 없음: 탭 전환, 즐겨찾기 별표, 필터, 하단 배너, 뒤로가기 다이얼로그

**참고:** 커밋은 사용자가 요청할 때만 수행한다.
