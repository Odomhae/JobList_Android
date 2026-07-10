package com.odom.seoulJobInfo

import FavoritePref
import SearchPref
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Slider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
                .baseUrl("http://openapi.seoul.go.kr:8088/${BuildConfig.API_KEY}/json/GetJobInfo/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContent {
            JobInfoTheme {
                JobContent()
            }
        }
    }
}

val LocalTextScale = compositionLocalOf { 1.0f }

sealed interface UiState {
    object Loading : UiState
    object Empty : UiState
    data class Success(val jobs: List<JobInfo>) : UiState
}

suspend fun loadJobs(context: Context): UiState {
    return try {
        val pref = SearchPref(context)
        val jobs = MainActivity.RetrofitClient.create()
            .getCustomResult(pref.getEducation(), pref.getStyle(), pref.getLocation(), pref.getCareer())
            .getJobInfo?.row.orEmpty()
        if (jobs.isEmpty()) UiState.Empty else UiState.Success(jobs)
    } catch (_: Exception) {
        UiState.Empty
    }
}

@Composable
fun JobContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    var showExitDialog by remember { mutableStateOf(false) }
    var filterChangeCount by remember { mutableStateOf(0) }
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var favoriteVersion by remember { mutableStateOf(0) }
    val favoritePref = remember { FavoritePref(context) }
    val settingsPref = remember { SettingsPref(context) }
    var textScale by remember { mutableStateOf(settingsPref.getTextScale()) }
    var showSettings by remember { mutableStateOf(false) }

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
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
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
        uiState = loadJobs(context)
        loadInterstitial()
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
                Text(
                    text = "글자를 길게 누르면 복사가 됩니다",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                FilterRow(
                    onFilterChanged = {
                        uiState = UiState.Loading
                        filterChangeCount++
                        if (filterChangeCount % 3 == 0) {
                            val activity = context as? Activity
                            val ad = interstitialAd
                            if (activity != null && ad != null) {
                                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        interstitialAd = null
                                        loadInterstitial()
                                    }
                                }
                                ad.show(activity)
                            }
                        }
                        scope.launch { uiState = loadJobs(context) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (val state = uiState) {
                        is UiState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                        is UiState.Empty -> Text(
                            text = "검색 결과가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        is UiState.Success -> JobList(
                            infos = state.jobs,
                            listLabel = "검색결과",
                            favoritePref = favoritePref,
                            onFavoriteChanged = ::onFavoriteChanged
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
                modifier = Modifier.fillMaxWidth()
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

@Composable
fun FilterDropdown(
    placeholder: String,
    options: List<String>,
    savedValue: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf(if (savedValue != "%20") savedValue else placeholder) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(text = label)
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("전체") },
                onClick = {
                    label = placeholder
                    expanded = false
                    onSelected("%20")
                }
            )
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        label = item
                        expanded = false
                        onSelected(item)
                    }
                )
            }
        }
    }
}

@Composable
fun FilterRow(onFilterChanged: () -> Unit) {
    val context = LocalContext.current
    val pref = remember { SearchPref(context) }

    val locations = listOf(
        "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구",
        "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구",
        "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
    )

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        FilterDropdown("근무지", locations, pref.getLocation()) { v ->
            pref.saveLocation(v); onFilterChanged()
        }
    }
}

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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )
        LazyColumn(
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
        field("기업명칭", job.cmpnyNm)
        field("사업요약", job.bsnsSumryCn)
        field("모집요강", job.guiLn, "\n\n")
        field("근무시간", job.workTimeNm)
        field("공휴일", job.holidayNm, "\n\n")
        field("마감일", job.rceptClosNm)
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
        field("기업명칭", job.cmpnyNm)
        field("사업요약", job.bsnsSumryCn)
        field("모집요강", job.guiLn, "\n\n")
        field("근무시간", job.workTimeNm)
        field("공휴일", job.holidayNm, "\n\n")
        field("마감일", job.rceptClosNm, "\n\n")
        field("구인제목", job.joSj)
        field("근무예정지", job.workPararBassAdresCn)
        field("직무내용", job.dtyCn, "\n\n")
        field("급여조건", job.hopeWage, "\n\n")
        field("담당 상담사명", job.mngrNm)
        field("담당 상담사 전화번호", job.mngrPhonNo)
        field("담당 상담사 소속기관명", job.mngrInsttNm, "\n\n")
        field("기업 주소", job.bassAdresCn)
        field("구인신청번호", job.joReqstNo)
        field("구인등록번호", job.joRegistNo)
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

fun shortItemText(job: JobInfo): String {
    val sb = StringBuilder()
    sb.append("기업명칭: ${job.cmpnyNm}\n")
    sb.append("사업요약내용: ${job.bsnsSumryCn}\n")
    sb.append("모집요강: ${job.guiLn}\n\n")
    sb.append("근무시간: ${job.workTimeNm}\n")
    sb.append("공휴일: ${job.holidayNm}\n\n")
    sb.append("마감일: ${job.rceptClosNm}\n")
    return sb.toString()
}

fun longItemText(job: JobInfo): String {
    val sb = StringBuilder()
    sb.append("기업명칭: ${job.cmpnyNm}\n")
    sb.append("사업요약내용: ${job.bsnsSumryCn}\n")
    sb.append("모집요강: ${job.guiLn}\n\n")
    sb.append("근무시간: ${job.workTimeNm}\n")
    sb.append("공휴일: ${job.holidayNm}\n\n")
    sb.append("마감일: ${job.rceptClosNm}\n")
    sb.append("구인제목: ${job.joSj}\n")
    sb.append("근무예정지: ${job.workPararBassAdresCn}\n")
    sb.append("직무내용: ${job.dtyCn}\n\n")
    sb.append("급여조건: ${job.hopeWage}\n")
    sb.append("담당 상담사명: ${job.mngrNm}\n")
    sb.append("담당 상담사 전화번호: ${job.mngrPhonNo}\n")
    sb.append("담당 상담사 소속기관명: ${job.mngrInsttNm}\n\n")
    sb.append("기업 주소: ${job.bassAdresCn}\n")
    sb.append("구인신청번호: ${job.joReqstNo}\n")
    sb.append("구인등록번호: ${job.joRegistNo}\n")
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
    var isFavorite by remember { mutableStateOf(favoritePref.isFavorite(job.joReqstNo ?: "")) }
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
                    indication = rememberRipple(bounded = true),
                    onClick = { isExpanded = !isExpanded },
                    onLongClick = { copyToClipboard(context, isExpanded, job) }
                )
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    val nowFavorite = !isFavorite
                    if (nowFavorite) favoritePref.add(job) else favoritePref.remove(job.joReqstNo ?: "")
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
