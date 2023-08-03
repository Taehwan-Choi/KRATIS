package com.kratis1698


import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.io.File
import java.io.FileWriter
import java.lang.Math.*
import java.text.SimpleDateFormat
import java.util.*


class MapTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 50981698
        private const val CHANNEL_ID = "KRATIS1698"
    }

    data class LocationInfo(
        val time: Long,  // 측정시간(ms)
        val latitude: Double,  // 위도
        val longitude: Double,  // 경도
        val altitude: Int,  // 고도(해발미터)
        val accuracy: Int,  // 오차범위(미터)
        val speed: Int,  // 속도(당초 초속이나 시속으로 변환하여 저장)
        val bearing: Int  // 방향(0 북쪽, 90 동쪽, 180 남쪽, 270 서쪽)
    )

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var tempHorsePK: String? = null
    private var tempHorseName: String? = null
    private var tempUserDiv: String? = null


    private lateinit var startBottomLeft : LatLng
    private lateinit var startTopRight : LatLng
    private lateinit var finishBottomLeft : LatLng
    private lateinit var finishTopRight : LatLng


    private var startTime: Date? = null

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 1000 // 위치 업데이트 간격 (밀리초)
        fastestInterval = 500 // 가장 빠른 위치 업데이트 간격 (밀리초)
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 위치 요청 우선순위
    }

    private lateinit var locationCallback: LocationCallback

    lateinit var currentlocationInfo: LocationInfo
    var locationInfoFullList  = mutableListOf<LocationInfo>()
    var latLngList = mutableListOf<LatLng>()

    private var binder: MapTrackingBinder? = null

    var statusTrained = false







    inner class MapTrackingBinder : Binder() {
        fun getService(): MapTrackingService = this@MapTrackingService
    }


    override fun onBind(intent: Intent?): IBinder? {
        binder = MapTrackingBinder()
        return binder
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("MY_LOG", "Tracking Service onCreate")

        //로그 파일 초기화
//        File(applicationContext.filesDir, "TrainingRecord").delete()


        val sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE
        )

        tempHorsePK = sharedPref.getString("Horse_PK", null)
        tempHorseName = sharedPref.getString("Horse_Name", null)
        tempUserDiv = sharedPref.getString("User_Div", null)



        if (tempUserDiv == "제주 목장"){

            startBottomLeft = LatLng(33.4153146, 126.6698925) // 좌측 하단 지점 좌표
            startTopRight = LatLng(33.4170673, 126.6713069) // 우측 상단 지점 좌표

            finishBottomLeft = LatLng(33.4083759, 126.6689949) // 좌측 하단 지점 좌표
            finishTopRight = LatLng(33.4099342, 126.670175) // 우측 상단 지점 좌표

        }else if (tempUserDiv == "장수 목장"){

            startBottomLeft = LatLng(35.7227781,127.6463655) // 좌측 하단 지점 좌표
            startTopRight = LatLng(35.7240254,127.6484111) // 우측 상단 지점 좌표

            finishBottomLeft = LatLng(35.7196201,127.6509355) // 좌측 하단 지점 좌표
            finishTopRight = LatLng(35.7207465,127.6521163) // 우측 상단 지점 좌표

        }




        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            createLocationCallback()
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {



                    currentlocationInfo = LocationInfo(
                        location.time,
                        location.latitude,
                        location.longitude,
                        location.altitude.toInt(),
                        location.accuracy.toInt(),
                        (location.speed * 3.6).toInt(),
                        location.bearing.toInt()
                    )


                    // 위치 정보가 가끔씩 극단적인 위치로 튀는 현상을 최소화하기 위해서 데이터 일부러 누락
                    //시속 100km 기준은 초속 28m 수준으로 1펄롱(200m)를 7초에 주파하는 수준
                    //그 이상의 속도는 알고리즘상의 오류 정보라고 가정하고 아예 누락시킴으로서 신뢰성 제고
                    //오차정보가 100미터 이상일 경우에도 위치정보를 고의적으로 누락
                    if (currentlocationInfo.speed <= 100  && currentlocationInfo.accuracy <= 100 ){

                        // 현재 위치와 선분 위의 가장 가까운 점 사이의 거리를 계산
//                        val currentLatLng = LatLng(location.latitude, location.longitude)
//                        val closestPointOnLine = projectPointOnLine(currentLatLng)
//                        val distance = euclideanDistanceInMeters(currentLatLng, closestPointOnLine)
//
//                        // 거리가 20m 이하인 경우만 선분위의 점으로 보정
//                        if (distance <= 20) {
//                            currentlocationInfo = LocationInfo(
//                                location.time,
//                                closestPointOnLine.latitude,
//                                closestPointOnLine.longitude,
//                                location.altitude.toInt(),
//                                location.accuracy.toInt(),
//                                (location.speed * 3.6).toInt(),
//                                location.bearing.toInt()
//                            )
//                        }




                        locationInfoFullList.add(currentlocationInfo)
//                        latLngList.add(LatLng(location.latitude, location.longitude))


                        latLngList.add(LatLng(currentlocationInfo.latitude, currentlocationInfo.longitude))



                        Log.d("MY_LOG", "${latLngList.lastOrNull()}")


                        // 시점부에 진입시 startTime을 현재시간으로 지속 동기화 함
                        if (isInStartArea(currentlocationInfo.latitude, currentlocationInfo.longitude)){
                            startTime = Date()
                        }



                        if (startTime != null) {
                            if (isInFinishArea(currentlocationInfo.latitude, currentlocationInfo.longitude)){

                                //시점부 출발 기록이 있는 상태로, 종점부 도달시 해당 기록 저장
                                makingLog("$tempHorseName ${((Date().time - startTime!!.time) / 1000).toInt()}초")

                                // 언덕주로 조교기록이 있는 상태라고 플래그(플래그가 있는 상태에서는 액티비티 종료시 자동 스크린샷 저장)
                                statusTrained = true

                                startTime = null

                            } else {
                                // 출발한지 300초(5분)이 경과해도 종점부에 도달하지 못 한 경우, 훈련을 종료한 것으로 간주하고 startTime 강제 초기화
                                if (Date().time - startTime!!.time > 300 * 1000) {
                                    startTime = null
                                }
                            }
                        }
                    }else{
//                        makingLog("위치 측정 부정확(오차 ${location.accuracy.toInt()} m)")
                    }

                }
            }
        }
    }



    private fun makingLog(text: String) {
        val currentTime = SimpleDateFormat("M월 d일, HH:mm:ss", Locale.getDefault()).format(Date())

        var csvFileName = "TrainingRecord.csv"
        val csvFile = File(getExternalFilesDir(null), csvFileName)
        csvFile.appendText("$currentTime, $text\n")
    }




    private fun isInStartArea(latitude: Double, longitude: Double): Boolean {

        val targetBounds = LatLngBounds.Builder()
            .include(startBottomLeft)
            .include(startTopRight)
            .build()

        val targetLatLng = LatLng(latitude, longitude)
        return targetBounds.contains(targetLatLng)
    }


    private fun isInFinishArea(latitude: Double, longitude: Double): Boolean {
        val targetBounds = LatLngBounds.Builder()
            .include(finishBottomLeft)
            .include(finishTopRight)
            .build()

        val targetLatLng = LatLng(latitude, longitude)
        return targetBounds.contains(targetLatLng)
    }


    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {



        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())


        requestLocationUpdates()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(NotificationChannel(
                CHANNEL_ID,
                "마사회 조교 정보",
                NotificationManager.IMPORTANCE_HIGH
            ))

            val createdChannel = notificationManager?.getNotificationChannel(CHANNEL_ID)


            if (createdChannel != null) {
                Log.d("MY_LOG", "Notification Channel created successfully")
            } else {
                Log.d("MY_LOG", "Notification Channel creation failed")
            }

        }
    }


    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, UphillMapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Service")
            .setContentText("Tracking service is on foreground")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)

        if (notification != null) {
            Log.d("MY_LOG", "Notification created successfully")
        } else {
            Log.d("MY_LOG", "Notification creation failed")
        }

        return notification
    }






    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
        Log.d("MY_LOG", "Tracking Service onDestroy")
    }











