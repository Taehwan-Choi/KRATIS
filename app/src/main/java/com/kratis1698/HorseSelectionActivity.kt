package com.kratis1698

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class HorseSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horse_selection)

        val sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE
        )

        val tempUserPk = sharedPref.getString("User_PK", null)
        val buttonLayout = findViewById<LinearLayout>(R.id.button_layout)
        val inputStream = File(getExternalFilesDir(null), "horse.csv")

        val trackingType = intent.getStringExtra("Tracking_Type")


        data class Horse(val first: String, val second: String)

        val horses = mutableListOf<Horse>()

        inputStream.reader(Charsets.UTF_8).buffered().useLines { lines ->
            lines.drop(1).forEach {
                val row = it.split(",")
                if (row[0] == tempUserPk) {
                    val horse = Horse(row[1], row[2])
                    horses.add(horse)
                }
            }
        }


//        val horseListUpdateTimeText = findViewById<TextView>(R.id.horseListUpdateTime)
//
//
//        runOnUiThread {
//            horseListUpdateTimeText.text = SimpleDateFormat("리스트 업데이트 : yyyy-MM-dd / HH:mm:ss", Locale.getDefault()).format(
//                Date()
//            )
//        }







        for (horse in horses) {
            val button = Button(this)

            button.text = String.format("%s(%s)", horse.second, horse.first)


            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)



            button.setOnClickListener {

                when (trackingType) {
                    "Training" -> {
                        val intent = Intent(this, MapsActivity::class.java)

                        val sharedPref = getSharedPreferences("KRAIS_Preferences", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("Horse_PK", horse.first)
                        editor.putString("Horse_Name", horse.second)
                        editor.apply()

                        startActivity(intent)
                    }
                    "Uphill_Training" -> {
                        val intent = Intent(this, UphillMapsActivity::class.java)

                        val sharedPref = getSharedPreferences("KRAIS_Preferences", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("Horse_PK", horse.first)
                        editor.putString("Horse_Name", horse.second)
                        editor.apply()

                        startActivity(intent)
                    }
                }


            }
            buttonLayout.addView(button)
        }


    }


}