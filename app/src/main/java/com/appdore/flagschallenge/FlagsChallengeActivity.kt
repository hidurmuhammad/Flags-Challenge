package com.appdore.flagschallenge

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appdore.flagschallenge.model.Data
import com.appdore.flagschallenge.model.Question
import com.google.gson.Gson
import java.util.Locale

class FlagsChallengeActivity : AppCompatActivity() {
    private lateinit var hourEditText: EditText
    private lateinit var minuteEditText: EditText
    private lateinit var secondEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var flagImageView: ImageView
    private lateinit var option1Button: Button
    private lateinit var option2Button: Button
    private lateinit var option3Button: Button
    private lateinit var option4Button: Button
    private lateinit var resultTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var gameOverTextView: TextView
    private lateinit var scheduleLayout: View
    private lateinit var challengeLayout: View
    private lateinit var countdownTextView: TextView
    private lateinit var tvOption1: TextView
    private lateinit var tvOption2: TextView
    private lateinit var tvOption3: TextView
    private lateinit var tvOption4: TextView
    private lateinit var tvQuestionCount: TextView
    private var questions = ArrayList<Question>()
    private var currentQuestion = 0
    private var score = 0
    private var challengeStarted = false
    private var countDownTimer: CountDownTimer? = null
    private var currentQuestionTime: Long = 30000 // 30 seconds
    private var questionStartTime: Long = 0
    private var remainingTime: Long = 0
    private var isTimerRunning = false
    private var isActivityPaused = false
    private var isButtonClicked = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentQuestion = savedInstanceState.getInt("currentQuestion")
            questionStartTime = savedInstanceState.getLong("questionStartTime")
            challengeStarted = savedInstanceState.getBoolean("challengeStarted")
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_flags_challenge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        loadJson()
        saveButton.setOnClickListener {
            hideKeyboard(this)
            val hour = hourEditText.text.toString()
            val minute = minuteEditText.text.toString()
            val second = secondEditText.text.toString()

            if (hour.isEmpty() || minute.isEmpty() || second.isEmpty()) {
                Toast.makeText(this, "Please enter hour, minute, and second", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val hourValue = hour.toIntOrNull()
            val minuteValue = minute.toIntOrNull()
            val secondValue = second.toIntOrNull()

            if (hourValue == null || minuteValue == null || secondValue == null) {
                Toast.makeText(
                    this,
                    "Invalid input. Please enter valid numbers",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (hourValue < 0 || minuteValue < 0 || secondValue < 0) {
                Toast.makeText(this, "Time cannot be negative", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minuteValue >= 60 || secondValue >= 60) {
                Toast.makeText(
                    this,
                    "Invalid time format. Minutes and seconds should be less than 60",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val challengeTime =
                System.currentTimeMillis() + (hourValue * 3600 + minuteValue * 60 + secondValue) * 1000L
            startCountdown(challengeTime)
        }
    }

    private fun startCountdown(challengeTime: Long) {
        countdownTextView.visibility = View.VISIBLE
        object : CountDownTimer(challengeTime - System.currentTimeMillis(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                if (seconds <= 20) {
                    countdownTextView.text = "WILL START IN \n $seconds"
                    countdownTextView.visibility = View.VISIBLE
                } else {
                    countdownTextView.visibility = View.VISIBLE
                    timerTextView.text = "${formatTime(millisUntilFinished)}"
                }
            }

            override fun onFinish() {
                challengeStarted = true
                scheduleLayout.visibility = View.GONE
                challengeLayout.visibility = View.VISIBLE
                countdownTextView.visibility = View.GONE
                startChallenge()
            }
        }.start()
    }

    private fun startChallenge() {
        try {
            displayQuestion()
            questionStartTime = System.currentTimeMillis()
            isTimerRunning = true
            remainingTime = 30000
            startChallengeWithRemainingTime(remainingTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentQuestion", currentQuestion)
        outState.putLong("questionStartTime", questionStartTime)
        outState.putBoolean("challengeStarted", challengeStarted)
    }

    private fun displayQuestion() {
        try {
            val question = questions[currentQuestion]
            tvQuestionCount.text = "${currentQuestion + 1}"
            questionTextView.text = "Guess the Country by the Flag?"
            flagImageView.setImageResource(getFlagResource(question.country_code))
            val options = question.countries.map { it.country_name }.toTypedArray()
            option1Button.text = options[0]
            option2Button.text = options[1]
            option3Button.text = options[2]
            option4Button.text = options[3]
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        option1Button.setOnClickListener { handleOptionClick(0) }
        option2Button.setOnClickListener { handleOptionClick(1) }
        option3Button.setOnClickListener { handleOptionClick(2) }
        option4Button.setOnClickListener { handleOptionClick(3) }
    }

    private fun handleOptionClick(optionIndex: Int) {
        if (isButtonClicked) return
        isButtonClicked = true
        countDownTimer?.cancel()
        val question = questions[currentQuestion]
        val correctOptionIndex = question.countries.indexOfFirst { it.id == question.answer_id }

        if (optionIndex == correctOptionIndex) {
            score++
            resultTextView.text = getString(R.string.correct)
            when (optionIndex) {
                0 -> {
                    option1Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option1Button.setTextColor(resources.getColor(R.color.black))
                    tvOption1.visibility = View.VISIBLE
                    tvOption1.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption1.text = getString(R.string.correct)
                }

                1 -> {
                    option2Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option2Button.setTextColor(resources.getColor(R.color.black))
                    tvOption2.visibility = View.VISIBLE
                    tvOption2.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption2.text = getString(R.string.correct)
                }

                2 -> {
                    option3Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option3Button.setTextColor(resources.getColor(R.color.black))
                    tvOption3.visibility = View.VISIBLE
                    tvOption3.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption3.text = getString(R.string.correct)
                }

                3 -> {
                    option4Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option4Button.setTextColor(resources.getColor(R.color.black))
                    tvOption4.visibility = View.VISIBLE
                    tvOption4.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption4.text = getString(R.string.correct)
                }
            }
        } else {
            resultTextView.text = getString(R.string.wrong)
            when (optionIndex) {
                0 -> {
                    option1Button.setBackgroundResource(R.drawable.filled_rounded_border)
                    option1Button.setTextColor(resources.getColor(R.color.white))
                    tvOption1.visibility = View.VISIBLE
                    tvOption1.setTextColor(resources.getColor(R.color.wrong_red))
                    tvOption1.text = getString(R.string.wrong)
                }

                1 -> {
                    option2Button.setBackgroundResource(R.drawable.filled_rounded_border)
                    option2Button.setTextColor(resources.getColor(R.color.white))
                    tvOption2.visibility = View.VISIBLE
                    tvOption2.setTextColor(resources.getColor(R.color.wrong_red))
                    tvOption2.text = getString(R.string.wrong)
                }

                2 -> {
                    option3Button.setBackgroundResource(R.drawable.filled_rounded_border)
                    option3Button.setTextColor(resources.getColor(R.color.white))
                    tvOption3.visibility = View.VISIBLE
                    tvOption3.setTextColor(resources.getColor(R.color.wrong_red))
                    tvOption3.text = getString(R.string.wrong)
                }

                3 -> {
                    option4Button.setBackgroundResource(R.drawable.filled_rounded_border)
                    option4Button.setTextColor(resources.getColor(R.color.white))
                    tvOption4.visibility = View.VISIBLE
                    tvOption4.setTextColor(resources.getColor(R.color.wrong_red))
                    tvOption4.text = getString(R.string.wrong)
                }
            }
            when (correctOptionIndex) {
                0 -> {
                    option1Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option1Button.setTextColor(resources.getColor(R.color.black))
                    tvOption1.visibility = View.VISIBLE
                    tvOption1.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption1.text = getString(R.string.correct)
                }

                1 -> {
                    option2Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option2Button.setTextColor(resources.getColor(R.color.black))
                    tvOption2.visibility = View.VISIBLE
                    tvOption2.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption2.text = getString(R.string.correct)
                }

                2 -> {
                    option3Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option3Button.setTextColor(resources.getColor(R.color.black))
                    tvOption3.visibility = View.VISIBLE
                    tvOption3.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption3.text = getString(R.string.correct)
                }

                3 -> {
                    option4Button.setBackgroundResource(R.drawable.green_rounded_border)
                    option4Button.setTextColor(resources.getColor(R.color.black))
                    tvOption4.visibility = View.VISIBLE
                    tvOption4.setTextColor(resources.getColor(R.color.correct_green))
                    tvOption4.text = getString(R.string.correct)
                }
            }
        }

        scoreTextView.text = "SCORE: $score/${questions.size}"

        remainingTime = 30000 - (System.currentTimeMillis() - questionStartTime)
        if (remainingTime < 0) {
            remainingTime = 0
        }

        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "00:${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                resultTextView.text = ""
                resetButtons()
                currentQuestion++
                isButtonClicked = false
                if (currentQuestion < questions.size) {
                    displayQuestion()
                    questionStartTime = System.currentTimeMillis()
                    startChallengeWithRemainingTime(30000)
                } else {
                    gameOver()
                }
            }
        }.start()
    }

    private fun resetButtons() {
        option1Button.setBackgroundResource(R.drawable.gray_rounded_border)
        option1Button.setTextColor(resources.getColor(R.color.black))
        option2Button.setBackgroundResource(R.drawable.gray_rounded_border)
        option2Button.setTextColor(resources.getColor(R.color.black))
        option3Button.setBackgroundResource(R.drawable.gray_rounded_border)
        option3Button.setTextColor(resources.getColor(R.color.black))
        option4Button.setBackgroundResource(R.drawable.gray_rounded_border)
        option4Button.setTextColor(resources.getColor(R.color.black))
        tvOption1.visibility = View.GONE
        tvOption2.visibility = View.GONE
        tvOption3.visibility = View.GONE
        tvOption4.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
    }

    override fun onPause() {
        super.onPause()
        challengeStarted = false
        isActivityPaused = true
        countDownTimer?.let {
            it.cancel()
            countDownTimer = null
        }
        if (isTimerRunning) {
            remainingTime = currentQuestionTime - (System.currentTimeMillis() - questionStartTime)
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityPaused = false
        if (isTimerRunning && remainingTime > 0) {
            questionStartTime = System.currentTimeMillis() - (currentQuestionTime - remainingTime)
            startChallengeWithRemainingTime(remainingTime)
        }
    }

    private fun startChallengeWithRemainingTime(remainingTime: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                resultTextView.text = ""
                resetButtons()
                currentQuestion++
                if (currentQuestion < questions.size) {
                    displayQuestion()
                    questionStartTime = System.currentTimeMillis()
                    startChallengeWithRemainingTime(30000)
                } else {
                    gameOver()
                }
            }
        }.start()
    }

    private fun gameOver() {
        challengeStarted = false
        isTimerRunning = false
        countDownTimer?.cancel()
        countDownTimer = null
        challengeLayout.visibility = View.GONE
        gameOverTextView.visibility = View.VISIBLE
        timerTextView.text = "00:00"
        gameOverTextView.text = "GAME OVER\nSCORE: $score/${questions.size}"
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(millis: Long): String {
        val hours = millis / (60 * 60 * 1000)
        val minutes = (millis % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = ((millis % (60 * 60 * 1000)) % (60 * 1000)) / 1000

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun getFlagResource(countryCode: String): Int {
        return try {
            return resources.getIdentifier(
                "flag_${countryCode.lowercase(Locale.getDefault())}",
                "drawable",
                packageName
            )
        } catch (e: Exception) {
            // Handle the case where the flag resource is not found
            e.printStackTrace()
            R.drawable.default_flag // Return a default flag resource
        }
    }

    private fun loadJson() {
        // Parse JSON
        val json = """
         {
       "questions": [
           {
               "answer_id": 160,
               "countries": [
                   {
                       "country_name": "Bosnia and Herzegovina",
                       "id": 29
                   },
                   {
                       "country_name": "Mauritania",
                       "id": 142
                   },
                   {
                       "country_name": "Chile",
                       "id": 45
                   },
                   {
                       "country_name": "New Zealand",
                       "id": 160
                   }
               ],
               "country_code": "NZ"
           },
           {
               "answer_id": 13,
               "countries": [
                   {
                       "country_name": "Aruba",
                       "id": 13
                   },
                   {
                       "country_name": "Serbia",
                       "id": 184
                   },
                   {
                       "country_name": "Montenegro",
                       "id": 150
                   },
                   {
                       "country_name": "Moldova",
                       "id": 147
                   }
               ],
               "country_code": "AW"
           },
           {
               "answer_id": 66,
               "countries": [
                   {
                       "country_name": "Kenya",
                       "id": 117
                   },
                   {
                       "country_name": "Montenegro",
                       "id": 150
                   },
                   {
                       "country_name": "Ecuador",
                       "id": 66
                   },
                   {
                       "country_name": "Bhutan",
                       "id": 26
                   }
               ],
               "country_code": "EC"
           },
           {
               "answer_id": 174,
               "countries": [
                   {
                       "country_name": "Niue",
                       "id": 164
                   },
                   {
                       "country_name": "Paraguay",
                       "id": 174
                   },
                   {
                       "country_name": "Tuvalu",
                       "id": 232
                   },
                   {
                       "country_name": "Indonesia",
                       "id": 105
                   }
               ],
               "country_code": "PY"
           },
           {
               "answer_id": 122,
               "countries": [
                   {
                       "country_name": "Kyrgyzstan",
                       "id": 122
                   },
                   {
                       "country_name": "Zimbabwe",
                       "id": 250
                   },
                   {
                       "country_name": "Saint Lucia",
                       "id": 190
                   },
                   {
                       "country_name": "Ireland",
                       "id": 108
                   }
               ],
               "country_code": "KG"
           },
           {
               "answer_id": 192,
               "countries": [
                   {
                       "country_name": "Saint Pierre and Miquelon",
                       "id": 192
                   },
                   {
                       "country_name": "Namibia",
                       "id": 155
                   },
                   {
                       "country_name": "Greece",
                       "id": 87
                   },
                   {
                       "country_name": "Anguilla",
                       "id": 8
                   }
               ],
               "country_code": "PM"
           },
           {
               "answer_id": 113,
               "countries": [
                   {
                       "country_name": "Belarus",
                       "id": 21
                   },
                   {
                       "country_name": "Falkland Islands",
                       "id": 73
                   },
                   {
                       "country_name": "Japan",
                       "id": 113
                   },
                   {
                       "country_name": "Iraq",
                       "id": 107
                   }
               ],
               "country_code": "JP"
           },
           {
               "answer_id": 230,
               "countries": [
                   {
                       "country_name": "Barbados",
                       "id": 20
                   },
                   {
                       "country_name": "Italy",
                       "id": 111
                   },
                   {
                       "country_name": "Turkmenistan",
                       "id": 230
                   },
                   {
                       "country_name": "Cocos Island",
                       "id": 48
                   }
               ],
               "country_code": "TM"
           },
           {
               "answer_id": 81,
               "countries": [
                   {
                       "country_name": "Maldives",
                       "id": 137
                   },
                   {
                       "country_name": "Aruba",
                       "id": 13
                   },
                   {
                       "country_name": "Monaco",
                       "id": 148
                   },
                   {
                       "country_name": "Gabon",
                       "id": 81
                   }
               ],
               "country_code": "GA"
           },
           {
               "answer_id": 141,
               "countries": [
                   {
                       "country_name": "Martinique",
                       "id": 141
                   },
                   {
                       "country_name": "Montenegro",
                       "id": 150
                   },
                   {
                       "country_name": "Barbados",
                       "id": 20
                   },
                   {
                       "country_name": "Monaco",
                       "id": 148
                   }
               ],
               "country_code": "MQ"
           },
           {
               "answer_id": 23,
               "countries": [
                   {
                       "country_name": "Germany",
                       "id": 84
                   },
                   {
                       "country_name": "Dominica",
                       "id": 63
                   },
                   {
                       "country_name": "Belize",
                       "id": 23
                   },
                   {
                       "country_name": "Tuvalu",
                       "id": 232
                   }
               ],
               "country_code": "BZ"
           },
           {
               "answer_id": 60,
               "countries": [
                   {
                       "country_name": "Falkland Islands",
                       "id": 73
                   },
                   {
                       "country_name": "Czech Republic",
                       "id": 60
                   },
                   {
                       "country_name": "Mauritania",
                       "id": 142
                   },
                   {
                       "country_name": "British Indian Ocean Territory",
                       "id": 33
                   }
               ],
               "country_code": "CZ"
           },
           {
               "answer_id": 235,
               "countries": [
                   {
                       "country_name": "United Arab Emirates",
                       "id": 235
                   },
                   {
                       "country_name": "United Arab Emirates",
                       "id": 235
                   },
                   {
                       "country_name": "Macedonia",
                       "id": 133
                   },
                   {
                       "country_name": "Guernsey",
                       "id": 93
                   }
               ],
               "country_code": "AE"
           },
           {
               "answer_id": 114,
               "countries": [
                   {
                       "country_name": "Turks and Caicos Islands",
                       "id": 231
                   },
                   {
                       "country_name": "Myanmar",
                       "id": 154
                   },
                   {
                       "country_name": "Jersey",
                       "id": 114
                   },
                   {
                       "country_name": "Ethiopia",
                       "id": 72
                   }
               ],
               "country_code": "JE"
           },
           {
               "answer_id": 126,
               "countries": [
                   {
                       "country_name": "Malawi",
                       "id": 135
                   },
                   {
                       "country_name": "Trinidad and Tobago",
                       "id": 227
                   },
                   {
                       "country_name": "Nepal",
                       "id": 157
                   },
                   {
                       "country_name": "Lesotho",
                       "id": 126
                   }
               ],
               "country_code": "LS"
           }
       ]
   }
""".trimIndent()
        val gson = Gson()
        val data = gson.fromJson(json, Data::class.java)
        questions = data.questions
    }

    private fun initView() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        hourEditText = findViewById(R.id.hour_edit_text)
        minuteEditText = findViewById(R.id.minute_edit_text)
        secondEditText = findViewById(R.id.second_edit_text)
        saveButton = findViewById(R.id.save_button)
        timerTextView = findViewById(R.id.timer_text_view)
        questionTextView = findViewById(R.id.question_text_view)
        countdownTextView = findViewById(R.id.countdown_text_view)
        flagImageView = findViewById(R.id.flag_image_view)
        option1Button = findViewById(R.id.option_1_button)
        option2Button = findViewById(R.id.option_2_button)
        option3Button = findViewById(R.id.option_3_button)
        option4Button = findViewById(R.id.option_4_button)
        resultTextView = findViewById(R.id.result_text_view)
        scoreTextView = findViewById(R.id.score_text_view)
        tvOption1 = findViewById(R.id.tv_option1)
        tvOption2 = findViewById(R.id.tv_option2)
        tvOption3 = findViewById(R.id.tv_option3)
        tvOption4 = findViewById(R.id.tv_option4)
        tvQuestionCount = findViewById(R.id.tv_question_count)
        gameOverTextView = findViewById(R.id.game_over_text_view)
        scheduleLayout = findViewById(R.id.schedule_layout)
        challengeLayout = findViewById(R.id.challenge_layout)

        challengeLayout.visibility = View.GONE
        gameOverTextView.visibility = View.GONE
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        private const val COUNTDOWN_BEFORE_CHALLENGE = 20 // seconds
        private const val QUESTION_TIME = 30 // seconds
        private const val INTERVAL_TIME = 10 // seconds
        private const val COUNTDOWN_INTERVAL = 1000 // milliseconds
        private const val CHALLENGE_COUNTDOWN_INTERVAL = 1000 // milliseconds
        private const val INTERVAL_COUNTDOWN = 30000 // milliseconds
    }
}