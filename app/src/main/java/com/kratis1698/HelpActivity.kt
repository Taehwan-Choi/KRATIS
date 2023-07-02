package com.kratis1698

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val howToUseButton = findViewById<Button>(R.id.tracking_button)
        howToUseButton.setOnClickListener {
//            val intent = Intent(this, HowToUseActivity::class.java)
//            startActivity(intent)
        }

        val localRecordButton = findViewById<Button>(R.id.record_button)
        localRecordButton.setOnClickListener {
            val intent = Intent(this, LocalRecordActivity::class.java)
            startActivity(intent)
        }

        val logFileButton = findViewById<Button>(R.id.log_file)
        logFileButton.setOnClickListener {
            val intent = Intent(this, LogCheckActivity::class.java)
            startActivity(intent)
        }


        val trainingReferenceButton = findViewById<Button>(R.id.horse_check_button)
        trainingReferenceButton.setOnClickListener {
            val intent = Intent(this, TrainingReferenceActivity::class.java)
            startActivity(intent)
        }

        val problemSolvingButton = findViewById<Button>(R.id.user_button)
        problemSolvingButton.setOnClickListener {
//            val intent = Intent(this, ProblemSolvingActivity::class.java)
//            startActivity(intent)
        }
    }
}





















//class HelpActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_help)
//
//        val helpText = findViewById<TextView>(R.id.help_text)
//        helpText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
//
//        helpText.visibility = View.VISIBLE
//
//    }
//}