package wonyong.by.zolzolzol

data class ToiletData(var PUBLFACLT_DIV_NM : String?="",
                      var PBCTLT_PLC_NM: String?="",
                      var REFINE_ROADNM_ADDR: String?="",
                      var REFINE_LOTNO_ADDR: String?="",
                      var MALE_FEMALE_TOILET_YN: String?="",
                      var MALE_WTRCLS_CNT: Int?=0,
                      var MALE_UIL_CNT: Int?=0,
                      var MALE_DSPSN_WTRCLS_CNT: Int?=0,
                      var MALE_DSPSN_UIL_CNT: Int?=0,
                      var MALE_CHILDUSE_WTRCLS_CNT: Int?=0,
                      var MALE_CHILDUSE_UIL_CNT: Int?=0,
                      var FEMALE_WTRCLS_CNT: Int?=0,
                      var FEMALE_DSPSN_WTRCLS_CNT: Int?=0,
                      var FEMALE_CHILDUSE_WTRCLS_CNT: Int?=0,
                      var MANAGE_INST_NM: String?="",
                      var MANAGE_INST_TELNO: String?="",
                      var OPEN_TM_INFO: String?="",
                      var INSTL_YY: String?="",
                      var REFINE_WGS84_LAT: Double?=0.0,
                      var REFINE_WGS84_LOGT: Double?=0.0,
                      var DATA_STD_DE: String?="",
                      var SIGUN_NM: String?="",
                      var SIGUN_CD: String?="",
                      var REFINE_ZIP_CD: String?="",
                      var COMMENT: String=""

    ) {

}