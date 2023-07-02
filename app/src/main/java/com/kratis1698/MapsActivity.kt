package com.kratis1698

//import com.kratis1698.R
import android.Manifest
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var polyline: Polyline

    private lateinit var timer1sec: Timer
    private lateinit var timer10sec: Timer

    private lateinit var sharedPref: SharedPreferences

    private var focusValue = false

    private lateinit var trackAreaPolygon: Polygon

    private lateinit var uphillTrackStartPolygon: Polygon
    private lateinit var uphillTrackFinishPolygon: Polygon
    private lateinit var uphillTrackOutOfRangePolygon: Polygon

    private lateinit var currentlocationInfo: LocationInfo
    private lateinit var locationCallback: LocationCallback


    // 0은 비조교 구역 // 1은 조교구역 // 2는 언덕주로 출발부 진입 // 3은 언덕주로 종점부 도달 상태
    private var myLocationState: Int = 0

    var locationDataList = LocationDataList()

    private var notATraining = 0L
    private var lightTraining = 0L
    private var moderateTraining = 0L
    private var heavyTraining = 0L


    private var startTime: Date? = null
    private var uphillStartTime: Date? = null
    private var uphillFinishTime: Date? = null

    private lateinit var notATrainingTextLabel: TextView
    private lateinit var lightTrainingTextLabel: TextView
    private lateinit var moderateTrainingTextLabel: TextView
    private lateinit var heavyTrainingTextLabel: TextView

    //
    private lateinit var bottomLayout: LinearLayout
    private lateinit var bottomTextLabel: TextView

    private lateinit var mapOverlayScrollLayout: LinearLayout



    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }


    // 구글 맵에서는 가장 빠른 위치 갱신 간격은 1초
    // 여러 센서의 신호를 결합, 분석해서 위치를 판단하기 때문에 더 빠른 속도의 위치 갱신은 미지원
    // GPS가 작동하지 않는 실내 환경에서는, 5초에 한번씩만 위치 갱신
    // (기술, 알고리즘 근본적인 한계이기 때문에 이 점을 고려하여 기능 설계)

    private val locationRequest = createLocationRequest()



    data class LocationInfo(
        val time: Long,  // 측정시간(ms)
        val latitude: Double,  // 위도
        val longitude: Double,  // 경도
        val altitude: Int,  // 고도(해발미터)
        val accuracy: Int,  // 오차범위(미터)
        val speed: Int,  // 속도(당초 초속이나 시속으로 변환하여 저장)
        val bearing: Int  // 방향(0 북쪽, 90 동쪽, 180 남쪽, 270 서쪽)
    )


    private val locationInfoFullList = mutableListOf<LocationInfo>()


    data class LocationData(val time: Long, val latlng: LatLng, val speed: Int)


    class LocationDataList {
        private val dataList = mutableListOf<LocationData>()

        fun add(locationData: LocationData) {
            dataList.add(locationData)
        }

        // 인덱스 값이 0이면 현재 위치, 1,2,3순으로 이전 값들을 반환하는 메서드
        fun getRecent(index: Int): LocationData? {
            if (index >= dataList.size) {
                return null
            }
            return dataList.getOrNull(dataList.lastIndex - index)
        }

        // 최근 10초 이내의 모든 위경도 값을 반환하는 메서드
        fun getRecentLatLngs(): List<LatLng> {
            val latLngs = mutableListOf<LatLng>()
            val timeThreshold = Date().time - 10000L

            for (i in dataList.lastIndex downTo 0) {
                val locationData = dataList[i]
                latLngs.add(locationData.latlng)
                if (locationData.time <= timeThreshold) {
                    break
                }

            }

            return latLngs
        }


    }

    private fun formatTime(timeInMillis: Long): String {
        val seconds = timeInMillis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(map)
            }
        }
    }


    private fun createLocationRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = 1000 // Location updates interval (in milliseconds)
            fastestInterval = 500 // Fastest location update interval (in milliseconds)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Location request priority
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)



        // 화면이 꺼지지 않고 계속 켜져있도록 하는 코드
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


//        sharedpreference는 OnCreate 에서만 정의가 가능하다! 클래스 단에서 초기화 하려고 하면 오류나므로 주의

        sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE
        )

        focusValue = sharedPref.getBoolean("Tracking_Focus", false)


        val toggleButton = findViewById<ToggleButton>(R.id.button_toggle)

        // 현재 fixedFocus 상태에 따라 토글버튼 설정
        toggleButton.isChecked = focusValue

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            // 버튼 클릭 시 fixedFocus 상태 변경
            sharedPref.edit().putBoolean("Tracking_Focus", isChecked).apply()
            focusValue = isChecked
        }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }


