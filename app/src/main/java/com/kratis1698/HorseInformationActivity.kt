package com.kratis1698

//import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup



class HorseInformationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horse_information)


        val scope = CoroutineScope(Dispatchers.Main)
        val horseText = findViewById<TextView>(R.id.horse_text)

        // 코루틴을 실행합니다.
        scope.launch {
            // 네트워크 작업을 수행합니다.
//            val doc = Jsoup.connect("https://studbook.kra.co.kr/html/info/ind/s_majuck.jsp?mabun=0050925").get()
            val doc = withContext(Dispatchers.IO) {
//                Jsoup.connect("https://studbook.kra.co.kr/html/info/ind/s_majuck.jsp?mabun=0050925").ignoreHttpErrors(true).get()
                Jsoup.connect("https://www.example.com/").get()

            }

//            val td = doc.select("td[headers=ma04]")
//            val text12 = td.select("span").text()
            val text12 = doc.select("body div p").text()

            horseText.text = text12.toString()


        }
    }
}
