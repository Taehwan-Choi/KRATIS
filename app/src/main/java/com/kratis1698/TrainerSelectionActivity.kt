package com.kratis1698

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class TrainerSelectionActivity : AppCompatActivity() {
    private lateinit var buttonLayout: LinearLayout
    private lateinit var trainers: MutableList<Triple<String, String, String>>
    private lateinit var tempUserDiv: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trainer_selection)

        tempUserDiv = intent.getStringExtra("User_Div").toString()

        buttonLayout = findViewById(R.id.button_layout)

        val inputStream = File(getExternalFilesDir(null), "trainer.csv")
        trainers = mutableListOf()

        loadTrainers(tempUserDiv, inputStream)


    }

    private fun loadTrainers(tempUserDiv: String?, file: File) {
        trainers.clear()
        file.reader(Charsets.UTF_8).buffered().useLines { lines ->
            lines.drop(1).forEach {
                val row = it.split(",")
                if (row[1] == tempUserDiv) {
                    trainers.add(Triple(row[0], row[2], row[3]))
                }
            }
        }
        println(trainers)
        refreshButtons()

    }

    private fun refreshButtons() {
        buttonLayout.removeAllViews()
        for (trainer in trainers) {
            val button = createButton(trainer)
            buttonLayout.addView(button)
        }
    }

    private fun createButton(trainer: Triple<String, String, String>): Button {
        val button = Button(this)
        button.text = String.format("%s", trainer.third)
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)

        button.setOnClickListener {

            val tempUserDiv = intent.getStringExtra("User_Div")

            val sharedPref = getSharedPreferences("Shared_Preferences", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()

            editor.putString("User_Div", tempUserDiv)
            editor.putString("User_PK", trainer.first)
            editor.putString("User_Num", trainer.second)
            editor.putString("User_Name", trainer.third)
            editor.apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        return button


    }
}