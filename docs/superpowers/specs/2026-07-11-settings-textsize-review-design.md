# 설정 (글자 크기 · 앱 리뷰) Design

**Date:** 2026-07-11
**Status:** Approved
**Scope:** TopAppBar 우측 설정(⚙) 버튼 + 설정 바텀시트(카드 글자 크기 슬라이더, 앱 리뷰 남기기 버튼). 아키텍처 변경 없음.

## Goal

- TopAppBar 오른쪽 끝에 설정 버튼을 추가한다.
- 설정 바텀시트에서 카드 글자 크기를 슬라이더로 조절하고, 앱 리뷰를 남길 수 있다.
- 글자 크기는 앱 재시작 후에도 유지된다.

## Non-Goals

- 다크테마/기타 설정 항목 없음 (글자 크기 + 리뷰 버튼만).
- 카드 외 영역(툴바, 탭, 필터 버튼 등) 글자 크기 조절 없음 — 카드 본문(`ShortItem`/`LongItem`)만.
- 별도 설정 Activity/화면 없음 — 바텀시트로 완결.

## Section 1 — TopAppBar 설정 버튼

`Toolbar` 컴포저블 수정:

- `TopAppBar`의 `actions`에 `IconButton` 추가 — `Icons.Default.Settings`, `tint = onPrimary`.
- `contentDescription = "설정"`.
- `Toolbar(onSettingsClick: () -> Unit)` 시그니처로 변경, `JobContent`가 `showSettings = true`로 전환하는 콜백 전달.

## Section 2 — 설정 바텀시트

Material3 `ModalBottomSheet` 사용 (`@OptIn(ExperimentalMaterial3Api::class)` — Compose BOM 2022.10.00의 material3 버전에 `ModalBottomSheet`가 없으면 `Dialog` 기반 커스텀 시트로 대체한다. 구현 시 확인).

내용 (위에서 아래로):

1. **제목**: "설정" (`titleMedium`)
2. **글자 크기 레이블**: "글자 크기" (`labelLarge`)
3. **슬라이더**: `Slider(value, onValueChange, valueRange = 0.8f..1.6f, steps = 3)`
   - 4단계 스냅: 0.8 / 1.0 / 1.2 / 1.4 / 1.6 중 5개 값 (steps=3은 중간 3개 스냅 → 실제 5단계). 값 변경 즉시:
     - `SettingsPref.saveTextScale(value)` 저장
     - `textScale` 상태 갱신 → 카드 글자 실시간 반영
   - 슬라이더 양끝에 작은 "A" / 큰 "A" 텍스트 표시
4. **미리보기 텍스트**: "기업명칭: 미리보기" — 현재 스케일이 적용된 예시 한 줄
5. **앱 리뷰 버튼**: `OutlinedButton` "★ 앱 리뷰 남기기" — 기존 `triggerInAppReview()` 재사용

닫기: 시트 밖 탭 또는 스와이프 다운으로 dismiss (`showSettings = false`).

## Section 3 — 글자 크기 적용 및 저장

### 저장소: SettingsPref

`app/src/main/java/SettingsPref.kt` (default 패키지, `SearchPref` 패턴):

```
SettingsPref(context)
  ├── saveTextScale(value: Float)   // "textScale" 키에 저장
  └── getTextScale(): Float          // 기본값 1.0f
```

### 적용: CompositionLocal

```kotlin
val LocalTextScale = compositionLocalOf { 1.0f }
```

- `JobContent`에서 `textScale` 상태 보유 (초기값 = `SettingsPref.getTextScale()`).
- 콘텐츠 트리를 `CompositionLocalProvider(LocalTextScale provides textScale) { ... }`로 감싼다.
- `ShortItem`/`LongItem`에서 `LocalTextScale.current`를 읽어 적용:
  - `bodyMedium.fontSize * scale`, `labelLarge.fontSize * scale`을 `SpanStyle`에 반영.
- 파라미터 체인(`JobList` → `ExpandableCardView` → items) 추가 없이 전달.

## Affected Files

| 파일 | 변경 |
|------|------|
| `app/src/main/java/SettingsPref.kt` | 신규 — 글자 크기 저장 |
| `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt` | Toolbar 설정 버튼, 설정 바텀시트, LocalTextScale, ShortItem/LongItem 스케일 적용 |

`ApiService.kt`, `ApiResult.kt`, `SearchPref.kt`, `FavoritePref.kt`, `AndroidManifest.xml`, `app/build.gradle` 변경 없음 (리뷰 의존성은 이미 추가됨).

## Testing / Verification

- 빌드: `.\gradlew assembleDebug` 성공.
- 기기 수동 확인:
  - TopAppBar 우측에 ⚙ 아이콘 표시, 탭 시 바텀시트 열림.
  - 슬라이더 조절 시 미리보기와 카드 글자 크기가 즉시 바뀜.
  - 앱 재시작 후에도 조절한 글자 크기 유지.
  - "앱 리뷰 남기기" 탭 시 인앱 리뷰 플로우 요청 (Play Store 설치 환경에서 팝업 확인).
  - 시트 밖 탭/스와이프로 닫힘.
  - 기존 기능 회귀 없음: 탭 전환, 즐겨찾기, 필터, 광고, 뒤로가기 다이얼로그.
