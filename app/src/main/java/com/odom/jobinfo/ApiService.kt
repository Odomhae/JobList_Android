package com.odom.jobinfo

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {


    // 전체 결과
    @GET("{START_INDEX}/{END_INDEX}")
   suspend fun getResult (
        @Path("START_INDEX") START_INDEX: Int?,
        @Path("END_INDEX") END_INDEX: Int?
    ) : ApiResult


   // 선택조건으로 검색
   @GET("1/900/{EDUCATION}/{STYLE}/{LOCATION}/{CAREER}")
   suspend fun getCustomResult (
       @Path("EDUCATION") EDUCATION : String?= "%20", // 학력코드
       @Path("STYLE") STYLE : String? = "%20", // 고용형태
       @Path("LOCATION") LOCATION : String?, // 근무지 주소
       @Path("CAREER") CAREER : String? = "%20" // 경력조건
   ) : ApiResult

}