package com.odom.seoulJobInfo

import FavoritePref
import SettingsPref
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.play.core.review.ReviewManagerFactory
import com.odom.seoulJobInfo.ui.theme.JobInfoTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : ComponentActivity() {

    object RetrofitClient {
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://openapi.seoul.go.kr:8088/${BuildConfig.API_KEY}/json/recMntList/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 36(Android 16)은 edge-to-edge가 강제되고 opt-out이 불가하므로,
        // 콘텐츠가 시스템 바 아래로 그려지도록 하고 Compose에서 인셋 패딩을 적용한다.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        MobileAds.initialize(this)
        setContent {
            JobInfoTheme {
                JobContent()
            }
        }
    }
}

val LocalTextScale = compositionLocalOf { 1.0f }

// 지역 필터 단위. 하나의 시/구가 실제 데이터에 등장하는 REGION_CD를 모두 담는다.
// (구가 있는 시는 데이터가 구 단위 코드로 오므로 시 코드 + 구 코드를 함께 포함)
data class Region(val name: String, val codes: List<String>)

private fun region(name: String, vararg codes: String) = Region(name, codes.toList())

fun Region.isSelectedIn(selected: Set<String>): Boolean = codes.any { it in selected }

fun Region.toggleIn(selected: Set<String>): Set<String> =
    if (isSelectedIn(selected)) selected - codes.toSet() else selected + codes

// 서울시 25개 자치구
val SEOUL_REGIONS: List<Region> = listOf(
    region("종로구", "11110"),
    region("중구", "11140"),
    region("용산구", "11170"),
    region("성동구", "11200"),
    region("광진구", "11215"),
    region("동대문구", "11230"),
    region("중랑구", "11260"),
    region("성북구", "11290"),
    region("강북구", "11305"),
    region("도봉구", "11320"),
    region("노원구", "11350"),
    region("은평구", "11380"),
    region("서대문구", "11410"),
    region("마포구", "11440"),
    region("양천구", "11470"),
    region("강서구", "11500"),
    region("구로구", "11530"),
    region("금천구", "11545"),
    region("영등포구", "11560"),
    region("동작구", "11590"),
    region("관악구", "11620"),
    region("서초구", "11650"),
    region("강남구", "11680"),
    region("송파구", "11710"),
    region("강동구", "11740")
)

// 경기도 (구가 있는 시는 시 코드 + 구 코드를 모두 포함)
val GYEONGGI_REGIONS: List<Region> = listOf(
    region("수원시", "41110", "41111", "41113", "41115", "41117"),
    region("성남시", "41130", "41131", "41133", "41135"),
    region("의정부시", "41150"),
    region("안양시", "41170", "41171", "41173"),
    region("부천시", "41190", "41192", "41194", "41196"),
    region("광명시", "41210"),
    region("평택시", "41220"),
    region("동두천시", "41250"),
    region("안산시", "41270", "41271", "41273"),
    region("고양시", "41280", "41281", "41285", "41287"),
    region("과천시", "41290"),
    region("구리시", "41310"),
    region("남양주시", "41360"),
    region("오산시", "41370"),
    region("시흥시", "41390"),
    region("군포시", "41410"),
    region("의왕시", "41430"),
    region("하남시", "41450"),
    region("용인시", "41460", "41461", "41463", "41465"),
    region("파주시", "41480"),
    region("이천시", "41500"),
    region("안성시", "41550"),
    region("김포시", "41570"),
    region("화성시", "41590", "41591", "41593", "41595", "41597"),
    region("광주시", "41610"),
    region("양주시", "41630"),
    region("포천시", "41650"),
    region("여주시", "41670"),
    region("연천군", "41800"),
    region("가평군", "41820"),
    region("양평군", "41830")
)

// 인천광역시
val INCHEON_REGIONS: List<Region> = listOf(
    region("제물포구", "28125"),
    region("영종구", "28155"),
    region("미추홀구", "28177"),
    region("연수구", "28185"),
    region("남동구", "28200"),
    region("부평구", "28237"),
    region("계양구", "28245"),
    region("서해구", "28275"),
    region("검단구", "28290"),
    region("강화군", "28710"),
    region("옹진군", "28720")
)

const val PAGE_SIZE = 1000

