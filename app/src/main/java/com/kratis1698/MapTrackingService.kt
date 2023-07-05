package com.kratis1698


import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class MapTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 50981698
        private const val CHANNEL_ID = "TrackingServiceChannel"
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

    private var horseName: String? = null
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
                    locationInfoFullList.add(currentlocationInfo)
                    latLngList.add(LatLng(location.latitude, location.longitude))


                    Log.d("MY_LOG", "${latLngList.lastOrNull()}")



                    // 시점부에 진입시 startTime을 현재시간으로 지속 동기화 함
                    if (isInStartArea(location.latitude, location.longitude)){
                        if (startTime == null) {
                            startTime = Date()
//                            시점부 진입시에는 startTime을 계속 갱신하게 되며, 하행하여 퇴장할 때도 로그가 남는 단점이 있어서
//                            실제 출시 시에는 로그를 남기지 않는 것이 좋을 것으로 판단
                            makingLog("실내언덕주로 시점부 진입")

                        }else{
                            startTime = Date()
                        }

                    }



                    if (startTime != null) {
                        if (isInFinishArea(location.latitude, location.longitude)){
                            
                            //시점부 출발 기록이 있는 상태로, 종점부 도달시 해당 기록 저장
                            makingLog("주파기록 ${((Date().time - startTime!!.time) / 1000).toInt()}초")
                            startTime = null

                        }

// 출발한지 300초(5분)이 경과해도 종점부에 도달하지 못 한 경우(훈련을 종료한 경우) startTime 강제 초기화
                        if (Date().time - startTime!!.time > 300 * 1000) {
                            startTime = null
                        }


                    }
                }
            }
        }
    }



    private fun makingLog(text: String) {
        val currentTime = SimpleDateFormat("M월 d일, HH:mm:ss", Locale.getDefault()).format(Date())

        val file = File(applicationContext.filesDir, "TrainingRecord")

        val fileWriter = FileWriter(file, true)
        fileWriter.append("$currentTime, $text\n")
        fileWriter.flush()
        fileWriter.close()
    }




    private fun isInStartArea(latitude: Double, longitude: Double): Boolean {
        val bottomLeft = LatLng(33.4160626, 126.6701271) // 좌측 하단 지점 좌표
        val topRight = LatLng(33.4167705, 126.6711056) // 우측 상단 지점 좌표

        val targetBounds = LatLngBounds.Builder()
            .include(bottomLeft)
            .include(topRight)
            .build()

        val targetLatLng = LatLng(latitude, longitude)
        return targetBounds.contains(targetLatLng)
    }


    private fun isInFinishArea(latitude: Double, longitude: Double): Boolean {
        val bottomLeft = LatLng(33.408618, 126.6689086) // 좌측 하단 지점 좌표
        val topRight = LatLng(33.409366, 126.6700555) // 우측 상단 지점 좌표

        val targetBounds = LatLngBounds.Builder()
            .include(bottomLeft)
            .include(topRight)
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

        //알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TrackingServiceChannel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        horseName = intent?.getStringExtra("Horse_Name")
        makingLog("$horseName 트래킹 시작")

        startForeground(NOTIFICATION_ID, createNotification())
//        Log.d("MY_LOG", "convert to foreground")
        requestLocationUpdates()

        return START_STICKY
    }



    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, UphillMapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Service")
            .setContentText("Tracking service is on foreground")
            .setSmallIcon(R.drawable.ic_menu_slideshow)
            .setContentIntent(pendingIntent)
            .build()
    }





    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
        Log.d("MY_LOG", "Tracking Service onDestroy")
        makingLog("$horseName 트래킹 종료")
    }


}