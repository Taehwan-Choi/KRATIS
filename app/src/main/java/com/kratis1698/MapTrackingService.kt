package com.kratis1698


/*
import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import java.util.*


class MapTrackingService : Service() {

    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    //아래 변수는 액티비티에서 직접 참조하도록 함(기본 접근 제한자 public)
    var myLocationState: Int = 0
    var locationServiceDataList = LocationDataList()

    lateinit var currentlocationInfo: LocationInfo

    var notATraining = 0L
    var lightTraining = 0L
    var moderateTraining = 0L
    var heavyTraining = 0L

    var startTime: Date? = null
    var uphillStartTime: Date? = null
    var uphillFinishTime: Date? = null

    val polylineLatLngList = mutableListOf<LatLng>()





    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1

    }





    data class LocationInfo(
        val time: Long,
        val latitude: Double,
        val longitude: Double,
        val altitude: Int,
        val accuracy: Int,
        val speed: Int,
    )

    data class LocationData(
        val time: Long,
        val latlng: LatLng,
        val speed: Int
    )

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



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        return START_STICKY
    }



    private fun requestLocationPermission() {
        val intent = Intent(this, MapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_LOCATION_PERMISSION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val request = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        try {
            pendingIntent.send(this, 0, Intent().putExtra("permissions", request))
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
        }
    }


    inner class LocationBinder : Binder() {
        fun getService(): MapTrackingService {
            return this@MapTrackingService
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }



    fun getLatLngList(): MutableList<LatLng> {
        return polylineLatLngList
    }




    override fun onCreate() {

        super.onCreate()







        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                val location = locationResult.lastLocation



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

                polylineLatLngList.add(currentLatLng)


                locationServiceDataList.add(
                    LocationData(
                        currentlocationInfo.time,
                        currentLatLng,
                        currentlocationInfo.speed
                    )
                )









            }
        }


    }


    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 위치 권한이 있을 때, 위치 정보 요청
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            // 위치 권한이 없을 때, 권한 요청 다이얼로그 표시
            requestLocationPermission()
        }

    }




    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }


}


*/