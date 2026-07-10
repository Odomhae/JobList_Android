# 즐겨찾기 & 인앱 리뷰 Design

**Date:** 2026-07-11
**Status:** Approved
**Scope:** 공고 즐겨찾기(별표) 기능 + Google Play 인앱 리뷰 연동. 아키텍처 변경 없음 (ViewModel/repository 도입 안 함).

## Goal

- 사용자가 마음에 드는 공고에 별표를 눌러 저장하고, 별도 탭에서 모아볼 수 있다.
- 즐겨찾기를 3번 추가한 시점에 Google Play 인앱 리뷰 요청을 띄운다.

## Non-Goals

- 즐겨찾기 공고의 만료/갱신 알림 없음.
- 즐겨찾기 정렬/필터 없음.
- Room DB 도입 없음 (SharedPreferences + Gson으로 충분).
- 인앱 리뷰 팝업 강제 표시 없음 (Google 정책상 표시 여부는 Play가 결정).

## Section 1 — 저장소: FavoritePref

`app/src/main/java/FavoritePref.kt` (default 패키지, `SearchPref`와 동일한 위치·패턴).

```
FavoritePref(context)
  ├── add(job: JobInfo)          // JSON 직렬화 후 Set에 추가
  ├── remove(joReqstNo: String)  // joReqstNo 기준으로 제거
  ├── isFavorite(joReqstNo: String): Boolean
  ├── getAll(): List<JobInfo>    // 전체 즐겨찾기 역직렬화
  └── incrementAndGetAddCount(): Int  // 누적 추가 횟수 (인앱 리뷰 트리거용)
```

**저장 방식:**
- Gson으로 `JobInfo` → JSON 문자열 직렬화, `"favorites"` 키에 `Set<String>` 저장.
- `joReqstNo`를 고유 식별자로 사용. `add` 시 기존 동일 ID 항목을 먼저 제거 후 추가(덮어쓰기).
- 누적 추가 횟수는 `"favoritesAddCount"` 별도 키에 Int 저장.

**의존성:** Gson은 이미 `retrofit2:converter-gson`으로 포함되어 있어 추가 불필요.

**공고 영속성:** API에서 해당 공고가 더 이상 반환되지 않아도 저장된 `JobInfo` 데이터를 그대로 표시한다.

## Section 2 — UI

### 2-1. 탭 (TabRow)

Toolbar 바로 아래, 힌트 문구 위에 `TabRow` 2개:

```
[전체]  [★ 즐겨찾기]
```

- `selectedTab` 상태 (`0` = 전체, `1` = 즐겨찾기)로 콘텐츠 영역 전환.
- **즐겨찾기 탭에서는 필터 드롭다운(근무지)과 안내 문구를 숨긴다.** 저장된 공고는 API 필터와 무관하기 때문.
- 탭 전환 시 로딩/API 재호출 없음 — 즐겨찾기는 `FavoritePref.getAll()` 로컬 읽기.

### 2-2. 별표 버튼

각 `ExpandableCardView` 카드 하단 행(펼치기/접기 버튼이 있는 Row)의 왼쪽에 `IconButton` 추가:

```
[펼치기 ▾]  오른쪽 정렬               [☆]
          ↑ 기존 Modifier.align(End)
```

실제 배치: 하단 Row를 `Arrangement.SpaceBetween`으로 변경, 왼쪽에 별표, 오른쪽에 펼치기/접기.

- **☆** (미저장) / **★** (저장) 아이콘 토글.
- 탭 즉시 `FavoritePref.add`/`remove` 호출 후 UI 상태 업데이트.
- 즐겨찾기 탭에서 ☆로 해제하면 해당 카드가 목록에서 즉시 사라진다.

### 2-3. 즐겨찾기 탭 콘텐츠

- `FavoritePref.getAll()`로 읽어 기존 `ExpandableCardView` 목록으로 렌더링 (별도 컴포저블 불필요).
- 비어 있으면 중앙에 "저장된 공고가 없습니다" 안내 (`UiState.Empty`와 동일한 스타일).
- "검색결과 : N개" 대신 "저장된 공고 : N개"로 레이블 변경.

### 2-4. 상태 동기화

별표 토글 시 즐겨찾기 목록을 갱신하기 위해 `JobContent`에 `favoriteVersion: Int` 상태를 두고, 토글마다 +1. `FavoriteList`는 `favoriteVersion`을 key로 받아 매번 `FavoritePref.getAll()`을 재호출한다.

## Section 3 — 인앱 리뷰

**의존성 추가:**
```gradle
implementation 'com.google.android.play:review-ktx:2.0.1'
```

**트리거 조건:** 별표 추가 시 `FavoritePref.incrementAndGetAddCount()` 반환값이 `3`일 때 인앱 리뷰 요청 실행. 이후 카운터는 계속 증가하지만 리뷰 요청은 `count == 3`일 때만 실행 (한 번만).

**구현:**
```
val reviewManager = ReviewManagerFactory.create(context)
reviewManager.requestReviewFlow()
  .addOnCompleteListener { task ->
      if (task.isSuccessful) {
          reviewManager.launchReviewFlow(activity, task.result)
      }
  }
```

`context as? Activity` 캐스팅 — `JobContent`에서 이미 동일 패턴 사용 중 (전면광고 표시).

**주의:** Google Play In-App Review는 실제 Play Store에 배포된 앱에서만 팝업이 표시된다. 개발/테스트 환경에서는 조용히 실패한다 (크래시 없음).

## Affected Files

| 파일 | 변경 내용 |
|------|-----------|
| `app/src/main/java/FavoritePref.kt` | 신규 — 즐겨찾기 저장소 |
| `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt` | TabRow 추가, 즐겨찾기 탭 UI, 별표 버튼, 인앱 리뷰 트리거 |
| `app/build.gradle` | `play:review-ktx:2.0.1` 의존성 추가 |

`ApiService.kt`, `ApiResult.kt`, `SearchPref.kt`, `AndroidManifest.xml` 변경 없음.

## Testing / Verification

- 빌드: `.\gradlew assembleDebug` 성공.
- 기기 수동 확인:
  - 카드에 ☆ 버튼이 보이고, 탭 시 ★로 바뀌며 즐겨찾기 탭에 나타난다.
  - ★ 탭 재진입 시에도 목록이 유지된다 (앱 재시작 포함).
  - 즐겨찾기 탭에서 ☆로 해제하면 즉시 목록에서 사라진다.
  - 즐겨찾기 탭에서는 필터 드롭다운이 숨겨진다.
  - 3번째 별표 추가 시 인앱 리뷰 요청이 발화된다 (실기기 + Play Store 설치 환경에서 팝업 확인).
  - 기존 기능(펼치기/접기, 복사, 전면광고, 뒤로가기 다이얼로그) 회귀 없음.
- 기존 단위 테스트 통과: `.\gradlew test`.
