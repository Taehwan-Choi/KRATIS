package com.kratis1698

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class HorseCheckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horse_selection)

        val sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE
        )

        val tempUserPk = sharedPref.getString("User_PK", null)

        val buttonLayout = findViewById<LinearLayout>(R.id.button_layout)

        val inputStream = resources.openRawResource(R.raw.kra_horses)

        val horses = mutableListOf<Triple<String, String, String>>()


        inputStream.reader(Charsets.UTF_8).buffered().useLines { lines ->
            lines.drop(1).forEach {
                val row = it.split(",")
                if (row[0] == tempUserPk) {
                    horses.add(Triple(row[1], row[2], row[3]))
                    //                    0은 trainer PK, 1은 Horse PK, 2는 Horse Name, 3은 Horse BY 순서
                }
            }
        }



        for (horse in horses) {
            val button = Button(this)

            if (horse.second.endsWith("자마")) {
                button.text = String.format(
                    "%s / %s ('%s)",
                    horse.first,
                    horse.second,
                    horse.third.takeLast(2)
                )
            } else {
                button.text = String.format("%s / %s", horse.first, horse.second)
            }


            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)


            button.setOnClickListener {

                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://studbook.kra.co.kr/html/info/ind/s_majuck.jsp?mabun=" + "${addLeadingZeros(horse.first)}"))
                startActivity(intent)

            }




            buttonLayout.addView(button)


        }
    }


    private fun addLeadingZeros(str: String) = String.format("%07d", str.toInt())


}




