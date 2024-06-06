package com.odom.jobinfo

import SearchPref
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.odom.jobinfo.ui.theme.JobInfoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                setContent {
                    JobContent()
                }
            }
        }
    }

}


@Composable
fun JobContent() {
    val context = LocalContext.current
    var jobList by remember { mutableStateOf<List<JobInfo>>(emptyList()) }

    var backPressedTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime > backPressedTime + 2000) {
            backPressedTime = currentTime
            Toast.makeText(context, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
        } else if (currentTime <= backPressedTime + 2000) {
            (context as? ComponentActivity)?.finish()
        }
    }

    LaunchedEffect(Unit) {

        val pref = SearchPref(context)
        val searchedJobs = MainActivity.RetrofitClient.create().getCustomResult(pref.getEducation(), pref.getStyle(), pref.getLocation(),  pref.getCareer()).getJobInfo?.row!!

        jobList = searchedJobs
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Toolbar("서울시 정보만 있습니다")
            LocationButton()
            JobList(jobList)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(name: String) {

    TopAppBar(
        title = { Text(text = name) },
     //   contentColor = MaterialTheme.colorScheme.primary,
     //   backgroundColor = Color.Yellow,

        navigationIcon = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu"
                )
            }
        },
   //     elevation = 12.dp,
        actions = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "MoreVert",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    )
    
}

@Composable
fun LocationButton() {
    val context = LocalContext.current
    val searchValue =  remember { SearchPref(context) }
    var jobList by remember { mutableStateOf<List<JobInfo>>(emptyList()) }

    var isDropDownMenuExpanded by remember { mutableStateOf(false) }
    val items = listOf("강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구",
                        "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구",
                        "은평구", "종로구", "중구", "중랑구")

    val getLocation = searchValue.getLocation()

    var locationText by remember { mutableStateOf(getLocation) }
    var selectedLocation: String

    if (getLocation != "%20") {
        locationText = getLocation
        selectedLocation = getLocation
    } else {
        locationText = "근무지 선택"
    }

    Column {
        Button(
            onClick = { isDropDownMenuExpanded = true }
        ) {
            Text(text = locationText)
        }

        Spacer(modifier = Modifier.height(16.dp))

        JobList(jobList)
    }

    DropdownMenu(
        modifier = Modifier.wrapContentSize(),
        expanded = isDropDownMenuExpanded,
        onDismissRequest = { isDropDownMenuExpanded = false }
    ) {
        items.forEachIndexed { index, _ ->
            DropdownMenuItem(
                text = { Text(items[index]) },
                onClick = {
                    locationText = items[index]
                    isDropDownMenuExpanded = false
                    selectedLocation = items[index]

                    searchValue.saveLocation(selectedLocation)

                    CoroutineScope(Dispatchers.Default).launch {
                        jobList = customSearch(context)
                    }

                })
        }

    }

}

suspend fun customSearch(context : Context) :  List<JobInfo> {

    val pref = SearchPref(context)
    val searchedJobs = MainActivity.RetrofitClient.create().getCustomResult(pref.getEducation(), pref.getStyle(), pref.getLocation(), pref.getCareer()).getJobInfo?.row!!

    return searchedJobs
}


@Composable
fun JobList(infos : List<JobInfo>) {
    if (infos.isNotEmpty()) {
        Text(text = "검색결과 : " + infos.size + "개")
    }

    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn {
        items(infos) { info ->
            ExpandableCardView(info)
            Divider(color = Color.DarkGray, thickness = Dp(10F))
        }
    }
}

fun shortItem(job: JobInfo) : String {
    val sb = StringBuilder()

    sb.append("기업명칭: ${job.cmpnyNm}\n")
    sb.append("사업요약내용: ${job.bsnsSumryCn}\n")
    sb.append("모집요강: ${job.guiLn}\n\n")

    sb.append("근무시간: ${job.workTimeNm}\n")
    sb.append("공휴일: ${job.holidayNm}\n\n")
    sb.append("마감일: ${job.rceptClosNm}\n") // fontSize = 15.sp,  fontStyle = FontStyle.Italic)

    return sb.toString()
}

fun longItem(job: JobInfo) : String {

    val sb = StringBuilder()

    sb.append("기업명칭: ${job.cmpnyNm}\n")
    sb.append("사업요약내용: ${job.bsnsSumryCn}\n")
    sb.append("모집요강: ${job.guiLn}\n\n")

    sb.append("근무시간: ${job.workTimeNm}\n")
    sb.append("공휴일: ${job.holidayNm}\n\n")
    sb.append("마감일: ${job.rceptClosNm}\n") // fontSize = 15.sp,  fontStyle = FontStyle.Italic)

    sb.append("구인제목: ${job.joSj}\n")
    sb.append("근무예정지: ${job.workPararBassAdresCn}\n")
    sb.append("직무내용: ${job.dtyCn}\n\n") //  fontSize = 15.sp, fontWeight = FontWeight.Bold)

    sb.append("급여조건: ${job.hopeWage}\n")

    sb.append("담당 상담사명: ${job.mngrNm}\n")
    sb.append("담당 상담사 전화번호: ${job.mngrPhonNo}\n")
    sb.append("담당 상담사 소속기관명: ${job.mngrInsttNm}\n\n")

    sb.append("기업 주소: ${job.bassAdresCn}\n")

    sb.append("구인신청번호: ${job.joReqstNo}\n")
    sb.append("구인등록번호: ${job.joRegistNo}\n")

    return  sb.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableCardView(job: JobInfo) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
       // elevation = CardElevation
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clickable { isExpanded = !isExpanded }
                .combinedClickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = rememberRipple(bounded = true),
                    onClick = { isExpanded = !isExpanded },
                    onLongClick = {
                        copyToClipboard(context, isExpanded, job)
                    }
                )
        ) {
           // Text("Title", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (isExpanded) {
                Text(text = longItem(job))
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Collapse"
                    )
                }
            } else {
                Text(text = shortItem(job))
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand"
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, isExpaned : Boolean, job: JobInfo) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copyText = if (isExpaned) longItem(job) else shortItem(job)
    val clip = ClipData.newPlainText("Copied Text", copyText)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(context, "글자가 복사되었습니다", Toast.LENGTH_SHORT).show()
}

