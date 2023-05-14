package com.kratis1698

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.opencsv.CSVReader
import java.io.InputStreamReader

class TrainingReferenceActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_reference)

        scrollView = findViewById(R.id.scrollview)

        // training_reference.csv 파일을 읽어와서 스크롤뷰에 추가합니다.
        val linearLayout = findViewById<LinearLayout>(R.id.scrollviewlinearlayout)



// training_reference.csv 파일을 읽어와서 스크롤뷰에 추가합니다.
        val reader = CSVReader(InputStreamReader(resources.openRawResource(R.raw.training_reference)))
        var nextLine: Array<String>?
        while (reader.readNext().also { nextLine = it } != null) {
            addRow(linearLayout, nextLine)
        }
        reader.close()
    }


    private fun addRow(linearLayout: LinearLayout, row: Array<String>?) {
        if (row == null) {
            return
        }

        val rowLayout = LinearLayout(this)
        rowLayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        rowLayout.orientation = LinearLayout.HORIZONTAL

        var color: Int? = null // 배경색 지정을 위한 변수

        for ((index, col) in row.withIndex()) {
            val textView = TextView(this)
            textView.layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            textView.text = col
            textView.gravity = Gravity.CENTER
            textView.setPadding(2, 2, 2, 2)

            if (index == 4) {
                if (col.toFloat() >= 40) {
                    color = ContextCompat.getColor(applicationContext, R.color.red)
                } else if (col.toFloat() >= 20) {
                    color = ContextCompat.getColor(applicationContext, R.color.orange)
                } else if (col.toFloat() >= 10) {
                    color = ContextCompat.getColor(applicationContext, R.color.green)
                } else {
                    color = ContextCompat.getColor(applicationContext, R.color.grey)
                }
            }

            rowLayout.addView(textView)
        }
        // 배경색이 지정된 rowLayout을 linearLayout에 추가합니다.
        rowLayout.setBackgroundColor(color ?: ContextCompat.getColor(applicationContext, R.color.grey))
        linearLayout.addView(rowLayout)
    }





}