//        일반 앱에서는 방해금지 모드를 직접적으로 켜는 방법이 불가능(시스템 앱만 안드로이드 세팅 변경 권한을 가질 수 있다)
//        val mNotificationManager: NotificationManager? = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
//        val mInterruptionFilter: Int = mNotificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_NONE

    //    방해금지모드가 활성화 되어 있을때 true를 반환하는 함수

    private fun isDoNotDisturbModeEnabled(): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }


    private fun mapOverlayInfoText(textMessage: String) {

        val textView = TextView(this)
        textView.text = textMessage
        textView.textSize = 14f
        textView.setTextColor(Color.GRAY)
        mapOverlayScrollLayout.addView(textView)

        // 텍스트 박스 6줄이 넘어가면 상단부터 삭제하도록
        if (mapOverlayScrollLayout.childCount > 6) {
            mapOverlayScrollLayout.removeViewAt(0)
        }


        val file = File(
            applicationContext.filesDir,
            "LogRecord"
        )
        val fileWriter = FileWriter(file, true)


        fileWriter.append(textMessage + "\n")
        fileWriter.flush()
        fileWriter.close()


    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
//        map.mapType = GoogleMap.MAP_TYPE_HYBRID
//        map.mapType = GoogleMap.MAP_TYPE_SATELLITE

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true

//        map.setMinZoomPreference(15.0f)


        val horseNameTextView = findViewById<TextView>(R.id.HorseNameLabel)

        bottomLayout = findViewById(R.id.bottomLayout)
        bottomTextLabel = findViewById(R.id.bottomTextLabel)


        val mapOverlayTextView = findViewById<TextView>(R.id.MapOverlayTextView)
        mapOverlayScrollLayout = findViewById(R.id.MapOverlayScrollLayout)
        mapOverlayScrollLayout.isHorizontalScrollBarEnabled = true

//        intent=getIntent()

        val tempHorsePK = intent.getStringExtra("Horse_PK")
        val tempHorseName = intent.getStringExtra("Horse_Name")
        val tempHorseBY = intent.getStringExtra("Horse_BY")

        val tempUserDiv = sharedPref.getString("User_Div", "")


        runOnUiThread {

            if (tempHorseName!!.endsWith("자마")) {
                horseNameTextView.text = String.format(
                    "%s / %s('%s)",
                    tempHorsePK,
                    tempHorseName,
                    tempHorseBY!!.takeLast(2)
                )
            } else {
                horseNameTextView.text = String.format("%s / %s", tempHorsePK, tempHorseName)
            }
        }



        when (sharedPref.getString("User_Div", "")) {
            "서울 경마장" -> {

                val trackAreaPolygonPoints = listOf(
                    LatLng(37.4517901, 127.0141836),
                    LatLng(37.4512865, 127.0186884),

                    LatLng(37.4485308, 127.0199818),
                    LatLng(37.4474917, 127.0180398),
                    LatLng(37.4434032, 127.0171326),
                    LatLng(37.4439417, 127.0127801),
                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)


            }
            "부경 경마장" -> {


                val trackAreaPolygonPoints = listOf(
                    LatLng(35.1567765, 128.8692325),
                    LatLng(35.1570044, 128.8801655),

                    LatLng(35.1522325, 128.8798325),
                    LatLng(35.1519696, 128.8695009),
                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)

            }


            "제주 목장" -> {
                val trackAreaPolygonPoints = listOf(
//                    LatLng(33.4173393, 126.6810064),
//                    LatLng(33.4172193, 126.6811354),
//                    LatLng(33.4171826, 126.6810798),
//                    LatLng(33.4172973, 126.6809854),

                    LatLng(33.4125522, 126.6702322),
                    LatLng(33.4124268, 126.673286),

                    LatLng(33.4117573, 126.6732317),
                    LatLng(33.411652, 126.6748732),
                    LatLng(33.4106501, 126.6747699),
                    LatLng(33.410762, 126.6731445),

                    LatLng(33.4083429, 126.6729535),
                    LatLng(33.4084414, 126.6697708)


                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)


                val uphillTrackStartPoints = listOf(
                    LatLng(33.4153088,126.6709958),
                    LatLng(33.4153844,126.6699602),

                    LatLng(33.4170353,126.6700321),
                    LatLng(33.4170353,126.6712504),
                )

                val tempUphillStartPolygon = PolygonOptions()
                    .addAll(uphillTrackStartPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackStartPolygon = map.addPolygon(tempUphillStartPolygon)


                val uphillTrackFinishPoints = listOf(
                    LatLng(33.4100999,126.669064),
                    LatLng(33.4101075,126.6698378),

                    LatLng(33.4084596,126.6698289),
                    LatLng(33.408571,126.6690019),
                )

                val tempUphillFinishPolygon = PolygonOptions()
                    .addAll(uphillTrackFinishPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackFinishPolygon = map.addPolygon(tempUphillFinishPolygon)


                val uphillTrackOutOfRangePoints = listOf(
                    LatLng(33.4158663, 126.6714391),
                    LatLng(33.4159522, 126.6722392),

                    LatLng(33.4143784, 126.6724056),
                    LatLng(33.4143586, 126.6715896),
                )

                val tempUphillOutOfRangePolygon = PolygonOptions()
                    .addAll(uphillTrackOutOfRangePoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .strokeWidth(2f)
//                    .strokeColor(Color.TRANSPARENT)

                uphillTrackOutOfRangePolygon = map.addPolygon(tempUphillOutOfRangePolygon)


            }
            "장수 목장" -> {
                val trackAreaPolygonPoints = listOf(
                    LatLng(35.7206846, 127.6401457),
                    LatLng(35.7191748, 127.6419745),

                    LatLng(35.7143368, 127.6367553),
                    LatLng(35.7167141, 127.6336423),
                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)


                val uphillTrackStartPoints = listOf(
                    LatLng(35.7237493, 127.64736),
                    LatLng(35.7234353, 127.6479203),

                    LatLng(35.7229985, 127.6476579),
                    LatLng(35.7232548, 127.6469288),
                )

                val tempUphillStartPolygon = PolygonOptions()
                    .addAll(uphillTrackStartPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackStartPolygon = map.addPolygon(tempUphillStartPolygon)


                val uphillTrackFinishPoints = listOf(
                    LatLng(35.7202243, 127.6512563),
                    LatLng(35.7204004, 127.6518764),

                    LatLng(35.719902, 127.652194),
                    LatLng(35.7197432, 127.6514548),
                )

                val tempUphillFinishPolygon = PolygonOptions()
                    .addAll(uphillTrackFinishPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackFinishPolygon = map.addPolygon(tempUphillFinishPolygon)


                val uphillTrackOutOfRangePoints = listOf(
                    LatLng(35.7236228, 127.6451842),
                    LatLng(35.7224296, 127.6475228),

                    LatLng(35.7216719, 127.6462784),
                    LatLng(35.7225951, 127.6446317),
                )

                val tempUphillOutOfRangePolygon = PolygonOptions()
                    .addAll(uphillTrackOutOfRangePoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .strokeWidth(2f)


                uphillTrackOutOfRangePolygon = map.addPolygon(tempUphillOutOfRangePolygon)


            }
            else -> {
                finish()
            }


        }









        timer1sec = Timer()
        timer10sec = Timer()


        val timerTask1Sec = object : TimerTask() {
            override fun run() {
                runOnUiThread {

                    mapOverlayTextView.text = String.format(
                        "시 간 : %s\n" +
                                "위 도 : %s\n" +
                                "경 도 : %s\n" +
                                "고 도 : %s m\n" +
                                "오 차 : %s m\n" +
                                "속 도 : %s km/h\n" +
                                "방 향 : %s ",
                        SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(currentlocationInfo.time)),
                        currentlocationInfo.latitude,
                        currentlocationInfo.longitude,
                        currentlocationInfo.altitude,
                        currentlocationInfo.accuracy,
                        currentlocationInfo.speed,
                        currentlocationInfo.bearing

                    )


                }

            }
        }


        val timerTask10Sec = object : TimerTask() {
            override fun run() {

                if (!isDoNotDisturbModeEnabled()) {
                    runOnUiThread {

                        val alertDialog = AlertDialog.Builder(this@MapsActivity)
                        alertDialog.setMessage(getString(R.string.DonotDisturbAlarm))

                        alertDialog.setCancelable(false)
                        alertDialog.setPositiveButton("OK") { _, _ ->
                            finish()
                        }
                        alertDialog.show()


                    }

                }


            }


        }


        //        충분한 위치 정보 스택이 쌓였다고 판단되는 5초 후 부터 본격적인 정보 갱신 시작
        timer1sec.schedule(timerTask1Sec, 5000L, 1000L)
        
//        방해금지 모드 미활성화시 해당 액티비티가 작동하지 않도록 함
//        timer10sec.schedule(timerTask10Sec, 0L, 10000L)


        polyline = map.addPolyline(PolylineOptions().apply {
            color(ContextCompat.getColor(applicationContext, R.color.grey))
            width(10f)
            geodesic(true)
        }
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            map.isMyLocationEnabled = true




            fusedLocationClient.lastLocation.addOnSuccessListener { firstLocation ->
                if (firstLocation != null) {


                    val cameraPosition = CameraPosition.Builder()
                        .target(
                            LatLng(
                                sharedPref.getFloat("User_Latitude", 0f).toDouble(),
                                sharedPref.getFloat("User_Longitude", 0f).toDouble()
                            )
                        )
                        .zoom(sharedPref.getFloat("User_ZoomLevel", 17f))
                        .bearing(sharedPref.getFloat("User_Rotation", 0f)) // 로테이션 값 추가
                        .build()

                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

                    map.moveCamera(cameraUpdate)





                    class MyLocationCallback : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            locationResult ?: return
                            for (location in locationResult.locations) {

//                                    아래 코드 블럭은 매 위치값을 "갱신" 할때 마다 자동으로 호출되는 코드


                                currentlocationInfo = LocationInfo(
                                    location.time,
                                    location.latitude,
                                    location.longitude,
                                    location.altitude.toInt(),
                                    location.accuracy.toInt(),
                                    (location.speed * 3.6).toInt(),
                                    location.bearing.toInt()
                                )


                                locationInfoFullList.add(currentlocationInfo)

                                Log.d("MapsActivity", "currentlocationInfo: $currentlocationInfo")


                                
//                                매 순간 좌표 기록 남기도록 임시 수정한 부분
//
//

//                                mapOverlayInfoText(
//                                    String.format(
//                                        "시간 : %s  " +
//                                                "위도 : %s  " +
//                                                "경도 : %s  " +
//                                                "속도 : %s km/h " +
//                                                "방향 : %s",
//                                        SimpleDateFormat(
//                                            "HH:mm:ss",
//                                            Locale.getDefault()
//                                        ).format(Date(currentlocationInfo.time)),
//                                        currentlocationInfo.latitude,
//                                        currentlocationInfo.longitude,
//                                        currentlocationInfo.speed,
//                                        currentlocationInfo.bearing
//
//                                    )
//                                )







                                val currentLatLng =
                                    LatLng(
                                        currentlocationInfo.latitude,
                                        currentlocationInfo.longitude
                                    )


                                locationDataList.add(
                                    LocationData(
                                        currentlocationInfo.time,
                                        currentLatLng,
                                        currentlocationInfo.speed
                                    )
                                )


                                //                                    포커스 On이 설정되어 있을때만 현재 자신의 위치값으로 지속 초기화
                                //                                    animateCamera를 하면 카메라 이동 에니메이션 중 맵update가 되지 않으므로 moveCamera로 해야 함

                                if (focusValue) {
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLng(
                                            currentLatLng
                                        )
                                    )
                                }


                                // 자신의 이동 경로에 현재 좌표를 계속 추가해 주는 코드
                                polyline.points = polyline.points.plus(currentLatLng)


                                // 현재의 속도 값에 따라서 폴리라인의 색을 바꿔주는 코드
                                when {
                                    currentlocationInfo.speed <= 10 -> {
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.grey
                                        )


                                    }
                                    currentlocationInfo.speed <= 20 -> {
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.green
                                        )

                                    }
                                    currentlocationInfo.speed <= 40 -> {
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.orange
                                        )

                                    }
                                    else -> {
                                        // 40km/h초 이상일 때의 동작
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.red
                                        )


                                    }
                                }

                                if (locationDataList.getRecent(1) != null) {


                                    if (tempUserDiv == "제주 목장" || tempUserDiv == "장수 목장") {




                                        //언덕주로 시점부 진입시 처리(to 2)
                                        if (PolyUtil.containsLocation(
                                                currentLatLng,
                                                uphillTrackStartPolygon.points,
                                                true
                                            )
                                        ) {

                                            if (myLocationState != 2) {
                                                myLocationState = 2
                                                uphillStartTime = Date()

                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 언덕주로 진입"
                                                )


                                            }

                                            uphillStartTime = Date()


                                            runOnUiThread {
                                                bottomTextLabel.text = String.format(
                                                    "언덕 출발시간 %s", SimpleDateFormat(
                                                        "HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(uphillStartTime as Date).toString()
                                                )
                                                bottomTextLabel.setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        applicationContext,
                                                        R.color.blue
                                                    )
                                                )


                                            }

                                        }


                                        //언덕주로 종점부 진입시 처리(2 to 3)
                                        if (myLocationState == 2) {

                                            if (PolyUtil.containsLocation(
                                                    currentLatLng,
                                                    uphillTrackFinishPolygon.points,
                                                    true
                                                )
                                            ) {

                                                myLocationState = 3
                                                uphillFinishTime = Date()


                                                val uphillTrainingTime =
                                                    (uphillFinishTime!!.time - uphillStartTime!!.time) / 1000



                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 언덕주로 조교 완료" + String.format(
                                                        "(%s초)",
                                                        uphillTrainingTime
                                                    )
                                                )




                                                runOnUiThread {
                                                    bottomTextLabel.text = String.format(
                                                        "출발 %s / 조교 %s초",
                                                        SimpleDateFormat(
                                                            "HH:mm:ss",
                                                            Locale.getDefault()
                                                        ).format(uphillStartTime as Date)
                                                            .toString(),
                                                        uphillTrainingTime.toString()
                                                    )
                                                    bottomTextLabel.setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            applicationContext,
                                                            R.color.green
                                                        )
                                                    )

                                                }


                                                val file =
                                                    File(
                                                        applicationContext.filesDir,
                                                        "TrainingRecord"
                                                    )
                                                val fileWriter = FileWriter(file, true)

                                                val todayDateFormat =
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd",
                                                        Locale.getDefault()
                                                    )

                                                //                                            val tempUserPK = sharedPref.getString("User_PK", "미확인")

                                                val tempUserName =
                                                    sharedPref.getString("User_Name", "미확인")

                                                val startTimeFormat = SimpleDateFormat(
                                                    "HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(uphillStartTime as Date).toString()


                                                fileWriter.append(
                                                    "${
                                                        todayDateFormat.format(
                                                            uphillStartTime as Date
                                                        )
                                                    }, $startTimeFormat, 언덕, $tempUserName, $tempHorseName, $uphillTrainingTime\n"
                                                )

                                                fileWriter.flush()

                                                fileWriter.close()


                                            }
                                        }


                                        //언덕주로 조교 종료시 처리(2 to 0, 3 to 0)
                                        if (myLocationState == 2 || myLocationState == 3) {

                                            if (PolyUtil.containsLocation(
                                                    currentLatLng,
                                                    uphillTrackOutOfRangePolygon.points,
                                                    true
                                                )
                                            ) {
                                                myLocationState = 0



                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 언덕주로 조교 종료"
                                                )




                                                runOnUiThread {
                                                    bottomTextLabel.text =
                                                        getString(R.string.bottomText)
                                                    bottomTextLabel.setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            applicationContext,
                                                            R.color.grey
                                                        )
                                                    )

                                                }


                                            }
                                        }


                                    }


                                    // 조교 구역 진입시 처리(to 1)

                                    if ((PolyUtil.containsLocation(
                                            currentLatLng,
                                            trackAreaPolygon.points,
                                            true
                                        ) && myLocationState!= 2)
                                    ) {

                                        if (myLocationState != 1) {
                                            myLocationState = 1
                                            startTime = Date()

                                            mapOverlayInfoText(
                                                SimpleDateFormat(
                                                    "yyyy-MM-dd, HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(Date()).toString()
                                                        + " 조교 구역 진입"

                                            )


                                            notATrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.grey
                                                        )
                                                    )
                                                }



                                            lightTrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.green
                                                        )
                                                    )
                                                }

                                            moderateTrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.orange
                                                        )
                                                    )
                                                }

                                            heavyTrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.red
                                                        )
                                                    )

                                                }

                                            bottomLayout.removeView(bottomTextLabel)

                                            bottomLayout.addView(notATrainingTextLabel)
                                            bottomLayout.addView(lightTrainingTextLabel)
                                            bottomLayout.addView(moderateTrainingTextLabel)
                                            bottomLayout.addView(heavyTrainingTextLabel)


                                        }






                                        when {
                                            currentlocationInfo.speed <= 10 -> {
                                                notATraining += (locationDataList.getRecent(0)!!.time - locationDataList.getRecent(
                                                    1
                                                )!!.time)


                                                runOnUiThread {
                                                    notATrainingTextLabel.text =
                                                        formatTime(notATraining)
                                                }

                                            }

                                            currentlocationInfo.speed <= 20 -> {

                                                lightTraining += (locationDataList.getRecent(0)!!.time - locationDataList.getRecent(
                                                    1
                                                )!!.time)

                                                runOnUiThread {
                                                    lightTrainingTextLabel.text =
                                                        formatTime(lightTraining)
                                                }

                                            }


                                            currentlocationInfo.speed <= 40 -> {
                                                moderateTraining += (locationDataList.getRecent(
                                                    0
                                                )!!.time - locationDataList.getRecent(1)!!.time)

                                                runOnUiThread {
                                                    moderateTrainingTextLabel.text =
                                                        formatTime(moderateTraining)
                                                }

                                            }


                                            else -> {
                                                // 40km/h초 이상일 때의 동작
                                                heavyTraining += (locationDataList.getRecent(0)!!.time - locationDataList.getRecent(
                                                    1
                                                )!!.time)

                                                runOnUiThread {
                                                    heavyTrainingTextLabel.text =
                                                        formatTime(heavyTraining)
                                                }

                                            }


                                        }


                                    }


                                    // 조교구역에 있는 상태(1번 상태)인 채로 조교구역을 벗어나는 상황을 감지(1 to 0)
                                    if (myLocationState == 1 && !PolyUtil.containsLocation(
                                            currentLatLng,
                                            trackAreaPolygon.points,
                                            true
                                        )
                                    ) {


                                        val latLngsList = locationDataList.getRecentLatLngs()


                                        if (latLngsList.all {
                                                !PolyUtil.containsLocation(
                                                    it,
                                                    trackAreaPolygon.points,
                                                    true
                                                )
                                            }
                                        ) {

                                            mapOverlayInfoText(
                                                SimpleDateFormat(
                                                    "yyyy-MM-dd, HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(Date()).toString()
                                                        + " 조교 구역 이탈(10초 이상)"
                                            )

                                            val totalTrainingTime =
                                                (notATraining / 1000) + (lightTraining / 1000) + (moderateTraining / 1000) + (heavyTraining / 1000)


                                            if (totalTrainingTime >= 60) {

                                                val file = File(
                                                    applicationContext.filesDir,
                                                    "TrainingRecord"
                                                )
                                                val fileWriter = FileWriter(file, true)

                                                val todayDateFormat = SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    Locale.getDefault()
                                                )


                                                val tempUserName =
                                                    sharedPref.getString("User_Name", "미확인")

                                                val startTimeFormat = SimpleDateFormat(
                                                    "HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(startTime as Date).toString()

                                                fileWriter.append(
                                                    "${
                                                        todayDateFormat.format(
                                                            startTime as Date
                                                        )
                                                    }, $startTimeFormat, 조교, $tempUserName, $tempHorseName, ${notATraining / 1000}, ${lightTraining / 1000}, ${moderateTraining / 1000}, ${heavyTraining / 1000}, ${notATraining / 1000 + lightTraining / 1000 + moderateTraining / 1000 + heavyTraining / 1000}\n"
                                                )



                                                fileWriter.flush()

                                                fileWriter.close()

                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 조교 기록 저장 완료" +
                                                            String.format(
                                                                "(%s초)",
                                                                totalTrainingTime.toString()
                                                            )
                                                )


                                            } else {

                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 조교 기록 미저장(60초 미만)"
                                                )


                                            }





                                            myLocationState = 0

                                            startTime = null

                                            notATraining = 0L
                                            lightTraining = 0L
                                            moderateTraining = 0L
                                            heavyTraining = 0L



                                            bottomLayout.removeView(notATrainingTextLabel)
                                            bottomLayout.removeView(lightTrainingTextLabel)
                                            bottomLayout.removeView(moderateTrainingTextLabel)
                                            bottomLayout.removeView(heavyTrainingTextLabel)

                                            bottomLayout.addView(bottomTextLabel)


                                            runOnUiThread {
                                                bottomTextLabel.text =
                                                    getString(R.string.bottomText)
                                                bottomTextLabel.setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        applicationContext,
                                                        R.color.grey
                                                    )
                                                )

                                            }


                                        }


                                    }
                                }


                            }
                        }

                    }


                    locationCallback = MyLocationCallback()

                    // Request location updates
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )


                }
            }
        }
    }



    private fun takeScreenshot() {

        val scrollView = findViewById<ScrollView>(R.id.MapOverlayScrollView)
        scrollView.visibility = View.GONE

        val mapOverlayLayout = findViewById<LinearLayout>(R.id.MapOverlayScrollLayout)
        mapOverlayLayout.visibility = View.GONE



        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(33.41304652108367,126.67181611061095), 16.129726f
            )
        )





        val rootView = findViewById<ViewGroup>(R.id.ScreenView)

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundDrawable = rootView.background
        backgroundDrawable?.draw(canvas)
        rootView.draw(canvas)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            googleMap.snapshot { mapBitmap ->
                // Combine the bitmaps
                val combinedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
                val combinedCanvas = Canvas(combinedBitmap)
                combinedCanvas.drawBitmap(mapBitmap!!, 0f, 0f, null)
                combinedCanvas.drawBitmap(bitmap, 0f, 0f, null)

                // Save the combined bitmap
                val displayName = "screenshot_${System.currentTimeMillis()}.jpg"
                val contentResolver = applicationContext.contentResolver
                val imageCollection =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val imageDetails = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/training"
                    )
                }

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val imageUri = contentResolver.insert(imageCollection, imageDetails)

                        imageUri?.let {
                            contentResolver.openOutputStream(it)?.use { outputStream ->
                                combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            }
                        }
                    }
                }
            }
        }
    }



