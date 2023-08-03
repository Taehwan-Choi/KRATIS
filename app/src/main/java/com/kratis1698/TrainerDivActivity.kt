package com.kratis1698

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TrainerDivActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trainer_div)

        val jejuFarmButton = findViewById<Button>(R.id.jeju_farm)
        val jangsuFarmButton = findViewById<Button>(R.id.jangsu_farm)
        val unregisteredButton = findViewById<Button>(R.id.unregistered)

        val sharedPref = getSharedPreferences(
            "Shared_Preferences", Context.MODE_PRIVATE)



        jejuFarmButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
        jangsuFarmButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple)
        unregisteredButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)




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