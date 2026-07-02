package com.odom.seoulJobInfo

import SearchPref
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
        setContent {
            JobInfoTheme {
                JobContent()
            }
        }
    }

}

/** Single source of truth for what the content area should render. */
sealed interface UiState {
    object Loading : UiState
    object Empty : UiState
    data class Success(val jobs: List<JobInfo>) : UiState
}

/** Fetches postings for the currently saved filters. A null/empty/failed payload resolves to [UiState.Empty]. */
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
    val systemUiController = rememberSystemUiController()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    var backPressedTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime > backPressedTime + 2000) {
            backPressedTime = currentTime
            Toast.makeText(context, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(color = primaryColor, darkIcons = false)
        systemUiController.setNavigationBarColor(color = surfaceColor)
    }

    LaunchedEffect(Unit) {
        uiState = loadJobs(context)
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Toolbar()
            Text(
                text = "글자를 길게 누르면 복사가 됩니다",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            LocationButton(
                onLocationSelected = {
                    uiState = UiState.Loading
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

                    is UiState.Success -> JobList(state.jobs)
                }
            }
        }
    }
}

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

@Composable
fun LocationButton(onLocationSelected: () -> Unit) {
    val context = LocalContext.current
    val searchValue = remember { SearchPref(context) }
    val items = listOf(
        "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구",
        "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구",
        "은평구", "종로구", "중구", "중랑구"
    )

    var isDropDownMenuExpanded by remember { mutableStateOf(false) }
    val saved = searchValue.getLocation()
    var locationText by remember { mutableStateOf(if (saved != "%20") saved else "근무지 선택") }

    Box {
        OutlinedButton(
            modifier = Modifier.padding(start = 16.dp),
            onClick = { isDropDownMenuExpanded = true }
        ) {
            Text(text = locationText)
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = isDropDownMenuExpanded,
            onDismissRequest = { isDropDownMenuExpanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        locationText = item
                        isDropDownMenuExpanded = false
                        searchValue.saveLocation(item)
                        onLocationSelected()
                    }
                )
            }
        }
    }
}

@Composable
fun JobList(infos: List<JobInfo>) {
    Column {
        Text(
            text = "검색결과 : ${infos.size}개",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(infos) { info ->
                ExpandableCardView(info)
            }
        }
    }
}

@Composable
fun ShortItem(job: JobInfo) {
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle().copy(fontWeight = FontWeight.SemiBold)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
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
    val labelStyle = MaterialTheme.typography.labelLarge.toSpanStyle().copy(fontWeight = FontWeight.SemiBold)
    val valueStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
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
fun ExpandableCardView(job: JobInfo) {
    var isExpanded by remember { mutableStateOf(false) }
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