//    private fun takeScreenshot() {
//
//        map.moveCamera(
//            CameraUpdateFactory.newLatLngZoom(
//                LatLng(33.41304652108367,126.67181611061095), 16.129726f
//            )
//        )
//
//
//        map.snapshot { bitmap ->
//            val displayName = "screenshot_${System.currentTimeMillis()}.jpg"
//            val contentResolver = applicationContext.contentResolver
//            val imageCollection =
//                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//
//            val imageDetails = ContentValues().apply {
//                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
//                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//                put(
//                    MediaStore.Images.Media.RELATIVE_PATH,
//                    Environment.DIRECTORY_PICTURES + "/training"
//                )
//            }
//
//            lifecycleScope.launch {
//                withContext(Dispatchers.IO) {
//                    val imageUri = contentResolver.insert(imageCollection, imageDetails)
//
//                    imageUri?.let {
//                        contentResolver.openOutputStream(it)?.use { outputStream ->
//                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//                        }
//                    }
//                }
//            }
//        }
//    }


    override fun onBackPressed() {

        takeScreenshot()




        if (myLocationState == 1) {


            if ((notATraining / 1000) + (lightTraining / 1000) + (moderateTraining / 1000) + (heavyTraining / 1000) <= 60) {

                runOnUiThread {
                    val destroyAlertDialog = AlertDialog.Builder(this@MapsActivity)
                        .setMessage(getString(R.string.noRecordAlert))
                        .setCancelable(true)
                        .setPositiveButton("OK") { _, _ ->
                            finish()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                    destroyAlertDialog.show()
                }


            } else {

                runOnUiThread {
                    val destroyAlertDialog = AlertDialog.Builder(this@MapsActivity)
                        .setMessage(getString(R.string.backPressedAutoSave))
                        .setCancelable(true)
                        .setPositiveButton("OK") { _, _ ->


                            val file = File(
                                applicationContext.filesDir,
                                "TrainingRecord"
                            )
                            val fileWriter = FileWriter(file, true)

                            val todayDateFormat = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            )


                            val tempUserName =
                                sharedPref.getString("User_Name", "미확인")

                            val tempHorseName = intent.getStringExtra("Horse_Name")


                            val startTimeFormat = SimpleDateFormat(
                                "HH:mm:ss",
                                Locale.getDefault()
                            ).format(startTime as Date).toString()

                            fileWriter.append(
                                "${
                                    todayDateFormat.format(
                                        startTime as Date
                                    )
                                }, $startTimeFormat, 조교, $tempUserName, $tempHorseName, ${notATraining / 1000}, ${lightTraining / 1000}, ${moderateTraining / 1000}, ${heavyTraining / 1000}, ${notATraining / 1000 + lightTraining / 1000 + moderateTraining / 1000 + heavyTraining / 1000}\n"
                            )

                            fileWriter.flush()
                            fileWriter.close()


                            finish()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                    destroyAlertDialog.show()
                }
            }


        } else {

            finish()

        }


    }


    override fun onDestroy() {
        timer1sec.cancel()
        timer10sec.cancel()

        takeScreenshot()

        val currentCameraPosition = map.cameraPosition


        if ((notATraining / 1000) + (lightTraining / 1000) + (moderateTraining / 1000) + (heavyTraining / 1000) >= 60) {

            val file = File(
                applicationContext.filesDir,
                "TrainingRecord"
            )
            val fileWriter = FileWriter(file, true)

            val todayDateFormat = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )


            val tempUserName =
                sharedPref.getString("User_Name", "미확인")

            val tempHorseName = intent.getStringExtra("Horse_Name")


            val startTimeFormat = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(startTime as Date).toString()

            fileWriter.append(
                "${
                    todayDateFormat.format(
                        startTime as Date
                    )
                }, $startTimeFormat, 조교, $tempUserName, $tempHorseName, ${notATraining / 1000}, ${lightTraining / 1000}, ${moderateTraining / 1000}, ${heavyTraining / 1000}, ${notATraining / 1000 + lightTraining / 1000 + moderateTraining / 1000 + heavyTraining / 1000}\n"
            )

            fileWriter.flush()
            fileWriter.close()


            finish()
        }





        fusedLocationClient.removeLocationUpdates(locationCallback)

