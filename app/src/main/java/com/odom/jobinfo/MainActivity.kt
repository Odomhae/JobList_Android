package com.odom.jobinfo

import SearchValue
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.jobinfo.ui.theme.JobInfoTheme
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
                    JobList()
                }
            }
        }
    }

}


@Composable
fun JobList() {
    var jobList by remember { mutableStateOf<List<JobInfo>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {

        val fetchedLists = MainActivity.RetrofitClient.create().getResult(1,100).getJobInfo?.row
        jobList = fetchedLists!!
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            ToolbarSample("서울시 정보만 있습니다")

            Row {
                ButtonGrid()
                Button(modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = {
                        coroutineScope.launch {
                            jobList = customSearch()
                        }

                    } ) {
                    Text(text = "재검색")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            JobList(jobList)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSample(name: String) {

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


suspend fun customSearch() :  List<JobInfo> {

   //val location = SearchValue.getLocation(LocalContext.current)

    val searchedJobs = MainActivity.RetrofitClient.create().getCustomResult(SearchValue.education,SearchValue.style, SearchValue.location, SearchValue.career).getJobInfo?.row!!

    return searchedJobs
}

@Composable
fun ButtonGrid() {
    Column(modifier = Modifier.wrapContentSize(),
    ){
        Row {
            // 근무지 선택
            LocationButton()

            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { /* 버튼 2 클릭 처리 */ }) {
                Text("고용형태")
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // 간격 조절

        Row {
            Button(onClick = { /* 버튼 3 클릭 처리 */ }) {
                Text("경력조건")
            }
            Spacer(modifier = Modifier.width(16.dp)) // 간격 조절
            Button(onClick = { /* 버튼 4 클릭 처리 */ }) {
                Text("학력조건")
            }
        }
    }
}

@Composable
fun LocationButton() {
    var isDropDownMenuExpanded by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf("근무지 선택") }
    var selectedLocation = "%20"

    val getLocation = SearchValue.getLocation(LocalContext.current)
    Log.d("===ttTag" , getLocation)
    if (getLocation != "%20") {
        locationText = getLocation
        selectedLocation = getLocation
    }


    Button(
        onClick = { isDropDownMenuExpanded = true }
    ) {
        Text(text = locationText)
    }

    DropdownMenu(
        modifier = Modifier.wrapContentSize(),
        expanded = isDropDownMenuExpanded,
        onDismissRequest = { isDropDownMenuExpanded = false }
    ) {
        DropdownMenuItem(text = { Text("근무지 선택", fontWeight = FontWeight.Medium) }, onClick = {null})
        DropdownMenuItem(
            text = { Text("강남구") },
            onClick = {locationText = "강남구"
                      isDropDownMenuExpanded = false
                selectedLocation = "강남구"
                SearchValue.location = "강남구"})
        DropdownMenuItem(
            text = { Text("강동구") },
            onClick = {locationText = "강동구"
                isDropDownMenuExpanded = false
                selectedLocation = "강동구"
                SearchValue.location = "강동구"})
        DropdownMenuItem(
            text = { Text("강북구") },
            onClick = {locationText = "강북구"
                isDropDownMenuExpanded = false
                selectedLocation = "강북구"
                SearchValue.location = "강북구"})
        DropdownMenuItem(
            text = { Text("강서구") },
            onClick = {locationText = "강서구"
                isDropDownMenuExpanded = false
                selectedLocation = "강서구"
                SearchValue.location = "강서구"})
        DropdownMenuItem(
            text = { Text("관악구") },
            onClick = {locationText = "관악구"
                isDropDownMenuExpanded = false
                selectedLocation = "관악구"
                SearchValue.location = "관악구"})

    }

    Log.d("===ttTag2" , selectedLocation)
    Log.d("===ttTag3" , SearchValue.location)

    SearchValue.saveLocation(LocalContext.current, SearchValue.location)
}


@Composable
fun JobList(infos : List<JobInfo>) {
    LazyColumn {
        items(infos) { info ->
            ExpandableCardView(info)
            Divider(color = Color.DarkGray, thickness = Dp(10F))
        }
    }
}

@Composable
fun ShortJobItem(job: JobInfo) {
    Text(text = "기업명칭: ${job.cmpnyNm}")
    Text(text = "사업요약내용: ${job.bsnsSumryCn}")
    Text(text = "모집요강: ${job.guiLn}")

    Text(text = "")
    Text(text = "근무시간: ${job.workTimeNm}")
    Text(text = "공휴일: ${job.holidayNm}")

    Text(text = "")
    Text(text = "마감일: ${job.rceptClosNm}", fontSize = 15.sp,  fontStyle = FontStyle.Italic)
}

@Composable
fun FullJobItem(job: JobInfo) {
    Text(text = "기업명칭: ${job.cmpnyNm}")
    Text(text = "사업요약내용: ${job.bsnsSumryCn}")
    Text(text = "모집인원수: ${job.rcritNmprCo}")
    Text(text = "학력코드명: ${job.acdmcrNm}")
    Text(text = "고용형태코드명: ${job.emplymStleCmmnMm}")
    Text(text = "근무예정지 주소: ${job.workPararBassAdresCn}")
    Text(text = "직무내용: ${job.dtyCn}",  fontSize = 15.sp, fontWeight = FontWeight.Bold)

    Text(text = "")
    Text(text = "경력조건코드명: ${job.careerCndNm}")
    Text(text = "급여조건: ${job.hopeWage}")
    //Text(text = "퇴직금구분: ${job.retGrantsNm}")
    Text(text = "근무시간: ${job.workTimeNm}")
    Text(text = "근무형태: ${job.workTmNm}")
    Text(text = "공휴일: ${job.holidayNm}")
    Text(text = "주당근무시간: ${job.weekWorkHr}")
    Text(text = "4대보험: ${job.joFeinsrSbscrbNm}")

    Text(text = "")
    Text(text = "마감일: ${job.rceptClosNm}", fontSize = 15.sp,  fontStyle = FontStyle.Italic)
    Text(text = "제출서류: ${job.presentnPapersNm}")

    Text(text = "")
    Text(text = "담당 상담사명: ${job.mngrNm}")
    Text(text = "담당 상담사 전화번호: ${job.mngrPhonNo}")
    Text(text = "담당 상담사 소속기관명: ${job.mngrInsttNm}")

    Text(text = "")
    Text(text = "기업 주소: ${job.bassAdresCn}")
    Text(text = "구인제목: ${job.joSj}")
    Text(text = "등록일: ${job.joRegDt}")
    Text(text = "모집요강: ${job.guiLn}")

    Text(text = "구인신청번호: ${job.joReqstNo}")
    Text(text = "구인등록번호: ${job.joRegistNo}")

}

@Composable
fun ExpandableCardView(job: JobInfo) {
    var isExpanded by remember { mutableStateOf(false) }

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
        ) {
           // Text("Title", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (isExpanded) {
                FullJobItem(job)
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
                ShortJobItem(job = job)
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

