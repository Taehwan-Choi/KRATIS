package com.kratis1698

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class TrainerSelectionActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var buttonLayout: LinearLayout
    private lateinit var trainers: MutableList<Triple<String, String, String>>
    private lateinit var tempUserDiv: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trainer_selection)

        val radioButton1 = findViewById<RadioButton>(R.id.radioButton)
        val radioButton2 = findViewById<RadioButton>(R.id.radioButton2)



// Find the RadioGroup by its id and set visibility to GONE
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.visibility = View.GONE

        // Find the EditText by its id and set visibility to GONE
        val editText = findViewById<EditText>(R.id.editTextTextPersonName2)
        editText.visibility = View.GONE




// 라디오 버튼 클릭 이벤트 리스너 설정
//        radioButton1.setOnClickListener {
//            editText.isEnabled = true
//            editText.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
//                // 숫자만 허용하는 필터링 로직
//                source.filter { it.isDigit() }
//            })
//
//            // EditText에 숫자 이외의 글자가 있는 경우 제거
//            val text = editText.text.toString()
//            val filteredText = text.filter { it.isDigit() }
//            if (text != filteredText) {
//                editText.setText(filteredText)
//                editText.setSelection(filteredText.length) // 커서를 항상 끝에 위치하도록
//            }
//
//            val toast = Toast.makeText(this@TrainerSelectionActivity, "숫자만 입력해 주세요", Toast.LENGTH_SHORT)
//            val timer = Timer()
//            timer.schedule(object : TimerTask() {
//                override fun run() {
//                    runOnUiThread {
//                        toast.cancel()
//                    }
//                }
//            }, 100) // 2초 후에 토스트 메시지가 자동으로 사라집니다.
//            toast.show()
//
//            editText.requestFocus() // editText에 포커스 요청
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT) // 키패드 표시
//            editText.inputType = InputType.TYPE_CLASS_NUMBER // 숫자 키패드로 설정
//        }
//
//        radioButton2.setOnClickListener {
//            editText.isEnabled = true
//            editText.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
//                // 한글만 허용하는 필터링 로직
//                val regex = Regex("[가-힣ㄱ-ㅎㅏ-ㅣㆍᆢ]+|^$")
//                if (source.toString().matches(regex)) {
//                    // 한글일 경우 허용
//                    source
//                } else {
//                    // 한글이 아닐 경우 입력 차단
//                    val toast = Toast.makeText(this@TrainerSelectionActivity, "한글만 입력해 주세요", Toast.LENGTH_SHORT)
//                    val timer = Timer()
//                    timer.schedule(object : TimerTask() {
//                        override fun run() {
//                            runOnUiThread {
//                                toast.cancel()
//                            }
//                        }
//                    }, 100) // 2초 후에 토스트 메시지가 자동으로 사라집니다.
//                    toast.show()
//
//                    ""
//                }
//            })
//
//            editText.setText("") // editText 안의 글자를 모두 지움
//            editText.requestFocus() // editText에 포커스 요청
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT) // 키패드 표시
//            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME // 한글 키패드로 설정
//        }



        tempUserDiv = intent.getStringExtra("User_Div").toString()


        buttonLayout = findViewById(R.id.button_layout)
//        editText = findViewById(R.id.editTextTextPersonName2)
//        editText.setText("이름이나 번호를 선택해 주세요")


        val inputStream = File(getExternalFilesDir(null), "trainer.csv")
        trainers = mutableListOf()

        loadTrainers(tempUserDiv, inputStream)

//        if (!radioButton1.isChecked && !radioButton2.isChecked) {
//            editText.isEnabled = false
//        }
//        editText.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//
//
//                // 이벤트 발생 전에 수행할 작업
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//
//                // 텍스트 변경 시 수행할 작업
//                val text = s.toString()
//                if (text.isBlank()) {
//                    loadTrainers(tempUserDiv, inputStream)
//                }
//                else if(text.isDigitsOnly()){
//                    filterTrainers2(text)
//                }
//                else {
//                    filterTrainers(text)
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                loadTrainers2(tempUserDiv, inputStream)
//
//                editText.addTextChangedListener(object : TextWatcher {
//                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                        // 이벤트 발생 전에 수행할 작업
//                    }
//
//                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                        // 텍스트 변경 시 수행할 작업
//                        val text = s.toString()
//                        refreshButtonTextColor(text)
//                    }
//
//                    override fun afterTextChanged(s: Editable?) {
//                        // 텍스트 변경 후 수행할 작업
//                    }
//                })
//            }
//        })
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

//    private fun loadTrainers2(tempUserDiv: String?, file: File) {
//        trainers.clear()
//        file.reader(Charsets.UTF_8).buffered().useLines { lines ->
//            lines.drop(1).forEach {
//                val row = it.split(",")
//                if (row[1] == tempUserDiv) {
//                    trainers.add(Triple(row[0], row[2], row[3]))
//                }
//            }
//        }
//    }

