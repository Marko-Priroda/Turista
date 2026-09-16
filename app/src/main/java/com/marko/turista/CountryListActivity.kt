package com.marko.turista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CountryListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val continent =
            intent.getStringExtra("continent") ?: "Európa"

        val main = LinearLayout(this)

        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(Color.rgb(221, 232, 213))
        main.setPadding(24, 45, 24, 24)

        val backButton = TextView(this)

        backButton.text = "←  Späť"
        backButton.textSize = 20f
        backButton.setTextColor(Color.rgb(38, 50, 56))
        backButton.setPadding(10, 15, 10, 15)

        backButton.setOnClickListener {
            finish()
        }

        main.addView(backButton)

        val title = TextView(this)

        title.text = "${getContinentEmoji(continent)}  $continent"
        title.textSize = 28f
        title.setTextColor(Color.rgb(38, 50, 56))
        title.gravity = Gravity.CENTER
        title.setPadding(0, 10, 0, 25)

        main.addView(title)

        val scrollView = ScrollView(this)

        val countryList = LinearLayout(this)
        countryList.orientation = LinearLayout.VERTICAL

        val countries = CountryData.countries.filter {
            it.continent == continent
        }

        for (country in countries) {
            addCountry(
                countryList,
                country.flag,
                country.name
            )
        }

        scrollView.addView(countryList)

        main.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(main)
    }

    private fun addCountry(
        root: LinearLayout,
        flag: String,
        country: String
    ) {

        val item = TextView(this)

        item.text = "$flag  $country    ›"
        item.textSize = 20f
        item.setTextColor(Color.rgb(38, 50, 56))
        item.setBackgroundColor(Color.WHITE)
        item.setPadding(20, 25, 20, 25)
        item.isClickable = true

        item.setOnClickListener {

            val intent = Intent(
                this,
                CountryActivity::class.java
            )

            intent.putExtra("country", country)
            intent.putExtra("flag", flag)

            startActivity(intent)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 10)

        root.addView(item, params)
    }

    private fun getContinentEmoji(
        continent: String
    ): String {

        return when (continent) {
            "Európa" -> "🌍"
            "Ázia" -> "🌏"
            "Afrika" -> "🌍"
            "Severná Amerika" -> "🌎"
            "Južná Amerika" -> "🌎"
            "Oceánia" -> "🏝️"
            "Antarktída" -> "🧊"
            else -> "🌍"
        }
    }
}