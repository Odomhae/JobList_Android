package com.odom.jobinfo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class ApiResult {

    @SerializedName("GetJobInfo")
    @Expose
    var getJobInfo: GetJobInfo? = null
}

class GetJobInfo {
    @SerializedName("list_total_count")
    @Expose
    var listTotalCount: Int = 0

    @SerializedName("RESULT")
    @Expose
    var result: Result? = null

    @SerializedName("row")
    @Expose
    var row: List<JobInfo>? = null
}

class Result {
    @SerializedName("CODE")
    @Expose
    var code: String? = null

    @SerializedName("MESSAGE")
    @Expose
    var message: String? = null
}

class JobInfo {
    @SerializedName("JO_REQST_NO")
    @Expose
    var joReqstNo: String? = null

    @SerializedName("JO_REGIST_NO")
    @Expose
    var joRegistNo: String? = null

    @SerializedName("CMPNY_NM")
    @Expose
    var cmpnyNm: String? = null

    @SerializedName("BSNS_SUMRY_CN")
    @Expose
    var bsnsSumryCn: String? = null

    @SerializedName("RCRIT_JSSFC_CMMN_CODE_SE")
    @Expose
    var rcritJssfcCmmnCodeSe: String? = null

    @SerializedName("JOBCODE_NM")
    @Expose
    var jobcodeNm: String? = null

    @SerializedName("RCRIT_NMPR_CO")
    @Expose
    var rcritNmprCo: Double? = null

    @SerializedName("ACDMCR_CMMN_CODE_SE")
    @Expose
    var acdmcrCmmnCodeSe: String? = null

    @SerializedName("ACDMCR_NM")
    @Expose
    var acdmcrNm: String? = null

    @SerializedName("EMPLYM_STLE_CMMN_CODE_SE")
    @Expose
    var emplymStleCmmnCodeSe: String? = null

    @SerializedName("EMPLYM_STLE_CMMN_MM")
    @Expose
    var emplymStleCmmnMm: String? = null

    @SerializedName("WORK_PARAR_BASS_ADRES_CN")
    @Expose
    var workPararBassAdresCn: String? = null

    @SerializedName("SUBWAY_NM")
    @Expose
    var subwayNm: String? = null

    @SerializedName("DTY_CN")
    @Expose
    var dtyCn: String? = null

    @SerializedName("CAREER_CND_CMMN_CODE_SE")
    @Expose
    var careerCndCmmnCodeSe: String? = null

    @SerializedName("CAREER_CND_NM")
    @Expose
    var careerCndNm: String? = null

    @SerializedName("HOPE_WAGE")
    @Expose
    var hopeWage: String? = null

    @SerializedName("RET_GRANTS_NM")
    @Expose
    var retGrantsNm: String? = null

    @SerializedName("WORK_TIME_NM")
    @Expose
    var workTimeNm: String? = null

    @SerializedName("WORK_TM_NM")
    @Expose
    var workTmNm: String? = null

    @SerializedName("HOLIDAY_NM")
    @Expose
    var holidayNm: String? = null

    @SerializedName("WEEK_WORK_HR")
    @Expose
    var weekWorkHr: String? = null

    @SerializedName("JO_FEINSR_SBSCRB_NM")
    @Expose
    var joFeinsrSbscrbNm: String? = null

    @SerializedName("RCEPT_CLOS_NM")
    @Expose
    var rceptClosNm: String? = null

    @SerializedName("RCEPT_MTH_IEM_NM")
    @Expose
    var rceptMthIemNm: String? = null

    @SerializedName("MODEL_MTH_NM")
    @Expose
    var modelMthNm: String? = null

    @SerializedName("RCEPT_MTH_NM")
    @Expose
    var rceptMthNm: String? = null

    @SerializedName("PRESENTN_PAPERS_NM")
    @Expose
    var presentnPapersNm: String? = null

    @SerializedName("MNGR_NM")
    @Expose
    var mngrNm: String? = null

    @SerializedName("MNGR_PHON_NO")
    @Expose
    var mngrPhonNo: String? = null

    @SerializedName("MNGR_INSTT_NM")
    @Expose
    var mngrInsttNm: String? = null

    @SerializedName("BASS_ADRES_CN")
    @Expose
    var bassAdresCn: String? = null

    @SerializedName("JO_SJ")
    @Expose
    var joSj: String? = null

    @SerializedName("JO_REG_DT")
    @Expose
    var joRegDt: String? = null

    @SerializedName("GUI_LN")
    @Expose
    var guiLn: String? = null
}