//
//    주어진 위치를 후 보정하는 알고리즘 테스트
//  가까운 선분으로 강제 보정하는 방식 / 이동평균 방식도 구현할 수 있으니 참고
//
//    private val p1 = LatLng(33.4163658,126.6706671)  // 선분의 시작점
//    private val p2 = LatLng(33.4089684,126.6694762)  // 선분의 끝점
//    private val segmentDiffLongitude = p2.longitude - p1.longitude
//    private val segmentDiffLatitude = p2.latitude - p1.latitude
//    private val rdenominator = segmentDiffLongitude * segmentDiffLongitude + segmentDiffLatitude * segmentDiffLatitude
//
//    fun projectPointOnLine(p: LatLng): LatLng {
//        val rnumerator = (p.longitude-p1.longitude) * segmentDiffLongitude + (p.latitude-p1.latitude) * segmentDiffLatitude
//        var r = rnumerator / rdenominator
//
//        if (r < 0) {
//            r = 0.0
//        } else if (r > 1) {
//            r = 1.0
//        }
//
//        val px = p1.longitude + r * segmentDiffLongitude
//        val py = p1.latitude + r * segmentDiffLatitude
//
//        return LatLng(py, px)
//    }
//
//    fun euclideanDistanceInMeters(p1: LatLng, p2: LatLng): Double {
//        val latDistance = abs(p1.latitude - p2.latitude) * 111000 // latitude 1 degree is approximately 111 kilometers (or 111000 meters)
//        val lngDistance = abs(p1.longitude - p2.longitude) * (111320 * cos(toRadians(p1.latitude))) // longitude 1 degree varies. Here it is approximated using cosine of latitude
//
//        return sqrt(latDistance * latDistance + lngDistance * lngDistance)
//    }





}