// recMntList는 요청당 최대 1,000행 + 서버 필터가 없어, 페이지를 나눠 순차로 받는다(무한 스크롤).
// 지역 필터는 받아온 목록에 client-side로 적용한다.
suspend fun fetchJobPage(service: ApiService, start: Int): List<JobInfo> {
    return try {
        service.getResult(start, start + PAGE_SIZE - 1).recMntList?.row.orEmpty()
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
fun JobContent() {
    val context = LocalContext.current

    var showExitDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var favoriteVersion by remember { mutableStateOf(0) }
    val favoritePref = remember { FavoritePref(context) }
    val settingsPref = remember { SettingsPref(context) }
    var textScale by remember { mutableStateOf(settingsPref.getTextScale()) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedRegions by remember { mutableStateOf(settingsPref.getSelectedRegions()) }

    // 전면(전체) 광고: 지역 드롭다운 선택을 3번 바꿀 때마다 노출
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var regionChangeCount by remember { mutableStateOf(settingsPref.getRegionAdCount()) }

    // 무한 스크롤 상태
    val scope = rememberCoroutineScope()
    val service = remember { MainActivity.RetrofitClient.create() }
    val jobs = remember { mutableStateListOf<JobInfo>() }
    var isInitialLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var nextStart by remember { mutableStateOf(1) }

    suspend fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        val start = nextStart
        val page = fetchJobPage(service, start)
        jobs.addAll(page)
        nextStart = start + PAGE_SIZE
        hasMore = page.size == PAGE_SIZE
        isLoadingMore = false
    }

    val exitBannerAdView = remember {
        AdView(context).apply {
            setAdSize(AdSize.MEDIUM_RECTANGLE)
            adUnitId = context.getString(R.string.TEST_Admob_BANNER_ID).trim()
            loadAd(AdRequest.Builder().build())
        }
    }

    fun loadInterstitial() {
        InterstitialAd.load(
            context,
            context.getString(R.string.TEST_Admob_FULLSCREEN_ID).trim(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun triggerInAppReview() {
        val activity = context as? Activity ?: return
        val reviewManager = ReviewManagerFactory.create(context)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(activity, task.result)
            }
        }
    }

    fun onFavoriteChanged(added: Boolean) {
        favoriteVersion++
        if (added) {
            val count = favoritePref.incrementAndGetAddCount()
            if (count == 3) triggerInAppReview()
        }
    }

    LaunchedEffect(Unit) {
        loadInterstitial()
        loadMore()
        isInitialLoading = false
    }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        ExitDialog(
            adView = exitBannerAdView,
            onDismiss = { showExitDialog = false },
            onExit = { (context as? ComponentActivity)?.finish() }
        )
    }

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

    CompositionLocalProvider(LocalTextScale provides textScale) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar가 자체 windowInsets로 상태 바 뒤까지 primary 색을 채운다
            // (→ 흰색 시스템 아이콘이 보임). 하단 내비게이션 바는 배너에서 따로 처리.
            Toolbar(onSettingsClick = { showSettings = true })
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
                val onRegionChange: (Set<String>) -> Unit = { newSelection ->
                    // 체크(추가)로 코드 수가 늘어난 경우만 카운트. 해제/전체는 제외.
                    val isCheck = newSelection.size > selectedRegions.size
                    selectedRegions = newSelection
                    settingsPref.saveSelectedRegions(newSelection)

                    // 체크를 3번 할 때마다 전면 광고 노출 (카운트는 저장되어 재실행 후에도 유지)
                    val activity = context as? Activity
                    val ad = interstitialAd
                    if (isCheck) {
                        regionChangeCount++
                        settingsPref.saveRegionAdCount(regionChangeCount)
                    }
                    if (isCheck && regionChangeCount % 3 == 0 && activity != null && ad != null) {
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                loadInterstitial() // 다음 광고 미리 로드
                            }

                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                interstitialAd = null
                                loadInterstitial()
                            }
                        }
                        ad.show(activity)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RegionFilterDropdown(
                        title = "서울",
                        regions = SEOUL_REGIONS,
                        selected = selectedRegions,
                        onSelectionChange = onRegionChange,
                        modifier = Modifier.weight(1f)
                    )
                    RegionFilterDropdown(
                        title = "경기",
                        regions = GYEONGGI_REGIONS,
                        selected = selectedRegions,
                        onSelectionChange = onRegionChange,
                        modifier = Modifier.weight(1f)
                    )
                    RegionFilterDropdown(
                        title = "인천",
                        regions = INCHEON_REGIONS,
                        selected = selectedRegions,
                        onSelectionChange = onRegionChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "글자를 길게 누르면 복사가 됩니다",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val filteredJobs = if (selectedRegions.isEmpty()) {
                    jobs.toList()
                } else {
                    jobs.filter { it.regionCd in selectedRegions }
                }
                val listState = rememberLazyListState()

                // 필터 결과가 아직 없고 더 받을 페이지가 남았으면 계속 불러온다
                // (희소한 지역만 선택했을 때 자동으로 다음 페이지를 끌어온다)
                LaunchedEffect(filteredJobs.isEmpty(), hasMore, isLoadingMore, selectedRegions) {
                    if (filteredJobs.isEmpty() && hasMore && !isLoadingMore) {
                        loadMore()
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isInitialLoading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                        filteredJobs.isEmpty() && !hasMore -> Text(
                            text = "선택한 지역의 공고가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        filteredJobs.isEmpty() -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                        else -> JobList(
                            infos = filteredJobs,
                            listLabel = "검색결과",
                            favoritePref = favoritePref,
                            onFavoriteChanged = ::onFavoriteChanged,
                            listState = listState,
                            isLoadingMore = isLoadingMore,
                            onLoadMore = { scope.launch { loadMore() } }
                        )
                    }
                }
            }

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

            // 하단 배너 광고 (두 탭 공통)
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = ctx.getString(R.string.TEST_Admob_BANNER_ID).trim()
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            )
        }
    }
    }
}