//    private fun filterTrainers(text: String) {
//        val filteredTrainers = trainers.filter { trainer ->
//            trainer.third.contains(text, ignoreCase = true)
//        }
//        trainers.clear()
//        trainers.addAll(filteredTrainers)
//        refreshButtons()
//
//        if (filteredTrainers.isEmpty()) {
//            // 일치하는 결과가 없는 경우 문구를 표시하는 로직
//            val noResultText = "일치하는 결과가 없습니다"
//            val noResultButton = Button(this)
//            noResultButton.tag = "noResultButton"  // 태그를 설정하여 나중에 찾기 위해 사용
//            noResultButton.text = noResultText.replace("/", "")
//            noResultButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
//            buttonLayout.addView(noResultButton)
//        } else {
//            // 일치하는 결과가 있을 경우 문구를 표시하는 로직을 제거
//            val noResultButton = buttonLayout.findViewWithTag<Button>("noResultButton")
//            noResultButton?.let {
//                buttonLayout.removeView(it)
//            }
//        }
//    }


//    private fun filterTrainers2(text: String) {
//        val filteredTrainers = trainers.filter { trainer ->
//            trainer.second.contains(text, ignoreCase = true)
//        }
//        trainers.clear()
//        trainers.addAll(filteredTrainers)
//        refreshButtons()
//        if (filteredTrainers.isEmpty()) {
//            // 일치하는 결과가 없는 경우 문구를 표시하는 로직
//            val noResultText = "일치하는 결과가 없습니다"
//            val noResultButton = Button(this)
//            noResultButton.tag = "noResultButton"  // 태그를 설정하여 나중에 찾기 위해 사용
//            noResultButton.text = noResultText.replace("/", "")
//            noResultButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
//            buttonLayout.addView(noResultButton)
//        } else {
//            // 일치하는 결과가 있을 경우 문구를 표시하는 로직을 제거
//            val noResultButton = buttonLayout.findViewWithTag<Button>("noResultButton")
//            noResultButton?.let {
//                buttonLayout.removeView(it)
//            }
//        }
//
//
//    }
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

        // 버튼 텍스트 색상 변경 함수

    }
//    private fun refreshButtonTextColor(text: String) {
//        val buttonCount = buttonLayout.childCount
//        for (i in 0 until buttonCount) {
//            val button = buttonLayout.getChildAt(i) as Button
//            val buttonText = button.text.toString()
//            val startIndex = buttonText.indexOf(text, ignoreCase = true)
//
//            if(buttonText=="일치하는 결과가 없습니다"){return}
//
//            if (startIndex >= 0) {
//                // 입력된 텍스트와 일치하는 부분만 노란색으로 변경
//                val spannable = SpannableString(buttonText)
//                val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.yellow))
//                spannable.setSpan(
//                    foregroundColorSpan,
//                    startIndex,
//                    startIndex + text.length,
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//                button.text = spannable
//            } else {
//                // 일치하지 않을 경우 기본 색상으로 변경
//                button.setTextColor(ContextCompat.getColor(this, R.color.white))
//            }
//        }
//    }

}




//class TrainerSelectionActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_trainer_selection)
//
//        val tempUserDiv = intent.getStringExtra("User_Div")
//
//
//        val sharedPref = getSharedPreferences(
//            "KRAIS_Preferences", Context.MODE_PRIVATE)
//
//        val editor = sharedPref.edit()
//
//
//        val buttonLayout = findViewById<LinearLayout>(R.id.button_layout)
//
////        val trainerListUpdateTimeText = findViewById<TextView>(R.id.trainerListUpdateTime)
////
////
////        runOnUiThread {
////            trainerListUpdateTimeText.text = SimpleDateFormat("리스트 업데이트 : yyyy-MM-dd / HH:mm:ss", Locale.getDefault()).format(Date())
////        }
//
//
//
//
//        val inputStream = resources.openRawResource(R.raw.kra_trainers)
//        val trainers = mutableListOf<Triple<String, String, String>>()
//
//
//        inputStream.reader(Charsets.UTF_8).buffered().useLines { lines ->
//            lines.drop(1).forEach {
//
//                val row = it.split(",")
//                if (row[1] == tempUserDiv) {
//                    trainers.add(Triple(row[0], row[2], row[3]))
////                    0은 PK, 1은 Div, 2는 Num, 3은 Name 순서
//                }
//            }
//        }
//
//        for (trainer in trainers) {
//            val button = Button(this)
//            button.text = String.format("%s / %s", trainer.second, trainer.third)
//            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
//
//
//            button.setOnClickListener {
//                editor.putString("User_PK", trainer.first)
//                editor.putString("User_Div", tempUserDiv)
//                editor.putString("User_Num", trainer.second)
//                editor.putString("User_Name", trainer.third)
//                editor.apply()
//                val intent = Intent(this, MainActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
//                startActivity(intent)
//                finish()
//            }
//            buttonLayout.addView(button)
//        }
//
//
//
//    }
//}
