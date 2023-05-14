package com.kratis1698

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val howToUseButton = findViewById<Button>(R.id.how_to_use)
        howToUseButton.setOnClickListener {
//            val intent = Intent(this, HowToUseActivity::class.java)
//            startActivity(intent)
        }

        val localRecordButton = findViewById<Button>(R.id.local_record)
        localRecordButton.setOnClickListener {
            val intent = Intent(this, LocalRecordActivity::class.java)
            startActivity(intent)
        }

        val logFileButton = findViewById<Button>(R.id.log_file)
        logFileButton.setOnClickListener {
            val intent = Intent(this, LogCheckActivity::class.java)
            startActivity(intent)
        }


        val trainingReferenceButton = findViewById<Button>(R.id.training_reference)
        trainingReferenceButton.setOnClickListener {
            val intent = Intent(this, TrainingReferenceActivity::class.java)
            startActivity(intent)
        }

        val problemSolvingButton = findViewById<Button>(R.id.problem_solving)
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