@Composable
fun ExitDialog(
    adView: AdView,
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "앱을 종료하시겠습니까?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    Button(
                        onClick = onExit,
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Text("종료") }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsSheet(
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
    onReviewClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val navBarHeight: Dp = remember {
        val activity = context as? Activity
        val insets = activity?.window?.decorView?.let { ViewCompat.getRootWindowInsets(it) }
        insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom
            ?.let { with(density) { it.toDp() } } ?: 0.dp
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp).padding(bottom = navBarHeight)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = "수도권 일자리") },
        colors = TopAppBarDefaults.topAppBarColors(
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

@Composable
fun RegionFilterDropdown(
    title: String,
    regions: List<Region>,
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCount = regions.count { it.isSelectedIn(selected) }
    val label = if (selectedCount == 0) title else "$title ($selectedCount)"
    val allCodes = regions.flatMap { it.codes }.toSet()

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label)
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "$title 지역 선택"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("$title 전체") },
                onClick = { onSelectionChange(selected - allCodes) },
                leadingIcon = {
                    Checkbox(checked = selectedCount == 0, onCheckedChange = null)
                }
            )
            regions.forEach { region ->
                DropdownMenuItem(
                    text = { Text(region.name) },
                    onClick = { onSelectionChange(region.toggleIn(selected)) },
                    leadingIcon = {
                        Checkbox(
                            checked = region.isSelectedIn(selected),
                            onCheckedChange = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun JobList(
    infos: List<JobInfo>,
    listLabel: String = "검색결과",
    favoritePref: FavoritePref,
    onFavoriteChanged: (added: Boolean) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null
) {
    // 리스트 끝에 가까워지면 다음 페이지를 불러온다
    if (onLoadMore != null) {
        LaunchedEffect(listState) {
            snapshotFlow {
                val layout = listState.layoutInfo
                val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 3) {
                    onLoadMore()
                }
            }
        }
    }

    Column {
        Text(
            text = "$listLabel : ${infos.size}개",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(infos) { info ->
                ExpandableCardView(
                    job = info,
                    favoritePref = favoritePref,
                    onFavoriteChanged = onFavoriteChanged
                )
            }
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ShortItem(job: JobInfo) {
    val scale = LocalTextScale.current
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle()
        .copy(fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.labelLarge.fontSize * scale)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
        .copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale)
    val text = buildAnnotatedString {
        fun field(label: String, value: String?, trailing: String = "\n") {
            withStyle(labelStyle) { append("$label: ") }
            withStyle(valueStyle) { append("${value ?: ""}$trailing") }
        }
        field("기업명", job.company)
        field("채용제목", job.title, "\n\n")
        field("근무지역", job.region)
        field("고용형태", job.empTpNm)
        field("임금조건", job.salTpNm, "\n\n")
        field("마감일", job.closeDt)
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun LongItem(job: JobInfo) {
    val scale = LocalTextScale.current
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle()
        .copy(fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.labelLarge.fontSize * scale)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
        .copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale)
    val text = buildAnnotatedString {
        fun field(label: String, value: String?, trailing: String = "\n") {
            withStyle(labelStyle) { append("$label: ") }
            withStyle(valueStyle) { append("${value ?: ""}$trailing") }
        }
        field("기업명", job.company)
        field("채용제목", job.title, "\n\n")
        field("근무지역", job.region)
        field("근무예정지", job.workRegion)
        field("경력", job.career)
        field("학력", listOfNotNull(job.minEdubg, job.maxEdubg).filter { it.isNotBlank() }.distinct().joinToString(" ~ "), "\n\n")
        field("업종", job.indTpCdNm)
        field("모집직종", job.jobsNm)
        field("모집인원", job.collectPsncnt)
        field("직무내용", job.jobCont, "\n\n")
        field("고용형태", job.empTpNm)
        field("근무시간/형태", job.workdayWorkhrCont)
        field("임금조건", job.salTpNm, "\n\n")
        field("전공", job.major)
        field("자격면허", job.certificate)
        field("병역특례채용희망", job.mltsvcExcHope)
        field("컴퓨터활용능력", job.compAbl)
        field("우대조건", job.pfCond, "\n\n")
        field("전형방법", job.selMthd)
        field("접수방법", job.rcptMthd)
        field("제출서류", job.submitDoc, "\n\n")
        field("4대보험", job.fourIns)
        field("퇴직금", job.retirepay)
        field("기타복리후생", job.etcWelfare, "\n\n")
        field("회사주소", job.corpAddr)
        field("채용부서", job.empChargerDpt)
        field("전화번호", job.contactTelno)
        field("등록일", job.regDt)
        field("마감일", job.closeDt)
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

fun shortItemText(job: JobInfo): String {
    val sb = StringBuilder()
    sb.append("기업명: ${job.company}\n")
    sb.append("채용제목: ${job.title}\n\n")
    sb.append("근무지역: ${job.region}\n")
    sb.append("고용형태: ${job.empTpNm}\n")
    sb.append("임금조건: ${job.salTpNm}\n\n")
    sb.append("마감일: ${job.closeDt}\n")
    return sb.toString()
}

fun longItemText(job: JobInfo): String {
    val sb = StringBuilder()
    sb.append("기업명: ${job.company}\n")
    sb.append("채용제목: ${job.title}\n\n")
    sb.append("근무지역: ${job.region}\n")
    sb.append("근무예정지: ${job.workRegion}\n")
    sb.append("경력: ${job.career}\n")
    sb.append("학력: ${job.minEdubg} ~ ${job.maxEdubg}\n\n")
    sb.append("업종: ${job.indTpCdNm}\n")
    sb.append("모집직종: ${job.jobsNm}\n")
    sb.append("모집인원: ${job.collectPsncnt}\n")
    sb.append("직무내용: ${job.jobCont}\n\n")
    sb.append("고용형태: ${job.empTpNm}\n")
    sb.append("근무시간/형태: ${job.workdayWorkhrCont}\n")
    sb.append("임금조건: ${job.salTpNm}\n\n")
    sb.append("전공: ${job.major}\n")
    sb.append("자격면허: ${job.certificate}\n")
    sb.append("병역특례채용희망: ${job.mltsvcExcHope}\n")
    sb.append("컴퓨터활용능력: ${job.compAbl}\n")
    sb.append("우대조건: ${job.pfCond}\n\n")
    sb.append("전형방법: ${job.selMthd}\n")
    sb.append("접수방법: ${job.rcptMthd}\n")
    sb.append("제출서류: ${job.submitDoc}\n\n")
    sb.append("4대보험: ${job.fourIns}\n")
    sb.append("퇴직금: ${job.retirepay}\n")
    sb.append("기타복리후생: ${job.etcWelfare}\n\n")
    sb.append("회사주소: ${job.corpAddr}\n")
    sb.append("채용부서: ${job.empChargerDpt}\n")
    sb.append("전화번호: ${job.contactTelno}\n")
    sb.append("등록일: ${job.regDt}\n")
    sb.append("마감일: ${job.closeDt}\n")
    return sb.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableCardView(
    job: JobInfo,
    favoritePref: FavoritePref,
    onFavoriteChanged: (added: Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(favoritePref.isFavorite(job.favoriteKey)) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = { isExpanded = !isExpanded },
                    onLongClick = { copyToClipboard(context, isExpanded, job) }
                )
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    val nowFavorite = !isFavorite
                    if (nowFavorite) favoritePref.add(job) else favoritePref.remove(job.favoriteKey)
                    isFavorite = nowFavorite
                    onFavoriteChanged(nowFavorite)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline
                )
            }
            if (isExpanded) LongItem(job) else ShortItem(job)
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
        }
    }
}

private fun copyToClipboard(context: Context, isExpanded: Boolean, job: JobInfo) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copyText = if (isExpanded) longItemText(job) else shortItemText(job)
    val clip = ClipData.newPlainText("Copied Text", copyText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "글자가 복사되었습니다", Toast.LENGTH_SHORT).show()
}
