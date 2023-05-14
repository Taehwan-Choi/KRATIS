package com.kratis1698

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity


class TrainerSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trainer_selection)

        val tempUserDiv = intent.getStringExtra("User_Div")


        val sharedPref = getSharedPreferences(
            "KRAIS_Preferences", Context.MODE_PRIVATE)

        val editor = sharedPref.edit()


        val buttonLayout = findViewById<LinearLayout>(R.id.button_layout)

//        val trainerListUpdateTimeText = findViewById<TextView>(R.id.trainerListUpdateTime)
//
//
//        runOnUiThread {
//            trainerListUpdateTimeText.text = SimpleDateFormat("리스트 업데이트 : yyyy-MM-dd / HH:mm:ss", Locale.getDefault()).format(Date())
//        }




        val inputStream = resources.openRawResource(R.raw.kra_trainers)
        val trainers = mutableListOf<Triple<String, String, String>>()


        inputStream.reader(Charsets.UTF_8).buffered().useLines { lines ->
            lines.drop(1).forEach {

                val row = it.split(",")
                if (row[1] == tempUserDiv) {
                    trainers.add(Triple(row[0], row[2], row[3]))
//                    0은 PK, 1은 Div, 2는 Num, 3은 Name 순서
                }
            }
        }

        for (trainer in trainers) {
            val button = Button(this)
            button.text = String.format("%s / %s", trainer.second, trainer.third)
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)


            button.setOnClickListener {
                editor.putString("User_PK", trainer.first)
                editor.putString("User_Div", tempUserDiv)
                editor.putString("User_Num", trainer.second)
                editor.putString("User_Name", trainer.third)
                editor.apply()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            buttonLayout.addView(button)
        }



    }
}
