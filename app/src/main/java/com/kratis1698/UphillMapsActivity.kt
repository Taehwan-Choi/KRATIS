package com.kratis1698


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class UphillMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var map: GoogleMap
    private lateinit var polyline: Polyline

    private lateinit var timer1sec: Timer
    private lateinit var sharedPref: SharedPreferences
    private var focusValue = false
    private lateinit var mapOverlayScrollLayout: LinearLayout

    private var lastModifiedTime: Long = 0

    private lateinit var serviceConnection: ServiceConnection
    private lateinit var serviceIntent: Intent

    private var serviceInstance: MapTrackingService? = null

    private var tempHorsePK: String? = null
    private var tempHorseName: String? = null
    private var tempUserDiv: String? = null











    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uphill_maps)

        // 화면이 꺼지지 않고 계속 켜져있도록 하는 코드
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sharedPref = getSharedPreferences(
            "Shared_Preferences", Context.MODE_PRIVATE
        )


        focusValue = sharedPref.getBoolean("Tracking_Focus", false)

        tempHorsePK = sharedPref.getString("Horse_PK", "")
        tempHorseName = sharedPref.getString("Horse_Name", "")
        tempUserDiv = sharedPref.getString("User_Div", "")




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

    


    private fun readLogsAndUpdateScreen() {
        val file = File(applicationContext.filesDir, "TrainingRecord")

        if (file.lastModified() == lastModifiedTime) {
            // 파일이 변경되지 않았으므로 업데이트를 수행할 필요가 없음
            return
        }

        lastModifiedTime = file.lastModified()

        val logs: MutableList<String> = mutableListOf()

        try {
            val reader = BufferedReader(FileReader(file))
            var line: String? = reader.readLine()

            while (line != null) {
                logs.add(line)
                line = reader.readLine()
            }

            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        runOnUiThread {
            mapOverlayScrollLayout.removeAllViews()

            for (log in logs.takeLast(5)) {
                val textView = TextView(this)
                textView.text = log
                textView.textSize = 14f
                textView.setTextColor(Color.GRAY)
                mapOverlayScrollLayout.addView(textView)
            }
        }
    }





    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
//        map.mapType = GoogleMap.MAP_TYPE_SATELLITE


        map.setMaxZoomPreference(17.0f)
        map.setMinZoomPreference(12.0f)



        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
//        map.setMinZoomPreference(15.0f)

        val horseNameTextView = findViewById<TextView>(R.id.HorseNameLabel)

        val mapOverlayTextView = findViewById<TextView>(R.id.MapOverlayTextView)
        mapOverlayScrollLayout = findViewById(R.id.MapOverlayScrollLayout)
        mapOverlayScrollLayout.isHorizontalScrollBarEnabled = true



        polyline = map.addPolyline(PolylineOptions().apply {
            color(ContextCompat.getColor(applicationContext, R.color.orange))
            width(10f)
            geodesic(true)
        }
        )





        if (tempUserDiv == "제주 목장") {
            drawRectangleOnMap(LatLng(33.4153146, 126.6698925),LatLng(33.4170673, 126.6713069))
            drawRectangleOnMap(LatLng(33.4083759, 126.6689949), LatLng(33.4099342, 126.670175))

            drawLineOnMap(LatLng(33.4153146, 126.6698925),LatLng(33.4153146, 126.6713069))
            drawLineOnMap(LatLng(33.4099342, 126.6689949), LatLng(33.4099342, 126.670175))


        } else if (tempUserDiv == "장수 목장"){
            drawRectangleOnMap(LatLng(35.7227781,127.6463655),LatLng(35.7240254,127.6484111))
            drawRectangleOnMap(LatLng(35.7196201,127.6509355), LatLng(35.7207465,127.6521163))

            drawLineOnMap(LatLng(35.7227781,127.6484111),LatLng(35.7240254,127.6484111))
            drawLineOnMap(LatLng(35.7207465,127.6509355), LatLng(35.7207465,127.6521163))
        }







// 카메라 시점을 이전에 저장한 값으로 세팅하는 블록
        val cameraPosition = CameraPosition.Builder()
            .target(
                LatLng(
                    sharedPref.getFloat("User_Latitude", 0f).toDouble(),
                    sharedPref.getFloat("User_Longitude", 0f).toDouble()
                )
            )
            .zoom(sharedPref.getFloat("User_ZoomLevel", 17f))
            .bearing(sharedPref.getFloat("User_Rotation", 0f))
            .build()
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
        map.moveCamera(cameraUpdate)






//위치권한을 확인하고, 서비스를 시작하는 블록
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 위치 권한이 허용된 상태
            map.isMyLocationEnabled = true

            serviceIntent = Intent(this, MapTrackingService::class.java)




            // 포어그라운드 서비스 시작을 위해서는 명시적으로 호출해줘야함, bind에서 자동 생성 명령으로는 포어그라운드 형태로 실행되지 않음
            // 이렇게 해야만 서비스에서 onStartCommand 부분이 실행됨
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)

            } else {
                ContextCompat.startForegroundService(this, serviceIntent)
            }


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
        

        runOnUiThread {
            horseNameTextView.text = String.format("%s", tempHorseName)
        }





