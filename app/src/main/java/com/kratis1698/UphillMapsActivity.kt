package com.kratis1698


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class UphillMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val NOTIFICATION_ID = 1698
        private const val CHANNEL_ID = "TrackingServiceChannel"
    }

    private lateinit var map: GoogleMap
    private lateinit var polyline: Polyline

    private lateinit var timer1sec: Timer

    private lateinit var sharedPref: SharedPreferences

    private var focusValue = false

    private lateinit var bottomLayout: LinearLayout
    private lateinit var bottomTextLabel: TextView

    private lateinit var mapOverlayScrollLayout: LinearLayout

    lateinit var currentLocationInfo : MapTrackingService.LocationInfo
    var locationInfoFullList_binding : MutableList<MapTrackingService.LocationInfo> = mutableListOf<MapTrackingService.LocationInfo>()
    var latLngList_binding = mutableListOf<LatLng>()


    private var serviceInstance: MapTrackingService? = null
    private lateinit var serviceConnection: ServiceConnection



    private fun formatTime(timeInMillis: Long): String {
        val seconds = timeInMillis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }




//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_LOCATION_PERMISSION) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                onMapReady(map)
//            }
//        }
//    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uphill_maps)

        // 화면이 꺼지지 않고 계속 켜져있도록 하는 코드
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE
        )
        focusValue = sharedPref.getBoolean("Tracking_Focus", false)

        val toggleButton = findViewById<ToggleButton>(R.id.button_toggle)
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

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        map.setMinZoomPreference(15.0f)

        val horseNameTextView = findViewById<TextView>(R.id.HorseNameLabel)

        bottomLayout = findViewById(R.id.bottomLayout)
        bottomTextLabel = findViewById(R.id.bottomTextLabel)
        
        val mapOverlayTextView = findViewById<TextView>(R.id.MapOverlayTextView)
        mapOverlayScrollLayout = findViewById(R.id.MapOverlayScrollLayout)
        mapOverlayScrollLayout.isHorizontalScrollBarEnabled = true

        val tempHorsePK = intent.getStringExtra("Horse_PK")
        val tempHorseName = intent.getStringExtra("Horse_Name")
        val tempHorseBY = intent.getStringExtra("Horse_BY")
        val tempUserDiv = sharedPref.getString("User_Div", "")

        polyline = map.addPolyline(PolylineOptions().apply {
            color(ContextCompat.getColor(applicationContext, R.color.grey))
            width(10f)
            geodesic(true)
        }
        )





// 카메라 시점을 이전에 저장한 값으로 세팅하는 블록
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






//위치권한을 확인하고, 서비스를 시작하는 블록
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 위치 권한이 허용된 상태
            map.isMyLocationEnabled = true

            val serviceIntent = Intent(this, MapTrackingService::class.java)

            // 포어그라운드 서비스 시작을 위해서는 명시적으로 호출해줘야함, bind에서 자동 생성 명령으로는 포어그라운드 형태로 실행되지 않음
            // 이렇게 해야만 서비스에서 onStartCommand 부분이 실행됨
            startService(serviceIntent)

            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as MapTrackingService.MapTrackingBinder
                    serviceInstance = binder.getService()
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    serviceInstance = null
                }
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)



        } else {
            // 위치 권한이 거부된 상태이므로 권한 요청 대화상자를 표시
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
            )
        }
        




//기본 상단 마번 마명을 표출하는 기능
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





// 매초 마다 작동하는 기능 설정
        timer1sec = Timer()

        val timerTask1Sec = object : TimerTask() {
            override fun run() {

                val latLngList = serviceInstance?.latLngList
                val temp = serviceInstance?.currentlocationInfo


                runOnUiThread {
                    polyline.points = serviceInstance?.latLngList

                    if (focusValue && latLngList!!.isNotEmpty()) {
                        map.moveCamera(CameraUpdateFactory.newLatLng(latLngList.last()))
                    }

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
                        ).format(Date(temp!!.time)),
                        temp!!.latitude,
                        temp!!.longitude,
                        temp!!.altitude,
                        temp!!.accuracy,
                        temp!!.speed,
                        temp!!.bearing
                    )
                }
            }
        }
        timer1sec.schedule(timerTask1Sec, 5000L, 1000L)
    }


    override fun onPause() {
        super.onPause()
        Log.d("MY_LOG", "액티비티 중지")

//        val serviceIntent = Intent(this, MapTrackingService::class.java)
//        serviceIntent.action = MapTrackingService.ACTION_FOREGROUND_START
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            ContextCompat.startForegroundService(this, serviceIntent)
//        } else {
//            startService(serviceIntent)
//        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("MY_LOG", "액티비티 재개")

//        val serviceIntent = Intent(this, MapTrackingService::class.java)
//        serviceIntent.action = MapTrackingService.ACTION_FOREGROUND_STOP
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(serviceIntent)
//        } else {
//            startService(serviceIntent)
//        }
    }





// 액티비티가 종료될 때 호출되는 블록
    override fun onDestroy() {
        timer1sec.cancel()

        val currentCameraPosition = map.cameraPosition
        sharedPref.edit()
            .putFloat("User_Latitude", currentCameraPosition.target.latitude.toFloat())
            .putFloat("User_Longitude", currentCameraPosition.target.longitude.toFloat())
            .putFloat("User_ZoomLevel", currentCameraPosition.zoom)
            .putFloat("User_Rotation", currentCameraPosition.bearing)
            .apply()

    //서비스와의 바인딩 해제
        unbindService(serviceConnection)

        val serviceIntent = Intent(this, MapTrackingService::class.java)
        stopService(serviceIntent)

        super.onDestroy()
    }
}