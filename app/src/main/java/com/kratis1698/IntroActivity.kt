package com.kratis1698

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File


data class MyItem(val pcndId: String, val rccrsCd: String, val beloNo: String, val pcndNm:String )
data class MyItem2(val barnGbCd: Int, val hrnm: String, val hrno: String, val pcndId: String)
data class MyItem3(val barnGbCd: Int, val hrnm: String, val hrno: String, val pcndId: String,val pcndId2: String, val rccrsCd: String, val beloNo: String, val pcndNm:String)
data class MyItem4(val barnGbCd: String, val hrnm: String, val hrno: String, val pcndId: String,val pcndId2: String, val rccrsCd: String, val beloNo: String, val pcndNm:String)

/*
* 2023 6.23
* 인트로 로직
* 기능 api를 받어 조교사와 조교마 정보를 업데이트후 mainactivity로 넘긴다
*
* 데이터 파일은 storage / emulated / 0 / android / data / com.kratis1698 / files / 폴더에 있음
* */
class IntroActivity :  AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiService2: ApiService2
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        showLoadingBar() // 로딩바를 보이도록 설정


        val retrofit = Retrofit.Builder()
            .baseUrl("http://kradata.kra.co.kr:8082/service/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
        apiService2 = retrofit.create(ApiService2::class.java)

        GlobalScope.launch(Dispatchers.Main) {
            if (isNetworkAvailable()) {
                try {
                    val itemList = fetchDataFromApi()
                    val itemList2 = fetchDataFromApi2()
                    saveDataToCsv(itemList)
                    saveDataToCsv2(itemList2)

                    val intent = Intent(this@IntroActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    errorDialog()
                }
            } else {
                showExitConfirmationDialog()
            }
        }

    }

    private suspend fun fetchDataFromApi(): List<MyItem> {
        val response = apiService.getOpenDataList(
            "FCD6A16A8A7DEBAE2A6D8BBCDBD42F5134B77D8E9A2D83307E4599B0BE61CB",
            1,
            1000000,
            "json"
        )

        val responseBody = response.string()
        val jsonElement = JsonParser.parseString(responseBody)
        val responseObj = jsonElement.asJsonObject["response"].asJsonObject
        val bodyObj = responseObj["body"].asJsonObject
        val itemsObj = bodyObj["items"].asJsonObject
        val itemArray = itemsObj["item"].asJsonArray

        val itemList = mutableListOf<MyItem>()
        val itemobj2 = MyItem("Trainer_PK","Trainer_Div","Trainer_Num","Trainer_Name")
        itemList.add(itemobj2)
        for (item in itemArray) {
            val beloNo = item.asJsonObject["beloNo"].let {
                if (it.isJsonPrimitive && it.asJsonPrimitive.isNumber) {
                    it.asInt.toString()
                } else {
                    "-"
                }
            }
            val pcndId = item.asJsonObject["pcndId"].asString
            val pcndNm = item.asJsonObject["pcndNm"].asString
            val rccrsCd = item.asJsonObject["rccrsCd"].asInt
            val rccrsNm = when (rccrsCd) {
                1 -> "서울 경마장"
                3 -> "부경 경마장"
                2 -> "제주 경마장"
                10 ->"장수 목장"
                11 ->"제주 목장"
                else -> "-"
            }

            val itemObj = MyItem( pcndId ,rccrsNm, beloNo,  pcndNm)
            itemList.add(itemObj)

        }

        return itemList
    }
    private suspend fun fetchDataFromApi2(): List<MyItem2> {
        val response = apiService2.getOpenDataList(
            "FCD6A16A8A7DEBAE2A6D8BBCDBD42F5134B77D8E9A2D83307E4599B0BE61CB",
            1,
            1000000,
            "json"
        )

        val responseBody = response.string()
        val jsonElement = JsonParser.parseString(responseBody)
        val responseObj = jsonElement.asJsonObject["response"].asJsonObject
        val bodyObj = responseObj["body"].asJsonObject
        val itemsObj = bodyObj["items"].asJsonObject
        val itemArray = itemsObj["item"].asJsonArray

        val itemList = mutableListOf<MyItem2>()
        for (item in itemArray) {
            val barnGbCd = item.asJsonObject["barnGbCd"].asInt
            val hrnm = item.asJsonObject["hrnm"].asString
            val hrno = item.asJsonObject["hrno"].asString
            val pcndId = item.asJsonObject["pcndId"]?.asString ?: "-"


            val itemObj = MyItem2(barnGbCd, hrnm, hrno,pcndId)
            itemList.add(itemObj)
        }

        return itemList
    }

    private fun saveDataToCsv(itemList: List<MyItem>) {
        val csvData = itemList.joinToString("\n") { item ->
            "${item.pcndId},${item.rccrsCd},${item.beloNo},${item.pcndNm}"
        }

        var csvFileName = "trainer.csv"

        val csvFile = File(getExternalFilesDir(null), csvFileName)
        csvFile.writeText(csvData)
    }

    private suspend fun saveDataToCsv2(itemList2: List<MyItem2>) {
        val itemList = fetchDataFromApi()

        val combinedList = mutableListOf<MyItem3>()

        //api horse 정보를 raw.csv랑 맞추기 위해 조교사 api에 join함
        for (item1 in itemList) {
            for (item2 in itemList2) {
                if (item1.pcndId == item2.pcndId) {
                    val combinedItem = MyItem3(
                        item2.barnGbCd,
                        item2.hrnm,
                        item2.hrno,
                        item2.pcndId,
                        item1.pcndId,
                        item1.rccrsCd,
                        item1.beloNo,
                        item1.pcndNm
                    )
                    combinedList.add(combinedItem)
                }

            }
        }

        val csvData = combinedList.joinToString("\n") { item ->
            "${item.pcndId},${item.hrno},${item.hrnm},${item.beloNo},${item.pcndNm},${item.rccrsCd}"
        }

        val csvFileName = "horse.csv"
        val csvFile = File(getExternalFilesDir(null), csvFileName)
        csvFile.writeText("Trainer_PK,Horse_PK,Horse_Name,Trainer_Num,Trainer_Name,Trainer_Div\n"+csvData)
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("네트워크 연결을 확인해 주세요")
        builder.setPositiveButton("종료") { _, _ ->
            finish()
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun errorDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("데이터 업데이트 중 에러가 발생하였습니다")
        builder.setPositiveButton("종료") { _, _ ->
            finish()
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
    }
    private fun showLoadingBar() {
        loadingProgressBar.visibility = View.VISIBLE
    }

}