// 매초 마다 작동하는 기능 설정
        timer1sec = Timer()

        val timerTask1Sec = object : TimerTask() {
            override fun run() {

                readLogsAndUpdateScreen()

                val latLngList = serviceInstance?.latLngList
                val temp = serviceInstance?.currentlocationInfo


                runOnUiThread {
//                    실제 서비스 단계에서는 경로 그리는 기능 삭제// 최근 10개 위치만 그리는 기능
                    polyline.points = latLngList!!.takeLast(10)



                    if (focusValue && latLngList!!.isNotEmpty()) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLngList.last()), 200, null)
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



    private fun drawRectangleOnMap(bottomLeft: LatLng, topRight: LatLng) {
        val rectPoints = listOf(
            LatLng(bottomLeft.latitude, bottomLeft.longitude),
            LatLng(topRight.latitude, bottomLeft.longitude),
            LatLng(topRight.latitude, topRight.longitude),
            LatLng(bottomLeft.latitude, topRight.longitude)
        )

        val rectangle = PolygonOptions()
            .addAll(rectPoints)
            .strokeColor(Color.BLUE)
            .fillColor(Color.argb(50, 0, 0, 255))
            .strokeWidth(2f)

        map.addPolygon(rectangle)
    }


    private fun drawLineOnMap(startPoint: LatLng, endPoint: LatLng) {
        val polylineOptions = PolylineOptions().apply {
            add(startPoint)
            add(endPoint)
            color(Color.RED) // 색상 설정
            width(7f) // 두께 설정
        }

        map.addPolyline(polylineOptions)
    }

    private var savedCameraPosition: CameraPosition? = null
    private fun takeScreenshot() {
        savedCameraPosition = map.cameraPosition

        val desiredPosition = CameraPosition.Builder()
            .target(LatLng(33.41304652108367, 126.67181611061095))
            .zoom(15.129726f)
            .build()
        map.moveCamera(CameraUpdateFactory.newCameraPosition(desiredPosition))


        val rootView = findViewById<ViewGroup>(R.id.UphillScreenView)
        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundDrawable = rootView.background
        backgroundDrawable?.draw(canvas)
        rootView.draw(canvas)

        val topLayout = findViewById<LinearLayout>(R.id.topLayout)
        val topLayoutHeight = topLayout.height

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            googleMap.snapshot { mapBitmap ->
                // Combine the bitmaps
                val combinedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val combinedCanvas = Canvas(combinedBitmap)
                combinedCanvas.drawBitmap(mapBitmap!!, 0f, topLayoutHeight.toFloat(), null)
                combinedCanvas.drawBitmap(bitmap, 0f, 0f, null)

                // Restore the saved camera position
                savedCameraPosition?.let { cameraPosition ->
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }

                // Save the combined bitmap
                val displayName = "screenshot_${System.currentTimeMillis()}.jpg"
                val contentResolver = applicationContext.contentResolver
                val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val imageDetails = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/training")
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


    override fun onBackPressed() {
        runOnUiThread {
            val onBackPressedAlertDialog = AlertDialog.Builder(this@UphillMapsActivity)
                .setMessage(getString(R.string.onBackPressedAlertdialog))
                .setCancelable(true)
                .setPositiveButton("OK") { _, _ ->

                    if (serviceInstance?.statusTrained == true) {
                        takeScreenshot()
                    }

//                    takeScreenshot()

                    finish()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

            onBackPressedAlertDialog.show()
        }
    }






    override fun onPause() {
        super.onPause()
        Log.d("MY_LOG", "UphillMapsActivity onPause")
    }


    override fun onResume() {
        super.onResume()
        Log.d("MY_LOG", "UphillMapsActivity onResume")
    }


// 액티비티가 종료될 때 호출되는 블록
    override fun onDestroy() {

        Log.d("MY_LOG", "UphillMapsActivity onDestroy")
        timer1sec.cancel()

        val currentCameraPosition = map.cameraPosition
        sharedPref.edit()
            .putFloat("User_Latitude", currentCameraPosition.target.latitude.toFloat())
            .putFloat("User_Longitude", currentCameraPosition.target.longitude.toFloat())
            .putFloat("User_ZoomLevel", currentCameraPosition.zoom)
            .putFloat("User_Rotation", currentCameraPosition.bearing)
            .apply()


    //서비스와의 바인딩 해제 및 서비스 종료
        unbindService(serviceConnection)
        stopService(serviceIntent)

        super.onDestroy()
    }
}