//        Log.d("Cameralog", currentCameraPosition.target.latitude.toString())
//        Log.d("Cameralog", currentCameraPosition.target.longitude.toString())
//        Log.d("Cameralog", currentCameraPosition.zoom.toString())
//        Log.d("Cameralog", currentCameraPosition.bearing.toString())

        sharedPref.edit()
            .putFloat("User_Latitude", currentCameraPosition.target.latitude.toFloat())
            .putFloat("User_Longitude", currentCameraPosition.target.longitude.toFloat())
            .putFloat("User_ZoomLevel", currentCameraPosition.zoom)
            .putFloat("User_Rotation", currentCameraPosition.bearing)
            .apply()


        super.onDestroy()

    }

}











/*
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var polyline: Polyline




    private lateinit var timer1sec: Timer
    private lateinit var timer10sec: Timer

    private lateinit var sharedPref: SharedPreferences

    private var focusValue = false

    private lateinit var trackAreaPolygon: Polygon

    private lateinit var uphillTrackStartPolygon: Polygon
    private lateinit var uphillTrackFinishPolygon: Polygon
    private lateinit var uphillTrackOutOfRangePolygon: Polygon

    private lateinit var currentlocationInfo: LocationInfo
    private lateinit var locationCallback: LocationCallback


    // 0은 비조교 구역 // 1은 조교구역 // 2는 언덕주로 출발부 진입 // 3은 언덕주로 종점부 도달 상태
    private var myLocationState: Int = 0

    var locationDataList = LocationDataList()

    private var notATraining = 0L
    private var lightTraining = 0L
    private var moderateTraining = 0L
    private var heavyTraining = 0L


    private var startTime: Date? = null
    private var uphillStartTime: Date? = null
    private var uphillFinishTime: Date? = null

    private lateinit var notATrainingTextLabel: TextView
    private lateinit var lightTrainingTextLabel: TextView
    private lateinit var moderateTrainingTextLabel: TextView
    private lateinit var heavyTrainingTextLabel: TextView

    //
    private lateinit var bottomLayout: LinearLayout
    private lateinit var bottomTextLabel: TextView

    private lateinit var mapOverlayScrollLayout: LinearLayout


    private lateinit var trackingService: MapTrackingService



    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }


    // 가장 빠른 위치 갱신 간격은 1초
    // 여러 센서의 신호를 결합, 분석해서 위치를 판단하기 때문에 더 빠른 속도의 위치 갱신은 불필요
    // GPS가 작동하지 않는 실내 환경에서는, 5초에 한번씩만 위치를 갱신한다.
    // (기술, 알고리즘 근본적인 한계이기 때문에 이 점을 고려하여 기능 설계)

    private val locationRequest = createLocationRequest()


    //위도 경도 고도(해발미터) 정확도(오차범위 미터) 속도(당초 단위는 m/s -> 시속으로 환산하여 저장),  bearing 방향(0~360) 값도 있으나 불필요하다고 판단

    data class LocationInfo(
        val time: Long,
        val latitude: Double,
        val longitude: Double,
        val altitude: Int,
        val accuracy: Int,
        val speed: Int,
    )


    data class LocationData(val time: Long, val latlng: LatLng, val speed: Int)


    class LocationDataList {
        private val dataList = mutableListOf<LocationData>()

        fun add(locationData: LocationData) {
            dataList.add(locationData)
        }

        // 인덱스 값이 0이면 현재 위치, 1,2,3순으로 이전 값들을 반환하는 메서드
        fun getRecent(index: Int): LocationData? {
            if (index >= dataList.size) {
                return null
            }
            return dataList.getOrNull(dataList.lastIndex - index)
        }

        // 최근 10초 이내의 모든 위경도 값을 반환하는 메서드
        fun getRecentLatLngs(): List<LatLng> {
            val latLngs = mutableListOf<LatLng>()
            val timeThreshold = Date().time - 10000L

            for (i in dataList.lastIndex downTo 0) {
                val locationData = dataList[i]
                latLngs.add(locationData.latlng)
                if (locationData.time <= timeThreshold) {
                    break
                }

            }

            return latLngs
        }


    }

    private fun formatTime(timeInMillis: Long): String {
        val seconds = timeInMillis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(map)
            }
        }
    }


    private fun createLocationRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = 1000 // Location updates interval (in milliseconds)
            fastestInterval = 500 // Fastest location update interval (in milliseconds)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Location request priority
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // 화면이 꺼지지 않고 계속 켜져있도록 하는 코드
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


//        sharedpreference는 OnCreate 에서만 정의가 가능하다! 클래스 단에서 초기화 하려고 하면 오류나므로 주의

        sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE
        )

        focusValue = sharedPref.getBoolean("Tracking_Focus", false)


        val toggleButton = findViewById<ToggleButton>(R.id.button_toggle)

        // 현재 fixedFocus 상태에 따라 토글버튼 설정
        toggleButton.isChecked = focusValue

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            // 버튼 클릭 시 fixedFocus 상태 변경
            sharedPref.edit().putBoolean("Tracking_Focus", isChecked).apply()
            focusValue = isChecked
        }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        val intent = Intent(this, MapTrackingService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)



    }


//        일반 앱에서는 방해금지 모드를 직접적으로 켜는 방법이 불가능(시스템 앱만 안드로이드 세팅 변경 권한을 가질 수 있다)
//        val mNotificationManager: NotificationManager? = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
//        val mInterruptionFilter: Int = mNotificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_NONE

    //    방해금지모드가 활성화 되어 있을때 true를 반환하는 함수

    private fun isDoNotDisturbModeEnabled(): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }


    private fun mapOverlayInfoText(textMessage: String) {

        val textView = TextView(this)
        textView.text = textMessage
        textView.textSize = 14f
        textView.setTextColor(Color.GRAY)
        mapOverlayScrollLayout.addView(textView)

        // 텍스트 박스 6줄이 넘어가면 상단부터 삭제하도록
        if (mapOverlayScrollLayout.childCount > 6) {
            mapOverlayScrollLayout.removeViewAt(0)
        }


        val file = File(
            applicationContext.filesDir,
            "LogRecord"
        )
        val fileWriter = FileWriter(file, true)


        fileWriter.append(textMessage + "\n")
        fileWriter.flush()
        fileWriter.close()


    }


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MapTrackingService.LocationBinder
            trackingService = binder.getService()





            // Service 에서 액티비티의 polyline 객체를 가져와 사용할 수 있음
//            val polyline = trackingService.getPolyline()

            // TODO: 서비스에서 polyline 객체를 사용하여 위치 정보 추가 등을 구현



        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // TODO: 서비스와 연결이 끊겼을 때의 처리 구현
        }
    }




    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
//        map.mapType = GoogleMap.MAP_TYPE_HYBRID
//        map.mapType = GoogleMap.MAP_TYPE_SATELLITE

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true

//        map.setMinZoomPreference(15.0f)


        val horseNameTextView = findViewById<TextView>(R.id.HorseNameLabel)

        bottomLayout = findViewById(R.id.bottomLayout)
        bottomTextLabel = findViewById(R.id.bottomTextLabel)


        val mapOverlayTextView = findViewById<TextView>(R.id.MapOverlayTextView)
        mapOverlayScrollLayout = findViewById(R.id.MapOverlayScrollLayout)
        mapOverlayScrollLayout.isHorizontalScrollBarEnabled = true

//        intent=getIntent()

        val tempHorsePK = intent.getStringExtra("Horse_PK")
        val tempHorseName = intent.getStringExtra("Horse_Name")
        val tempHorseBY = intent.getStringExtra("Horse_BY")

        val tempUserDiv = sharedPref.getString("User_Div", "")


        runOnUiThread {

            if (tempHorseName!!.endsWith("자마")) {
                horseNameTextView.text = String.format(
                    "%s / %s('%s)",
                    tempHorsePK,
                    tempHorseName,
                    tempHorseBY!!.takeLast(2)
                )
            } else {
                horseNameTextView.text = String.format("%s / %s", tempHorsePK, tempHorseName)
            }
        }



        when (sharedPref.getString("User_Div", "")) {
            "서울 경마장" -> {

                val trackAreaPolygonPoints = listOf(
                    LatLng(37.4517901, 127.0141836),
                    LatLng(37.4512865, 127.0186884),

                    LatLng(37.4485308, 127.0199818),
                    LatLng(37.4474917, 127.0180398),
                    LatLng(37.4434032, 127.0171326),
                    LatLng(37.4439417, 127.0127801),
                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)


            }
            "부경 경마장" -> {


                val trackAreaPolygonPoints = listOf(
                    LatLng(35.1567765, 128.8692325),
                    LatLng(35.1570044, 128.8801655),

                    LatLng(35.1522325, 128.8798325),
                    LatLng(35.1519696, 128.8695009),
                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)

            }


            "제주 목장" -> {
                val trackAreaPolygonPoints = listOf(
//                    LatLng(33.4173393, 126.6810064),
//                    LatLng(33.4172193, 126.6811354),
//                    LatLng(33.4171826, 126.6810798),
//                    LatLng(33.4172973, 126.6809854),

                    LatLng(33.4125522, 126.6702322),
                    LatLng(33.4124268, 126.673286),

                    LatLng(33.4117573, 126.6732317),
                    LatLng(33.411652, 126.6748732),
                    LatLng(33.4106501, 126.6747699),
                    LatLng(33.410762, 126.6731445),

                    LatLng(33.4083429, 126.6729535),
                    LatLng(33.4084414, 126.6697708)


                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)


                val uphillTrackStartPoints = listOf(
                    LatLng(33.4153088,126.6709958),
                    LatLng(33.4153844,126.6699602),

                    LatLng(33.4170353,126.6700321),
                    LatLng(33.4170353,126.6712504),
                )

                val tempUphillStartPolygon = PolygonOptions()
                    .addAll(uphillTrackStartPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackStartPolygon = map.addPolygon(tempUphillStartPolygon)


                val uphillTrackFinishPoints = listOf(
                    LatLng(33.4100999,126.669064),
                    LatLng(33.4101075,126.6698378),

                    LatLng(33.4084596,126.6698289),
                    LatLng(33.408571,126.6690019),
                )

                val tempUphillFinishPolygon = PolygonOptions()
                    .addAll(uphillTrackFinishPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackFinishPolygon = map.addPolygon(tempUphillFinishPolygon)


                val uphillTrackOutOfRangePoints = listOf(
                    LatLng(33.4158663, 126.6714391),
                    LatLng(33.4159522, 126.6722392),

                    LatLng(33.4143784, 126.6724056),
                    LatLng(33.4143586, 126.6715896),
                )

                val tempUphillOutOfRangePolygon = PolygonOptions()
                    .addAll(uphillTrackOutOfRangePoints)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .strokeWidth(2f)


                uphillTrackOutOfRangePolygon = map.addPolygon(tempUphillOutOfRangePolygon)


            }
            "장수 목장" -> {
                val trackAreaPolygonPoints = listOf(
                    LatLng(35.7206846, 127.6401457),
                    LatLng(35.7191748, 127.6419745),

                    LatLng(35.7143368, 127.6367553),
                    LatLng(35.7167141, 127.6336423),
                )

                val tempPolygon = PolygonOptions()
                    .addAll(trackAreaPolygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeWidth(2f)

                trackAreaPolygon = map.addPolygon(tempPolygon)


                val uphillTrackStartPoints = listOf(
                    LatLng(35.7237493, 127.64736),
                    LatLng(35.7234353, 127.6479203),

                    LatLng(35.7229985, 127.6476579),
                    LatLng(35.7232548, 127.6469288),
                )

                val tempUphillStartPolygon = PolygonOptions()
                    .addAll(uphillTrackStartPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackStartPolygon = map.addPolygon(tempUphillStartPolygon)


                val uphillTrackFinishPoints = listOf(
                    LatLng(35.7202243, 127.6512563),
                    LatLng(35.7204004, 127.6518764),

                    LatLng(35.719902, 127.652194),
                    LatLng(35.7197432, 127.6514548),
                )

                val tempUphillFinishPolygon = PolygonOptions()
                    .addAll(uphillTrackFinishPoints)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255))
                    .strokeWidth(2f)

                uphillTrackFinishPolygon = map.addPolygon(tempUphillFinishPolygon)


                val uphillTrackOutOfRangePoints = listOf(
                    LatLng(35.7236228, 127.6451842),
                    LatLng(35.7224296, 127.6475228),

                    LatLng(35.7216719, 127.6462784),
                    LatLng(35.7225951, 127.6446317),
                )

                val tempUphillOutOfRangePolygon = PolygonOptions()
                    .addAll(uphillTrackOutOfRangePoints)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .strokeWidth(2f)


                uphillTrackOutOfRangePolygon = map.addPolygon(tempUphillOutOfRangePolygon)


            }
            else -> {
                finish()
            }


        }









        timer1sec = Timer()
        timer10sec = Timer()


        val timerTask1Sec = object : TimerTask() {
            override fun run() {

                polyline.points = trackingService.getLatLngList()
                runOnUiThread {



                    mapOverlayTextView.text = String.format(
                        "시 간 : %s\n" +
                                "위 도 : %s\n" +
                                "경 도 : %s\n" +
                                "고 도 : %s m\n" +
                                "오 차 : %s m\n" +
                                "속 도 : %s km/h",
                        SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(currentlocationInfo.time)),
                        currentlocationInfo.latitude,
                        currentlocationInfo.longitude,
                        currentlocationInfo.altitude,
                        currentlocationInfo.accuracy,
                        currentlocationInfo.speed
                    )


                }

            }
        }


        val timerTask10Sec = object : TimerTask() {
            override fun run() {

                if (!isDoNotDisturbModeEnabled()) {
                    runOnUiThread {

                        val alertDialog = AlertDialog.Builder(this@MapsActivity)
                        alertDialog.setMessage(getString(R.string.DonotDisturbAlarm))

                        alertDialog.setCancelable(false)
                        alertDialog.setPositiveButton("OK") { _, _ ->
                            finish()
                        }
                        alertDialog.show()


                    }

                }


            }


        }


        //        충분한 위치 정보 스택이 쌓였다고 판단되는 5초 후 부터 본격적인 정보 갱신 시작
        timer1sec.schedule(timerTask1Sec, 5000L, 1000L)
//        timer10sec.schedule(timerTask10Sec, 0L, 10000L)


        polyline = map.addPolyline(PolylineOptions().apply {
            color(ContextCompat.getColor(applicationContext, R.color.grey))
            width(10f)
            geodesic(true)
        }
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            map.isMyLocationEnabled = true




            fusedLocationClient.lastLocation.addOnSuccessListener { firstLocation ->
                if (firstLocation != null) {


                    val cameraPosition = CameraPosition.Builder()
                        .target(
                            LatLng(
                                sharedPref.getFloat("User_Latitude", 0f).toDouble(),
                                sharedPref.getFloat("User_Longitude", 0f).toDouble()
                            )
                        )
                        .zoom(sharedPref.getFloat("User_ZoomLevel", 17f))
                        .bearing(sharedPref.getFloat("User_Rotation", 0f)) // 로테이션 값 추가
                        .build()

                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

                    map.moveCamera(cameraUpdate)





                    class MyLocationCallback : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            locationResult ?: return
                            for (location in locationResult.locations) {

//                                    아래 코드 블럭은 매 위치값을 "갱신" 할때 마다 자동으로 호출되는 코드


                                currentlocationInfo = LocationInfo(
                                    location.time,
                                    location.latitude,
                                    location.longitude,
                                    location.altitude.toInt(),
                                    location.accuracy.toInt(),
                                    (location.speed * 3.6).toInt(),
                                )


                                val currentLatLng =
                                    LatLng(
                                        currentlocationInfo.latitude,
                                        currentlocationInfo.longitude
                                    )


                                locationDataList.add(
                                    LocationData(
                                        currentlocationInfo.time,
                                        currentLatLng,
                                        currentlocationInfo.speed
                                    )
                                )


                                //                                    포커스 On이 설정되어 있을때만 현재 자신의 위치값으로 지속 초기화
                                //                                    animateCamera를 하면 카메라 이동 에니메이션 중 맵update가 되지 않으므로 moveCamera로 해야 함

                                if (focusValue) {
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLng(
                                            currentLatLng
                                        )
                                    )
                                }


                                // 자신의 이동 경로에 현재 좌표를 계속 추가해 주는 코드
//                                polyline.points = polyline.points.plus(currentLatLng)

                                polyline.points = trackingService.getLatLngList()







                                // 현재의 속도 값에 따라서 폴리라인의 색을 바꿔주는 코드
                                when {
                                    currentlocationInfo.speed <= 10 -> {
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.grey
                                        )


                                    }
                                    currentlocationInfo.speed <= 20 -> {
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.green
                                        )

                                    }
                                    currentlocationInfo.speed <= 40 -> {
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.orange
                                        )

                                    }
                                    else -> {
                                        // 40km/h초 이상일 때의 동작
                                        polyline.color = ContextCompat.getColor(
                                            applicationContext,
                                            R.color.red
                                        )


                                    }
                                }

                                if (locationDataList.getRecent(1) != null) {


                                    if (tempUserDiv == "제주 목장" || tempUserDiv == "장수 목장") {




                                        //언덕주로 시점부 진입시 처리(to 2)
                                        if (PolyUtil.containsLocation(
                                                currentLatLng,
                                                uphillTrackStartPolygon.points,
                                                true
                                            )
                                        ) {

                                            if (myLocationState != 2) {
                                                myLocationState = 2
                                                uphillStartTime = Date()

                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 언덕주로 진입"
                                                )


                                            }

                                            uphillStartTime = Date()


                                            runOnUiThread {
                                                bottomTextLabel.text = String.format(
                                                    "언덕 출발시간 %s", SimpleDateFormat(
                                                        "HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(uphillStartTime as Date).toString()
                                                )
                                                bottomTextLabel.setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        applicationContext,
                                                        R.color.blue
                                                    )
                                                )


                                            }

                                        }


                                        //언덕주로 종점부 진입시 처리(2 to 3)
                                        if (myLocationState == 2) {

                                            if (PolyUtil.containsLocation(
                                                    currentLatLng,
                                                    uphillTrackFinishPolygon.points,
                                                    true
                                                )
                                            ) {

                                                myLocationState = 3
                                                uphillFinishTime = Date()


                                                val uphillTrainingTime =
                                                    (uphillFinishTime!!.time - uphillStartTime!!.time) / 1000



                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 언덕주로 조교 완료" + String.format(
                                                        "(%s초)",
                                                        uphillTrainingTime
                                                    )
                                                )




                                                runOnUiThread {
                                                    bottomTextLabel.text = String.format(
                                                        "출발 %s / 조교 %s초",
                                                        SimpleDateFormat(
                                                            "HH:mm:ss",
                                                            Locale.getDefault()
                                                        ).format(uphillStartTime as Date)
                                                            .toString(),
                                                        uphillTrainingTime.toString()
                                                    )
                                                    bottomTextLabel.setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            applicationContext,
                                                            R.color.green
                                                        )
                                                    )

                                                }


                                                val file =
                                                    File(
                                                        applicationContext.filesDir,
                                                        "TrainingRecord"
                                                    )
                                                val fileWriter = FileWriter(file, true)

                                                val todayDateFormat =
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd",
                                                        Locale.getDefault()
                                                    )

                                                //                                            val tempUserPK = sharedPref.getString("User_PK", "미확인")

                                                val tempUserName =
                                                    sharedPref.getString("User_Name", "미확인")

                                                val startTimeFormat = SimpleDateFormat(
                                                    "HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(uphillStartTime as Date).toString()


                                                fileWriter.append(
                                                    "${
                                                        todayDateFormat.format(
                                                            uphillStartTime as Date
                                                        )
                                                    }, $startTimeFormat, 언덕, $tempUserName, $tempHorseName, $uphillTrainingTime\n"
                                                )

                                                fileWriter.flush()

                                                fileWriter.close()


                                            }
                                        }


                                        //언덕주로 조교 종료시 처리(2 to 0, 3 to 0)
                                        if (myLocationState == 2 || myLocationState == 3) {

                                            if (PolyUtil.containsLocation(
                                                    currentLatLng,
                                                    uphillTrackOutOfRangePolygon.points,
                                                    true
                                                )
                                            ) {
                                                myLocationState = 0



                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 언덕주로 조교 종료"
                                                )




                                                runOnUiThread {
                                                    bottomTextLabel.text =
                                                        getString(R.string.bottomText)
                                                    bottomTextLabel.setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            applicationContext,
                                                            R.color.grey
                                                        )
                                                    )

                                                }


                                            }
                                        }


                                    }


                                    // 조교 구역 진입시 처리(to 1)

                                    if ((PolyUtil.containsLocation(
                                            currentLatLng,
                                            trackAreaPolygon.points,
                                            true
                                        ) && myLocationState!= 2)
                                    ) {

                                        if (myLocationState != 1) {
                                            myLocationState = 1
                                            startTime = Date()

                                            mapOverlayInfoText(
                                                SimpleDateFormat(
                                                    "yyyy-MM-dd, HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(Date()).toString()
                                                        + " 조교 구역 진입"

                                            )


                                            notATrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.grey
                                                        )
                                                    )
                                                }



                                            lightTrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.green
                                                        )
                                                    )
                                                }

                                            moderateTrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.orange
                                                        )
                                                    )
                                                }

                                            heavyTrainingTextLabel =
                                                TextView(this@MapsActivity).apply {
                                                    layoutParams = LinearLayout.LayoutParams(
                                                        0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        1f
                                                    )
                                                    text = context.getString(R.string.zero000)
                                                    gravity = Gravity.CENTER
                                                    textSize = 20f
                                                    setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.red
                                                        )
                                                    )

                                                }

                                            bottomLayout.removeView(bottomTextLabel)

                                            bottomLayout.addView(notATrainingTextLabel)
                                            bottomLayout.addView(lightTrainingTextLabel)
                                            bottomLayout.addView(moderateTrainingTextLabel)
                                            bottomLayout.addView(heavyTrainingTextLabel)


                                        }






                                        when {
                                            currentlocationInfo.speed <= 10 -> {
                                                notATraining += (locationDataList.getRecent(0)!!.time - locationDataList.getRecent(
                                                    1
                                                )!!.time)


                                                runOnUiThread {
                                                    notATrainingTextLabel.text =
                                                        formatTime(notATraining)
                                                }

                                            }

                                            currentlocationInfo.speed <= 20 -> {

                                                lightTraining += (locationDataList.getRecent(0)!!.time - locationDataList.getRecent(
                                                    1
                                                )!!.time)

                                                runOnUiThread {
                                                    lightTrainingTextLabel.text =
                                                        formatTime(lightTraining)
                                                }

                                            }


                                            currentlocationInfo.speed <= 40 -> {
                                                moderateTraining += (locationDataList.getRecent(
                                                    0
                                                )!!.time - locationDataList.getRecent(1)!!.time)

                                                runOnUiThread {
                                                    moderateTrainingTextLabel.text =
                                                        formatTime(moderateTraining)
                                                }

                                            }


                                            else -> {
                                                // 40km/h초 이상일 때의 동작
                                                heavyTraining += (locationDataList.getRecent(0)!!.time - locationDataList.getRecent(
                                                    1
                                                )!!.time)

                                                runOnUiThread {
                                                    heavyTrainingTextLabel.text =
                                                        formatTime(heavyTraining)
                                                }

                                            }


                                        }


                                    }


                                    // 조교구역에 있는 상태(1번 상태)인 채로 조교구역을 벗어나는 상황을 감지(1 to 0)
                                    if (myLocationState == 1 && !PolyUtil.containsLocation(
                                            currentLatLng,
                                            trackAreaPolygon.points,
                                            true
                                        )
                                    ) {


                                        val latLngsList = locationDataList.getRecentLatLngs()


                                        if (latLngsList.all {
                                                !PolyUtil.containsLocation(
                                                    it,
                                                    trackAreaPolygon.points,
                                                    true
                                                )
                                            }
                                        ) {

                                            mapOverlayInfoText(
                                                SimpleDateFormat(
                                                    "yyyy-MM-dd, HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(Date()).toString()
                                                        + " 조교 구역 이탈(10초 이상)"
                                            )

                                            val totalTrainingTime =
                                                (notATraining / 1000) + (lightTraining / 1000) + (moderateTraining / 1000) + (heavyTraining / 1000)


                                            if (totalTrainingTime >= 60) {

                                                val file = File(
                                                    applicationContext.filesDir,
                                                    "TrainingRecord"
                                                )
                                                val fileWriter = FileWriter(file, true)

                                                val todayDateFormat = SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    Locale.getDefault()
                                                )


                                                val tempUserName =
                                                    sharedPref.getString("User_Name", "미확인")

                                                val startTimeFormat = SimpleDateFormat(
                                                    "HH:mm:ss",
                                                    Locale.getDefault()
                                                ).format(startTime as Date).toString()

                                                fileWriter.append(
                                                    "${
                                                        todayDateFormat.format(
                                                            startTime as Date
                                                        )
                                                    }, $startTimeFormat, 조교, $tempUserName, $tempHorseName, ${notATraining / 1000}, ${lightTraining / 1000}, ${moderateTraining / 1000}, ${heavyTraining / 1000}, ${notATraining / 1000 + lightTraining / 1000 + moderateTraining / 1000 + heavyTraining / 1000}\n"
                                                )



                                                fileWriter.flush()

                                                fileWriter.close()

                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 조교 기록 저장 완료" +
                                                            String.format(
                                                                "(%s초)",
                                                                totalTrainingTime.toString()
                                                            )
                                                )


                                            } else {

                                                mapOverlayInfoText(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd, HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()).toString()
                                                            + " 조교 기록 미저장(60초 미만)"
                                                )


                                            }





                                            myLocationState = 0

                                            startTime = null

                                            notATraining = 0L
                                            lightTraining = 0L
                                            moderateTraining = 0L
                                            heavyTraining = 0L



                                            bottomLayout.removeView(notATrainingTextLabel)
                                            bottomLayout.removeView(lightTrainingTextLabel)
                                            bottomLayout.removeView(moderateTrainingTextLabel)
                                            bottomLayout.removeView(heavyTrainingTextLabel)

                                            bottomLayout.addView(bottomTextLabel)


                                            runOnUiThread {
                                                bottomTextLabel.text =
                                                    getString(R.string.bottomText)
                                                bottomTextLabel.setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        applicationContext,
                                                        R.color.grey
                                                    )
                                                )

                                            }


                                        }


                                    }
                                }


                            }
                        }

                    }


                    locationCallback = MyLocationCallback()

                    // Request location updates
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )


                }
            }
        }
    }


    override fun onBackPressed() {


        if (myLocationState == 1) {


            if ((notATraining / 1000) + (lightTraining / 1000) + (moderateTraining / 1000) + (heavyTraining / 1000) <= 60) {

                runOnUiThread {
                    val destroyAlertDialog = AlertDialog.Builder(this@MapsActivity)
                        .setMessage(getString(R.string.noRecordAlert))
                        .setCancelable(true)
                        .setPositiveButton("OK") { _, _ ->
                            finish()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                    destroyAlertDialog.show()
                }


            } else {

                runOnUiThread {
                    val destroyAlertDialog = AlertDialog.Builder(this@MapsActivity)
                        .setMessage(getString(R.string.backPressedAutoSave))
                        .setCancelable(true)
                        .setPositiveButton("OK") { _, _ ->


                            val file = File(
                                applicationContext.filesDir,
                                "TrainingRecord"
                            )
                            val fileWriter = FileWriter(file, true)

                            val todayDateFormat = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            )


                            val tempUserName =
                                sharedPref.getString("User_Name", "미확인")

                            val tempHorseName = intent.getStringExtra("Horse_Name")


                            val startTimeFormat = SimpleDateFormat(
                                "HH:mm:ss",
                                Locale.getDefault()
                            ).format(startTime as Date).toString()

                            fileWriter.append(
                                "${
                                    todayDateFormat.format(
                                        startTime as Date
                                    )
                                }, $startTimeFormat, 조교, $tempUserName, $tempHorseName, ${notATraining / 1000}, ${lightTraining / 1000}, ${moderateTraining / 1000}, ${heavyTraining / 1000}, ${notATraining / 1000 + lightTraining / 1000 + moderateTraining / 1000 + heavyTraining / 1000}\n"
                            )

                            fileWriter.flush()
                            fileWriter.close()


                            finish()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                    destroyAlertDialog.show()
                }
            }


        } else {

            finish()

        }


    }


    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timer1sec.cancel()
        timer10sec.cancel()

        unbindService(serviceConnection)



        val currentCameraPosition = map.cameraPosition

        sharedPref.edit()
            .putFloat("User_Latitude", currentCameraPosition.target.latitude.toFloat())
            .putFloat("User_Longitude", currentCameraPosition.target.longitude.toFloat())
            .putFloat("User_ZoomLevel", currentCameraPosition.zoom)
            .putFloat("User_Rotation", currentCameraPosition.bearing)
            .apply()


        super.onDestroy()

    }

}












//
//
//
//
//class MyService : Service() {
//
//    private val itemList = mutableListOf<String>()
//
//    fun addItem(item: String) {
//        itemList.add(item)
//    }
//
//    fun getItemList(): List<String> {
//        return itemList.toList()
//    }
//
//    // ...
//}



//
//
//
//
//
//
//class MyActivity : AppCompatActivity() {
//
//    private var myService: MyService? = null
//
//    private val connection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            val binder = service as MyService.MyBinder
//            myService = binder.getService()
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            myService = null
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_my)
//
//        // Service에 bind
//        val intent = Intent(this, MyService::class.java)
//        bindService(intent, connection, Context.BIND_AUTO_CREATE)
//
//        // ...
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//
//        // Service와 unbind
//        unbindService(connection)
//    }
//
//    private fun addItemToList(item: String) {
//        myService?.addItem(item)
//    }
//
//    // ...
//}
//
//
//
//
//


*/





















