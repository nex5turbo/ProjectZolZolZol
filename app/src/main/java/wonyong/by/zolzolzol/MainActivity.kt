package wonyong.by.zolzolzol

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.ArrayMap
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.util.*


class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener{


    lateinit var googleMap : GoogleMap
    var fusedLocationClient : FusedLocationProviderClient?=null
    var locationCallback : LocationCallback?=null
    var locationRequeset : LocationRequest?=null
    var loc = LatLng(37.715133, 126.734086)
    var data = ""
    var dataList: ArrayList<ToiletData> = ArrayList<ToiletData>()
    var dataMap: ArrayMap<String, ToiletData> = ArrayMap<String, ToiletData>()
    var markerList: ArrayList<Marker> = ArrayList<Marker>()
    var listFinish = false
    var backPressedTime = 0L
    var selectedSpinner = 1
    lateinit var clickMarker : Marker
    lateinit var nowMarker : Marker

    val DEF = 1
    val IN1KM = 2
    val IN500M = 3
    val IN100M = 4
    val SHORTEST = 5
    val IN5MIN = 6


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        initLocation()
        init()
    }

    private fun initLocation() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        ==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
       this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getUserLocation()
            startLocationUpdates()
            initMap()
        }else{
            ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment


        mapFragment.getMapAsync{
            googleMap = it
            googleMap.setOnMarkerClickListener(this)
            googleMap.setOnInfoWindowClickListener {
                if(it.title.equals("현재 위치")||it.title.equals("지정위치")){
                    return@setOnInfoWindowClickListener
                }
                val i = Intent(this, InformationActivity::class.java)
                val name = it.title
                val key = dataMap.get(name)
                i.putExtra("toiletName", name)
                i.putExtra("lat", loc.latitude)
                i.putExtra("logt", loc.longitude)
                i.putExtra("openTime", key?.OPEN_TM_INFO)
                i.putExtra("phoneNumber", key?.MANAGE_INST_TELNO)
                i.putExtra("both", key?.MALE_FEMALE_TOILET_YN)
                i.putExtra("toiletLatitude", key?.REFINE_WGS84_LAT)
                i.putExtra("toiletLongtitude", key?.REFINE_WGS84_LOGT)
                startActivity(i)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            }

            var dokdo = LatLng(37.2444436, 131.8786475)

            var options = MarkerOptions()
            options.position(dokdo).title("독도").snippet("다케시마 아니다")
            var dokdoMarker = googleMap.addMarker(options)
            clickMarker = googleMap.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)))
            clickMarker.remove()

        }
    }

    override fun onMarkerClick(marker : Marker): Boolean {
        if(marker.title.equals("현재 위치") || marker.title.equals("지정위치")){
            return false
        }
        var mkLat: Double = dataMap.get(marker.title)!!.REFINE_WGS84_LAT!!
        var mkLogt: Double = dataMap.get(marker.title)!!.REFINE_WGS84_LOGT!!
        var distance = CalculateDistance().distance(mkLat, mkLogt, loc.latitude, loc.longitude).toInt()
        var time = CalculateDistance().timeTake(distance)

        when(distance){
            in 0..250->{
                Toast.makeText(this, "굉장히 가까워요! ${time}분 정도 걸릴 것 같아요. 빨리빨리!", Toast.LENGTH_SHORT).show()
            }
            in 250..1000->{
                Toast.makeText(this, "조금 멀어요... ${time}분 정도 걸릴 것 같아요. 서둘러요!", Toast.LENGTH_SHORT).show()
            }
            else->{
                Toast.makeText(this, "${time}분 정도 걸릴 것 같은데...집이 더 가까울듯 ", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    private fun init() {
        spinner.setOnItemSelectedListener(object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                var er = parent?.getItemAtPosition(position)
                when(er){
                    "시 전체"->{
                        inofAllCity()
                        selectedSpinner = DEF
                        clickMarker.remove()
                        googleMap.setOnMapLongClickListener {}
                    }
                    "1km 반경"->{
                        rangeof(1000)
                        selectedSpinner = IN1KM
                        clickMarker.remove()
                        googleMap.setOnMapLongClickListener {}
                    }
                    "500m 반경"->{
                        rangeof(500)
                        selectedSpinner = IN500M
                        clickMarker.remove()
                        googleMap.setOnMapLongClickListener {}
                    }
                    "100m 반경"->{
                        rangeof(100)
                        selectedSpinner = IN100M
                        clickMarker.remove()
                        googleMap.setOnMapLongClickListener {}
                    }
                    "가장 가까운 화장실"->{
                        shortest()
                        selectedSpinner = SHORTEST
                        clickMarker.remove()
                        googleMap.setOnMapLongClickListener {}
                    }
                    "5분 이내의 거리"->{
                        var dialog = AlertDialog.Builder(this@MainActivity)
                        dialog.setTitle("주의사항").setMessage("해당 시간은 화장실과의 직선거리만을 계산한 시간입니다. 경로에 따라 시간이 더 걸릴 수 있습니다.")
                        dialog.setPositiveButton("Ok~", object : DialogInterface.OnClickListener{
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                in5minute()
                                selectedSpinner = IN5MIN
                                clickMarker.remove()
                                googleMap.setOnMapLongClickListener {}
                            }
                        })
                        dialog.create().show()
                    }
                    "위치 지정하기"->{
                        var dialog = AlertDialog.Builder(this@MainActivity)
                        dialog.setTitle("사용법").setMessage("원하는 지점을 1초 이상 눌러주세요.\n(현재 위치한 시군 화장실만 표시합니다.)")
                        dialog.setPositiveButton("Ok~", object : DialogInterface.OnClickListener{
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                selectedSpinner = DEF
                                googleMap.setOnMapLongClickListener {
                                    mapClick(it)
                                }
                            }
                        })
                        dialog.create().show()

                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        })
        locationButton.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f))
        }
    }

    @SuppressLint("MissingPermission")
    fun getUserLocation(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient?.lastLocation?.addOnSuccessListener {
            loc = LatLng(it.latitude, it.longitude)
            var add = geoCoder(loc)
            initAPI(add)
            var options = MarkerOptions()
            options.position(loc).title("현재 위치").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            nowMarker = googleMap.addMarker(options)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED&&
                  grantResults[1] == PackageManager.PERMISSION_GRANTED){
                getUserLocation()
                startLocationUpdates()
                initMap()
            }else{
                Toast.makeText(this, "위치정보 권한이 없으면 사용이 불가합니다.", Toast.LENGTH_SHORT).show()
                initMap()
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(){
        locationRequeset = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                nowMarker.remove()
                loc = LatLng(locationResult.locations[0].latitude, locationResult.locations[0].longitude)
                var options = MarkerOptions()
                options.position(loc).title("현재 위치").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                nowMarker = googleMap.addMarker(options)
                when(selectedSpinner){
                    DEF->{
                    }
                    IN1KM->{
                        rangeof(1000)
                    }
                    IN500M->{
                        rangeof(500)
                    }
                    IN100M->{
                        rangeof(100)
                    }
                    SHORTEST->{
                        shortest()
                    }
                    IN5MIN->{
                        in5minute()
                    }
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(
            locationRequeset,
            locationCallback,
            Looper.getMainLooper())
    }


    override fun onBackPressed() {

        var nowPressedTime = System.currentTimeMillis()
        if(backPressedTime == 0L){
            backPressedTime = nowPressedTime
            Toast.makeText(this, "종료하시려면 한번 더 눌러주세요", Toast.LENGTH_SHORT).show()
        }else if(nowPressedTime - backPressedTime > 3000){
            Toast.makeText(this, "종료하시려면 한번 더 눌러주세요", Toast.LENGTH_SHORT).show()
            backPressedTime = nowPressedTime
        }else{
            moveTaskToBack(true)
            finishAndRemoveTask()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun initAPI(address : String) {
            var thread = Thread(object : Runnable{
                override fun run(){
                    data = getXmlData(address)
                    runOnUiThread(object : Runnable{
                        override fun run(){
                            for(order : ToiletData in dataList) {

                                var markerOption = MarkerOptions()
                                var latlng =
                                    LatLng(order.REFINE_WGS84_LAT!!, order.REFINE_WGS84_LOGT!!)
                                markerOption.position(latlng)
                                order.PBCTLT_PLC_NM = order.PBCTLT_PLC_NM!!.replace(".", "")
                                markerOption.title(order.PBCTLT_PLC_NM)
                                markerOption.snippet("세부정보 확인")
                                var marker = googleMap.addMarker(markerOption)
                                markerList.add(marker)
                                dataMap.put(order.PBCTLT_PLC_NM, order)
                            }

                        }
                    })
                }
            }).start()
    }

    fun getXmlData(address: String):String{
        var location = URLEncoder.encode(address)

        var queryURL = "https://openapi.gg.go.kr/Publtolt?" +
                "&SIGUN_NM=$location&pIndex=1&pSize=1000&KEY=6d3cf9829c984d59a11126c66c81f992"

        var url = URL(queryURL)
        var inputStream = url.openStream()
        var tData = ToiletData()
        var factory = XmlPullParserFactory.newInstance()
        var xpp = factory.newPullParser()
        xpp.setInput(object : InputStreamReader(inputStream, "UTF-8"){})
        var strTag = ""
        xpp.next()
        var eventType = xpp.eventType
        while(eventType != XmlPullParser.END_DOCUMENT){
            when(eventType){
                XmlPullParser.START_DOCUMENT->{
//                    buffer.append("파싱 시작\n\n")
                }
                XmlPullParser.START_TAG->{
                    strTag = xpp.name
                    if(strTag.equals("row")){}
                    else if(strTag.equals("PUBLFACLT_DIV_NM")){
                        xpp.next()
                        if(xpp.text == null){
                            tData.PUBLFACLT_DIV_NM = ""
                            xpp.next()
                        }else {
                            tData.PUBLFACLT_DIV_NM = xpp.text
                        }
                    }else if(strTag.equals("PBCTLT_PLC_NM")){
//                        buffer.append("화장실이름 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.PBCTLT_PLC_NM =""
                            xpp.next()
                        }else {
                            tData.PBCTLT_PLC_NM = xpp.text
                        }
                    }else if(strTag.equals("REFINE_ROADNM_ADDR")){
//                        buffer.append("소재지도로명주소 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.REFINE_ROADNM_ADDR =""
                            xpp.next()
                        }else {
                            tData.REFINE_ROADNM_ADDR = xpp.text
                        }
                    }else if(strTag.equals("REFINE_LOTNO_ADDR")){
//                        buffer.append("소재지지번주소 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.REFINE_LOTNO_ADDR =""
                            xpp.next()
                        }else {
                            tData.REFINE_LOTNO_ADDR = xpp.text
                        }
                    }else if(strTag.equals("MALE_FEMALE_TOILET_YN")){
//                        buffer.append("남녀공용화장실여부 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_FEMALE_TOILET_YN =""
                            xpp.next()
                        }else {
                            tData.MALE_FEMALE_TOILET_YN = xpp.text
                        }
                    }else if(strTag.equals("MALE_WTRCLS_CNT")){
//                        buffer.append("남성용 대변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_WTRCLS_CNT =0
                            xpp.next()
                        }else {
                            tData.MALE_WTRCLS_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("MALE_UIL_CNT")){
//                        buffer.append("남성용 소변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_UIL_CNT =0
                            xpp.next()
                        }else {
                            tData.MALE_UIL_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("MALE_DSPSN_WTRCLS_CNT")){
//                        buffer.append("남성용 장애인용 대변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_DSPSN_WTRCLS_CNT =0
                            xpp.next()
                        }else {
                            tData.MALE_DSPSN_WTRCLS_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("MALE_DSPSN_UIL_CNT")){
//                        buffer.append("남성용 장애인용 소변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_DSPSN_UIL_CNT =0
                            xpp.next()
                        }else {
                            tData.MALE_DSPSN_UIL_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("MALE_CHILDUSE_WTRCLS_CNT")){
//                        buffer.append("남성용 어린이용 대변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_CHILDUSE_WTRCLS_CNT =0
                            xpp.next()
                        }else {
                            tData.MALE_CHILDUSE_WTRCLS_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("MALE_CHILDUSE_UIL_CNT")){
//                        buffer.append("남성용 어린이용 소변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MALE_CHILDUSE_UIL_CNT =0
                            xpp.next()
                        }else {
                            tData.MALE_CHILDUSE_UIL_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("FEMALE_WTRCLS_CNT")){
//                        buffer.append("여성용 대변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.FEMALE_WTRCLS_CNT =0
                            xpp.next()
                        }else {
                            tData.FEMALE_WTRCLS_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("FEMALE_DSPSN_WTRCLS_CNT")){
//                        buffer.append("여성용 장애인용 대변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.FEMALE_DSPSN_WTRCLS_CNT =0
                            xpp.next()
                        }else {
                            tData.FEMALE_DSPSN_WTRCLS_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("FEMALE_CHILDUSE_WTRCLS_CNT")){
//                        buffer.append("여성용 어린이용 대변기수 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.FEMALE_CHILDUSE_WTRCLS_CNT =0
                            xpp.next()
                        }else {
                            tData.FEMALE_CHILDUSE_WTRCLS_CNT = xpp.text.toInt()
                        }
                    }else if(strTag.equals("MANAGE_INST_NM")){
//                        buffer.append("관리기관명 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MANAGE_INST_NM =""
                            xpp.next()
                        }else {
                            tData.MANAGE_INST_NM = xpp.text
                        }
                    }else if(strTag.equals("MANAGE_INST_TELNO")){
//                        buffer.append("전화번호 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.MANAGE_INST_TELNO =""
                            xpp.next()
                        }else {
                            tData.MANAGE_INST_TELNO = xpp.text
                        }
                    }else if(strTag.equals("OPEN_TM_INFO")){
//                        buffer.append("개방시간 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.OPEN_TM_INFO =""
                            xpp.next()
                        }else {
                            tData.OPEN_TM_INFO = xpp.text
                        }
                    }else if(strTag.equals("INSTL_YY")){
//                        buffer.append("설치년도 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.INSTL_YY =""
                            xpp.next()
                        }else {
                            tData.INSTL_YY = xpp.text
                        }
                    }else if(strTag.equals("REFINE_WGS84_LAT")){
//                        buffer.append("위도 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.REFINE_WGS84_LAT =0.0
                            xpp.next()
                        }else {
                            tData.REFINE_WGS84_LAT = xpp.text.toDouble()
                        }
                    }else if(strTag.equals("REFINE_WGS84_LOGT")){
//                        buffer.append("경도 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.REFINE_WGS84_LOGT =0.0
                            xpp.next()
                        }else {
                            tData.REFINE_WGS84_LOGT = xpp.text.toDouble()
                        }
                    }else if(strTag.equals("DATA_STD_DE")){
//                        buffer.append("데이터기준일자 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.DATA_STD_DE =""
                            xpp.next()
                        }else {
                            tData.DATA_STD_DE = xpp.text
                        }
                    }else if(strTag.equals("SIGUN_NM")){
//                        buffer.append("시군명 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.SIGUN_NM =""
                            xpp.next()
                        }else {
                            tData.SIGUN_NM = xpp.text
                        }
                    }else if(strTag.equals("SIGUN_CD")){
//                        buffer.append("시군코드 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.SIGUN_CD =""
                            xpp.next()
                        }else {
                            tData.SIGUN_CD = xpp.text
                        }
                    }else if(strTag.equals("REFINE_ZIP_CD")){
//                        buffer.append("소재지우편번호 : ")
                        xpp.next()
                        if(xpp.text == null){
                            tData.REFINE_ZIP_CD =""
                            xpp.next()
                        }else {
                            tData.REFINE_ZIP_CD = xpp.text
                        }
                    }
                }
                XmlPullParser.TEXT->{

                }
                XmlPullParser.END_TAG->{
                    strTag = xpp.name

                    if(strTag.equals("row")){
                        Log.d("###", tData.PBCTLT_PLC_NM)
                        tData.PBCTLT_PLC_NM = tData.PBCTLT_PLC_NM!!.replace(".", "")
                        if(!tData.PBCTLT_PLC_NM!!.contains("학교")) {
                            dataList.add(
                                ToiletData(
                                    tData.PUBLFACLT_DIV_NM,
                                    tData.PBCTLT_PLC_NM,
                                    tData.REFINE_ROADNM_ADDR,
                                    tData.REFINE_LOTNO_ADDR,
                                    tData.MALE_FEMALE_TOILET_YN,
                                    tData.MALE_WTRCLS_CNT,
                                    tData.MALE_UIL_CNT,
                                    tData.MALE_DSPSN_WTRCLS_CNT,
                                    tData.MALE_DSPSN_UIL_CNT,
                                    tData.MALE_CHILDUSE_WTRCLS_CNT,
                                    tData.MALE_CHILDUSE_UIL_CNT,
                                    tData.FEMALE_WTRCLS_CNT,
                                    tData.FEMALE_DSPSN_WTRCLS_CNT,
                                    tData.FEMALE_CHILDUSE_WTRCLS_CNT,
                                    tData.MANAGE_INST_NM,
                                    tData.MANAGE_INST_TELNO,
                                    tData.OPEN_TM_INFO,
                                    tData.INSTL_YY,
                                    tData.REFINE_WGS84_LAT,
                                    tData.REFINE_WGS84_LOGT,
                                    tData.DATA_STD_DE,
                                    tData.SIGUN_NM,
                                    tData.SIGUN_CD,
                                    tData.REFINE_ZIP_CD,
                                    tData.COMMENT
                                )
                            )
                        }
                    }
                }
            }
            eventType = xpp.next()
        }
        listFinish = true
        return ""
    }

    fun rangeof(range: Int){
        var nowLatitude = loc.latitude
        var nowLongtitude = loc.longitude

        for(mk: Marker in markerList){
            mk.remove()
        }
        markerList.clear()
        for(data: ToiletData in dataList){
            var distance =
                CalculateDistance().distance(nowLatitude,
                    nowLongtitude,
                    data.REFINE_WGS84_LAT!!,
                    data.REFINE_WGS84_LOGT!!).toInt()
            if(distance <= range){
                var latLng = LatLng(data.REFINE_WGS84_LAT!!, data.REFINE_WGS84_LOGT!!)
                var mo = MarkerOptions()
                mo.title(data.PBCTLT_PLC_NM).snippet("세부정보 확인").position(latLng)
                var marker = googleMap.addMarker(mo)
                markerList.add(marker)
            }
        }
    }

    fun inofAllCity(){
        for(mk: Marker in markerList){
            mk.remove()
        }
        markerList.clear()
        for(data: ToiletData in dataList){
            var latLng = LatLng(data.REFINE_WGS84_LAT!!, data.REFINE_WGS84_LOGT!!)
            var mo = MarkerOptions()
            mo.title(data.PBCTLT_PLC_NM).snippet("세부정보 확인").position(latLng)
            var marker = googleMap.addMarker(mo)
            markerList.add(marker)
        }
    }

    fun shortest(){
        var nowLatitude = loc.latitude
        var nowLongtitude = loc.longitude
        var minDistanceData = ToiletData()
        var minDistance = 50000
        for(mk: Marker in markerList){
            mk.remove()
        }
        markerList.clear()
        for(data: ToiletData in dataList){
            var distance =
                CalculateDistance().distance(nowLatitude,
                    nowLongtitude,
                    data.REFINE_WGS84_LAT!!,
                    data.REFINE_WGS84_LOGT!!).toInt()
            if(distance <= minDistance){
                minDistanceData = data
                minDistance = distance
            }
        }
        var latLng = LatLng(minDistanceData.REFINE_WGS84_LAT!!, minDistanceData.REFINE_WGS84_LOGT!!)
        var mo = MarkerOptions()
        mo.title(minDistanceData.PBCTLT_PLC_NM).snippet("세부정보 확인").position(latLng)
        var marker = googleMap.addMarker(mo)
        markerList.add(marker)
    }

    fun in5minute(){
        var nowLatitude = loc.latitude
        var nowLongtitude = loc.longitude

        for(mk: Marker in markerList){
            mk.remove()
        }
        markerList.clear()
        for(data: ToiletData in dataList){
            var distance =
                CalculateDistance().distance(nowLatitude,
                    nowLongtitude,
                    data.REFINE_WGS84_LAT!!,
                    data.REFINE_WGS84_LOGT!!).toInt()
            var minute = CalculateDistance().timeTake(distance)
            if(minute <= 5){
                var latLng = LatLng(data.REFINE_WGS84_LAT!!, data.REFINE_WGS84_LOGT!!)
                var mo = MarkerOptions()
                mo.title(data.PBCTLT_PLC_NM).snippet("세부정보 확인").position(latLng)
                var marker = googleMap.addMarker(mo)
                markerList.add(marker)
            }
        }
    }

    fun stopLocationUpdates(){
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    fun geoCoder(latLng: LatLng) : String{
        var geo  = Geocoder(this, Locale.getDefault())
        lateinit var addresses : List<Address>
        addresses = geo.getFromLocation(
            latLng.latitude,
            latLng.longitude,
            1)
        var address = addresses.get(0)
        var tokenizer = StringTokenizer(address.getAddressLine(0).toString(), " ")
        var country = tokenizer.nextToken()
        var doToken = tokenizer.nextToken()
        if(!doToken.equals("경기도")){
            return doToken
        }
        var siToken = tokenizer.nextToken()

        return siToken
    }

    fun mapClick(latLng: LatLng){
        var options = MarkerOptions()
        options.position(latLng).title("지정위치").
        icon(BitmapDescriptorFactory.
        defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).snippet("지정위치 1km반경 화장실 표시")
        clickMarker.remove()
        clickMarker = googleMap.addMarker(options)


        var nowLatitude = latLng.latitude
        var nowLongtitude = latLng.longitude

        for(mk: Marker in markerList){
            mk.remove()
        }
        markerList.clear()
        for(data: ToiletData in dataList){
            var distance =
                CalculateDistance().distance(nowLatitude,
                    nowLongtitude,
                    data.REFINE_WGS84_LAT!!,
                    data.REFINE_WGS84_LOGT!!).toInt()
            if(distance <= 1000){
                var latLng = LatLng(data.REFINE_WGS84_LAT!!, data.REFINE_WGS84_LOGT!!)
                var mo = MarkerOptions()
                mo.title(data.PBCTLT_PLC_NM).snippet("세부정보 확인").position(latLng)
                var marker = googleMap.addMarker(mo)
                markerList.add(marker)
            }
        }
    }
}

//앱 실행시 현재위치 위,경도 가져와서 표시해주는 기능
//xml 파싱해서 현재위치 인근 공중 화장실 표시해주는 기능
//표시된 화장실 클릭하면 액티비티 전환해서 코멘트 달아주는 기능