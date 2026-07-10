# Filter UI Design — 학력·고용형태·경력 필터 추가

**Date:** 2026-07-10
**Status:** Approved (design phase)
**Scope:** 기존 근무지 드롭다운과 같은 패턴으로 학력/고용형태/경력 필터 UI를 추가. 아키텍처 변경 없음 (ViewModel/repository 도입 안 함).

## Goal

`ApiService.getCustomResult`와 `SearchPref`는 이미 네 가지 필터(근무지·학력·고용형태·경력)를 지원하지만, UI에는 근무지 드롭다운만 존재한다. 나머지 세 필터의 UI를 추가해 사용자가 조건을 좁혀 검색할 수 있게 한다.

선택된 UI 방식(A안): 기존 `OutlinedButton` + `DropdownMenu` 패턴을 그대로 확장해 필터 버튼 4개를 가로 스크롤 한 줄에 배치. 선택 즉시 재조회.

## Non-Goals (out of scope)

- 키워드 검색, 정렬 기능 (별도 후속 작업).
- API, `ApiService` 경로, `SearchPref` 키, `"%20"` 무필터 센티널 변경 없음.
- 바텀시트/FilterChip 등 다른 UI 방식 (A안으로 확정).
- 네트워크 오류 복구 UI 추가 없음 (기존 `Empty` 처리 유지).

## Section 1 — 공통 필터 드롭다운 컴포넌트

현재 `LocationButton`을 일반화한 단일 컴포저블로 통합한다:

```
FilterDropdown(
    placeholder: String,      // 미선택 시 라벨 ("근무지", "학력", "고용형태", "경력")
    options: List<String>,    // 선택 가능한 값 목록
    savedValue: String,       // SearchPref에서 읽은 현재 값 ("%20" = 미선택)
    onSelected: (String) -> Unit  // 선택값 저장 + 재조회 트리거 ("%20" = 전체/해제)
)
```

- 모양·동작은 기존 근무지 버튼과 동일: `OutlinedButton` + 화살표 아이콘 + `DropdownMenu`.
- 근무지 버튼(`LocationButton`)도 이 컴포넌트로 교체해 코드 경로를 하나로 유지한다.
- 선택된 필터는 버튼 라벨에 선택값을 표시하고, 미선택(`"%20"`)이면 플레이스홀더를 표시한다.

## Section 2 — 레이아웃

필터 4개를 `horizontalScroll`이 적용된 한 줄 `Row`에 배치한다:

```
[근무지 ▾] [학력 ▾] [고용형태 ▾] [경력 ▾]   → 화면 폭 초과 시 가로 스크롤
```

- 순서: 근무지 → 학력 → 고용형태 → 경력.
- 기존 위치(안내 문구 아래, 결과 카운트 위) 유지. 버튼 간 간격과 좌우 패딩은 기존 스타일 시스템을 따른다.

## Section 3 — "전체" 해제 항목

각 드롭다운 목록 맨 위에 **"전체"** 항목을 추가한다:

- 선택 시 `SearchPref`에 `"%20"`(API 무필터 센티널)을 저장하고 버튼 라벨을 플레이스홀더로 되돌린 뒤 재조회한다.
- 근무지에도 동일하게 적용 — 현재는 근무지를 한 번 고르면 해제할 방법이 없는데 이 문제도 함께 해결된다.

## Section 4 — 필터 값 목록

서울 열린데이터 API(`GetJobInfo`) 명세의 필드 값을 기준으로 하드코딩한다 (근무지 25개 구 목록과 같은 방식):

- **학력** (`ACDMCR_NM` 계열): 학력무관 / 초졸이하 / 중졸 / 고졸 / 대졸(2~3년) / 대졸(4년) / 석사 / 박사
- **고용형태** (`EMPLYM_STLE` 계열): 정규직 / 계약직 / 시간제 / 파견직 (후보 — 아래 검증에서 확정)
- **경력** (`CAREER_CND_NM` 계열): 관계없음 / 신입 / 경력

**검증 필수:** API가 실제로 받아들이는 값 문자열은 문서와 다를 수 있다. 구현 단계에서 각 후보 값으로 실제 API를 호출해 결과가 올바르게 필터링되는지 확인하고 최종 목록을 확정한다. 검증에서 동작하지 않는 값은 목록에서 제외한다. (한글 값이 URL 경로 세그먼트로 들어가는 방식은 근무지 필터에서 이미 검증됨 — 인코딩 문제 없음.)

## Section 5 — 데이터 흐름 / 상태

기존 인프라를 그대로 사용하며 새 저장소나 상태 구조는 추가하지 않는다:

- `SearchPref`는 이미 네 필터 모두의 저장/조회를 지원한다 (각 getter 기본값 `"%20"`).
- `loadJobs()`는 이미 네 값을 모두 `getCustomResult`에 전달한다.
- 필터 선택 시: `SearchPref`에 저장 → `UiState.Loading` → `loadJobs()` 재조회 → `Success`/`Empty`. 근무지 필터의 기존 흐름과 동일하다.

### 필터 유지 (persistence)

**선택한 필터는 앱을 종료했다가 다시 열어도 그대로 적용되고 표시되어야 한다:**

- 저장: 필터 선택 즉시 `SearchPref`(SharedPreferences)에 기록되므로 재시작 후에도 값이 남는다 (기존 동작).
- 적용: 앱 시작 시 첫 조회(`LaunchedEffect` → `loadJobs`)가 `SearchPref`에 저장된 네 필터 값을 그대로 사용하므로, 마지막으로 선택한 조건의 결과가 바로 표시된다.
- 표시: 각 `FilterDropdown`은 시작 시 `SearchPref`에서 저장값을 읽어 버튼 라벨을 복원한다 — 저장값이 `"%20"`이 아니면 그 값을, `"%20"`이면 플레이스홀더를 표시한다. (현재 근무지 버튼이 이미 이 패턴을 사용하며, 새 필터 세 개에도 동일하게 적용한다.)

## Affected Files

- `app/src/main/java/com/odom/seoulJobInfo/MainActivity.kt` — `FilterDropdown` 공통 컴포넌트 도입, `LocationButton` 교체, 필터 Row 레이아웃, "전체" 해제 처리, 시작 시 라벨 복원.

`ApiService.kt`, `ApiResult.kt`, `SearchPref.kt`, `AndroidManifest.xml`, `app/build.gradle` 변경 없음.

## Testing / Verification

- 빌드: `.\gradlew assembleDebug` 성공.
- 필터 값 검증: 각 학력/고용형태/경력 후보 값으로 실제 API 호출 → 필터링된 결과 확인, 최종 목록 확정.
- 기기/에뮬레이터 수동 확인 (`.\gradlew installDebug`):
  - 각 필터 단독 선택 시 결과가 해당 조건으로 좁혀진다.
  - 필터 2개 이상 조합 시 조건이 함께 적용된다.
  - "전체" 선택 시 해당 필터가 해제되고 라벨이 플레이스홀더로 돌아온다.
  - **앱 완전 종료 후 재실행: 마지막 선택 필터가 버튼 라벨에 표시되고, 첫 조회 결과에도 그대로 적용된다.**
  - 필터 4개가 좁은 화면에서 가로 스크롤로 모두 접근 가능하다.
  - 기존 기능(펼치기/접기, 길게 눌러 복사, 로딩/빈 결과 상태) 회귀 없음.
- 기존 단위 테스트 통과 (`.\gradlew test`).
