package com.kratis1698

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {


    companion object {
        private const val APPLICATION_VERSION = "1.0"
    }



//    전화 번호 가져오기 코드의 경우, 현제 getLine1Number 가 지원 종료되었고(API 33부터)
//    getNumber / getPhoneNumber 가 사용되므로 구현하기가 까다로운 상황


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)






        val trackingButton = findViewById<Button>(R.id.tracking_button)
        val uphillTrackingButton = findViewById<Button>(R.id.uphill_tracking_button)
        val recordButton = findViewById<Button>(R.id.record_button)
        val horseCheckButton = findViewById<Button>(R.id.horse_check_button)
        val userButton = findViewById<Button>(R.id.user_button)
        val helpButton = findViewById<Button>(R.id.help_button)
        val exitButton = findViewById<Button>(R.id.exit_button)




        val topLayOutText = findViewById<TextView>(R.id.Top_layout_Text)

        val versionUpdateInfoText = findViewById<TextView>(R.id.VersionUpdateInfoText)


        val sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE)

        val topLayoutTextString = sharedPref.getString("User_Div", "") + " " + sharedPref.getString("User_Num", "미등록 사용자")+ " " + sharedPref.getString("User_Name", "")
        topLayOutText.text = topLayoutTextString


        runOnUiThread {
            versionUpdateInfoText.text = "Version : " + APPLICATION_VERSION + "  /  " + SimpleDateFormat("입사마 업데이트 : MM-dd HH:mm", Locale.getDefault()).format(
                Date()
            )
        }

        val userDiv = sharedPref.getString("User_Div", null)


        if (userDiv == null) {
            trackingButton.visibility = View.GONE
            recordButton.visibility = View.GONE
            horseCheckButton.visibility = View.GONE
        } else {
            trackingButton.visibility = View.VISIBLE
            recordButton.visibility = View.VISIBLE
            horseCheckButton.visibility = View.VISIBLE
        }



        if (userDiv == "제주 목장" || userDiv == "장수 목장") {
            uphillTrackingButton.visibility = View.VISIBLE
        } else {
            uphillTrackingButton.visibility = View.GONE
        }




        userDiv?.let { tempUserDiv ->
            when (tempUserDiv) {
                "서울 경마장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
                "부경 경마장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                "제주 목장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                "장수 목장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this, R.color.purple))
                else -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
            }
        }



        trackingButton.setOnClickListener {

            if (userDiv != null) {
                val intent = Intent(this, HorseSelectionActivity::class.java)
                intent.putExtra("Tracking_Type", "Training")
                startActivity(intent)
            }
        }



        uphillTrackingButton.setOnClickListener {

            if (userDiv != null) {
                val intent = Intent(this, HorseSelectionActivity::class.java)
                intent.putExtra("Tracking_Type", "Uphill_Training")
                startActivity(intent)
            }
        }




        recordButton.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://studbook.kra.co.kr/studbook.jsp"))
            startActivity(intent)
        }



        horseCheckButton.setOnClickListener {
            val intent = Intent(this, HorseCheckActivity::class.java)
            startActivity(intent)
        }



        userButton.setOnClickListener {
            val intent = Intent(this, TrainerDivActivity::class.java)
            startActivity(intent)
        }



        helpButton.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }


        exitButton.setOnClickListener {
            finish()
        }


    }





}