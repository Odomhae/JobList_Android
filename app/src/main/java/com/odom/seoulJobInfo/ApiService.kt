package com.odom.seoulJobInfo

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    // 전체 결과 (recMntList는 START_INDEX/END_INDEX 만 받으며, 서버측 필터 파라미터가 없다)
    @GET("{START_INDEX}/{END_INDEX}")
    suspend fun getResult(
        @Path("START_INDEX") START_INDEX: Int,
        @Path("END_INDEX") END_INDEX: Int
    ): ApiResult

}
