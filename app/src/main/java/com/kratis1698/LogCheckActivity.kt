package com.kratis1698

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.*

class LogCheckActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var dataAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_check)

        listView = findViewById(R.id.list_view)

        val listOfData = readFromFile()

        setListView(listOfData)
    }

    private fun readFromFile(): List<String> {
        val file = File(applicationContext.filesDir, "LogRecord")

        if (!file.exists()) {
            return emptyList()
        }

        val data = mutableListOf<String>()
        try {
            val scanner = Scanner(file)

            while (scanner.hasNextLine()) {
                data.add(scanner.nextLine())
            }

            scanner.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    private fun setListView(listOfData: List<String>) {
        val reversedList = listOfData.reversed()
        dataAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, reversedList)
        listView.adapter = dataAdapter
    }
}