package com.odom.seoulJobInfo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class ApiResult {

    @SerializedName("recMntList")
    @Expose
    var recMntList: RecMntList? = null
}

class RecMntList {
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
    @SerializedName("COMPANY")
    @Expose
    var company: String? = null

    @SerializedName("TITLE")
    @Expose
    var title: String? = null

    @SerializedName("CAREER")
    @Expose
    var career: String? = null

    @SerializedName("REG_DT")
    @Expose
    var regDt: String? = null

    @SerializedName("CLOSE_DT")
    @Expose
    var closeDt: String? = null

    @SerializedName("REGION")
    @Expose
    var region: String? = null

    @SerializedName("STRTNM_CD")
    @Expose
    var strtnmCd: String? = null

    @SerializedName("MIN_EDUBG")
    @Expose
    var minEdubg: String? = null

    @SerializedName("MAX_EDUBG")
    @Expose
    var maxEdubg: String? = null

    @SerializedName("IND_TP_CD_NM")
    @Expose
    var indTpCdNm: String? = null

    @SerializedName("CORP_ADDR")
    @Expose
    var corpAddr: String? = null

    @SerializedName("JOBS_NM")
    @Expose
    var jobsNm: String? = null

    @SerializedName("JOB_CONT")
    @Expose
    var jobCont: String? = null

    @SerializedName("EMP_TP_NM")
    @Expose
    var empTpNm: String? = null

    @SerializedName("COLLECT_PSNCNT")
    @Expose
    var collectPsncnt: String? = null

    @SerializedName("SAL_TP_NM")
    @Expose
    var salTpNm: String? = null

    @SerializedName("MAJOR")
    @Expose
    var major: String? = null

    @SerializedName("CERTIFICATE")
    @Expose
    var certificate: String? = null

    @SerializedName("MLTSVC_EXC_HOPE")
    @Expose
    var mltsvcExcHope: String? = null

    @SerializedName("COMP_ABL")
    @Expose
    var compAbl: String? = null

    @SerializedName("PF_COND")
    @Expose
    var pfCond: String? = null

    @SerializedName("SEL_MTHD")
    @Expose
    var selMthd: String? = null

    @SerializedName("RCPT_MTHD")
    @Expose
    var rcptMthd: String? = null

    @SerializedName("SUBMIT_DOC")
    @Expose
    var submitDoc: String? = null

    @SerializedName("WORK_REGION")
    @Expose
    var workRegion: String? = null

    @SerializedName("WORKDAY_WORKHR_CONT")
    @Expose
    var workdayWorkhrCont: String? = null

    @SerializedName("FOUR_INS")
    @Expose
    var fourIns: String? = null

    @SerializedName("RETIREPAY")
    @Expose
    var retirepay: String? = null

    @SerializedName("ETC_WELFARE")
    @Expose
    var etcWelfare: String? = null

    @SerializedName("JOBS_CD")
    @Expose
    var jobsCd: String? = null

    @SerializedName("MIN_EDUBG_ICD")
    @Expose
    var minEdubgIcd: String? = null

    @SerializedName("MAX_EDUBG_ICD")
    @Expose
    var maxEdubgIcd: String? = null

    @SerializedName("REGION_CD")
    @Expose
    var regionCd: String? = null

    @SerializedName("EMP_TP_CD")
    @Expose
    var empTpCd: String? = null

    @SerializedName("ENTER_TP_CD")
    @Expose
    var enterTpCd: String? = null

    @SerializedName("SAL_TP_CD")
    @Expose
    var salTpCd: String? = null

    @SerializedName("EMP_CHARGER_DPT")
    @Expose
    var empChargerDpt: String? = null

    @SerializedName("CONTACT_TELNO")
    @Expose
    var contactTelno: String? = null

    /**
     * The API no longer returns a unique posting ID (the old JO_REQST_NO is gone),
     * so favorites are identified by a composite of the posting's stable fields.
     */
    val favoriteKey: String
        get() = "${company.orEmpty()}|${title.orEmpty()}|${regDt.orEmpty()}|${closeDt.orEmpty()}"
}
