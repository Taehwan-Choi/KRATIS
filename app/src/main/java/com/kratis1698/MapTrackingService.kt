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

        Log.d("MY_LOG", "service started")

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
                }
            }
        }
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

        startForeground(NOTIFICATION_ID, createNotification())
        Log.d("MY_LOG", "convert to foreground")

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
        Log.d("MY_LOG", "service terminated")
    }


}