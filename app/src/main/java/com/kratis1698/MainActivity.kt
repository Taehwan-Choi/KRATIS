package com.kratis1698

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {


    // UI elements
    private lateinit var trackingButton: Button
    private lateinit var uphillTrackingButton: Button
    private lateinit var recordButton: Button
    private lateinit var horseCheckButton: Button
    private lateinit var userButton: Button
    private lateinit var helpButton: Button
    private lateinit var exitButton: Button
    private lateinit var topLayOutText: TextView
    private lateinit var versionUpdateInfoText: TextView

    // Other elements
    private lateinit var sharedPref: SharedPreferences
    private var userDiv: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()

        updateUI()

        setOnClickListeners()
    }


    private fun initUI() {
        trackingButton = findViewById(R.id.tracking_button)
        uphillTrackingButton = findViewById(R.id.uphill_tracking_button)
        recordButton = findViewById(R.id.record_button)
        horseCheckButton = findViewById(R.id.horse_check_button)
        userButton = findViewById(R.id.user_button)
        helpButton = findViewById(R.id.help_button)
        exitButton = findViewById(R.id.exit_button)
        topLayOutText = findViewById<TextView>(R.id.Top_layout_Text)
        versionUpdateInfoText = findViewById<TextView>(R.id.VersionUpdateInfoText)

        sharedPref = getSharedPreferences("Shared_Preferences", Context.MODE_PRIVATE)
    }

    private fun updateUI() {

        userDiv = sharedPref.getString("User_Div", null)

        val topLayoutTextString = sharedPref.getString("User_Div", "") + " " + sharedPref.getString(
            "User_Num",
            "미등록 사용자") + " " + sharedPref.getString("User_Name", "")
        topLayOutText.text = topLayoutTextString


        userDiv?.let { tempUserDiv ->
            when (tempUserDiv) {
                "서울 경마장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this,
                                                                                    R.color.blue))
                "부경 경마장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this,
                                                                                    R.color.green))
                "제주 목장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this,
                                                                                   R.color.red))
                "장수 목장" -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this,
                                                                                   R.color.purple))
                else -> topLayOutText.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
            }
        }


        ///임시로 미사용 버튼 모두 비활성화
        trackingButton.visibility = View.GONE
        horseCheckButton.visibility = View.GONE
        helpButton.visibility = View.GONE



        if (userDiv == null) {
            recordButton.visibility = View.GONE
        } else {
            recordButton.visibility = View.VISIBLE
        }



        if (userDiv == "제주 목장" || userDiv == "장수 목장") {
            uphillTrackingButton.visibility = View.VISIBLE
        } else {
            uphillTrackingButton.visibility = View.GONE
        }



    }

    private fun setOnClickListeners() {


//        trackingButton.setOnClickListener {
//
//            if (userDiv != null) {
//                val intent = Intent(this, HorseSelectionActivity::class.java)
//                intent.putExtra("Tracking_Type", "Training")
//                startActivity(intent)
//            }
//        }



        uphillTrackingButton.setOnClickListener {

            if (userDiv != null) {
                val intent = Intent(this, HorseSelectionActivity::class.java)
                intent.putExtra("Tracking_Type", "Uphill_Training")
                startActivity(intent)
            }
        }


        recordButton.setOnClickListener {

            if (userDiv == "제주 목장") {
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.horsepia.com/hp/pa/pp/PAPP3030/viewPop.do"))
                startActivity(intent)

//                val intent =
//                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.horsepia.com/hp/pa/pp/PAPP3031/index.do"))
//                startActivity(intent)
            }

            if (userDiv == "장수 목장") {
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.horsepia.com/hp/pa/pp/PAPP3020/viewPop.do"))
                startActivity(intent)
            }

        }


//        horseCheckButton.setOnClickListener {
//            val intent = Intent(this, HorseCheckActivity::class.java)
//            startActivity(intent)
//        }



        userButton.setOnClickListener {
            val intent = Intent(this, TrainerDivActivity::class.java)
            startActivity(intent)
            finish()
        }



//        helpButton.setOnClickListener {
//            val intent = Intent(this, HelpActivity::class.java)
//            startActivity(intent)
//        }


        exitButton.setOnClickListener {
            showExitConfirmationDialog()
        }




    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }


    override fun onBackPressed() {
        showExitConfirmationDialog()
    }




    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("정말로 종료 하시겠습니까?")
        builder.setPositiveButton("종료") { _, _ ->
            deleteCSVFiles()
        }
        builder.setNegativeButton("취소") { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun deleteCSVFiles() {
        val csvFileName = "horse.csv"
        val csvFileName2 = "trainer.csv"
        val csvFile = File(getExternalFilesDir(null), csvFileName)
        val csvFile2 = File(getExternalFilesDir(null), csvFileName2)
        val isDeleted = csvFile.delete()
        val isDeleted2 = csvFile2.delete()

        if (isDeleted && isDeleted2) {
            finish()
        } else {
            finish()
        }
    }




}
