package com.kratis1698

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TrainerDivActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trainer_div)

        val seoulRacButton = findViewById<Button>(R.id.seoul_race)
        val busanRaceButton = findViewById<Button>(R.id.busan_race)
        val jejuFarmButton = findViewById<Button>(R.id.jeju_farm)
        val jangsuFarmButton = findViewById<Button>(R.id.jangsu_farm)
        val unregisteredButton = findViewById<Button>(R.id.unregistered)

        val sharedPref = getSharedPreferences(
            "Shared_Preferences", Context.MODE_PRIVATE)



        // 언덕주로용 어플이므로 일단 서울 부산을 표시하지 않음
        seoulRacButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue)
        busanRaceButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green)
        seoulRacButton.visibility = View.GONE
        busanRaceButton.visibility = View.GONE


        jejuFarmButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
        jangsuFarmButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple)
        unregisteredButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)



//        seoulRacButton.setOnClickListener {
//            val intent = Intent(this, TrainerSelectionActivity::class.java)
//            intent.putExtra("User_Div", "서울 경마장")
//            startActivity(intent)
//        }
//
//
//        busanRaceButton.setOnClickListener {
//            val intent = Intent(this, TrainerSelectionActivity::class.java)
//            intent.putExtra("User_Div", "부경 경마장")
//            startActivity(intent)
//        }

        jejuFarmButton.setOnClickListener {
            val intent = Intent(this, TrainerSelectionActivity::class.java)
            intent.putExtra("User_Div", "제주 목장")
            startActivity(intent)
        }

        jangsuFarmButton.setOnClickListener {
            val intent = Intent(this, TrainerSelectionActivity::class.java)
            intent.putExtra("User_Div", "장수 목장")
            startActivity(intent)
        }


        unregisteredButton.setOnClickListener {
            val editor = sharedPref.edit()
            editor.remove("User_PK")
            editor.remove("User_Div")
            editor.remove("User_Num")
            editor.remove("User_Name")
            editor.apply()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


    }




    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }



}