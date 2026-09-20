package com.marko.turista

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class CountryActivity : AppCompatActivity() {

    companion object {
        private const val STYLE_URL =
            "https://tiles.openfreemap.org/styles/liberty"
    }

    private lateinit var status: TextView
    private lateinit var downloadButton: Button
    private lateinit var progress: ProgressBar
    private var routingDownloadStarted = false

    private val offlineManager by lazy {
        OfflineManager.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        val countryName =
            intent.getStringExtra("country") ?: "Krajina"

        val countryFlag =
            intent.getStringExtra("flag") ?: "🌍"

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(OfflineUi.cream)
        main.setPadding(OfflineUi.dp(this,18),OfflineUi.dp(this,8),OfflineUi.dp(this,18),OfflineUi.dp(this,24))

        val backButton = TextView(this)
        backButton.text = "←  Späť"
        backButton.textSize = 20f
        backButton.setTextColor(Color.rgb(38, 50, 56))
        backButton.setPadding(10, 15, 10, 15)
        backButton.setOnClickListener { finish() }
        main.addView(backButton)

        val title = TextView(this)
        title.text = countryName
        title.textSize = 28f
        title.setTextColor(Color.rgb(38, 50, 56))
        title.gravity = Gravity.START
        title.setTypeface(null,android.graphics.Typeface.BOLD)
        title.setPadding(0, 20, 0, 25)
        main.addView(title)

        status = TextView(this)
        status.text =
            "Stiahne sa mapa aj dáta pre offline navigáciu."
        status.textSize = 17f
        status.setTextColor(Color.rgb(38, 50, 56))
        status.setPadding(10, 10, 10, 20)
        status.background=OfflineUi.surface(this)
        status.setPadding(OfflineUi.dp(this,16),OfflineUi.dp(this,16),OfflineUi.dp(this,16),OfflineUi.dp(this,16))
        main.addView(status)

        progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        )
        progress.max = 100
        progress.progress = 0
        progress.visibility = ProgressBar.GONE
        main.addView(progress)

        downloadButton = Button(this)
        downloadButton.text = "Stiahnuť mapu + navigáciu"
        downloadButton.setOnClickListener {
            downloadButton.isEnabled = false
            progress.visibility = ProgressBar.VISIBLE
            status.text = "Hľadám hranice štátu..."
            findCountryBounds(countryName, countryFlag)
        }
        downloadButton.isAllCaps=false
        downloadButton.setTextColor(android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled),intArrayOf()),intArrayOf(Color.WHITE,Color.LTGRAY)))
        downloadButton.background=OfflineUi.surface(this,OfflineUi.ink)
        downloadButton.minHeight=OfflineUi.dp(this,56)
        main.addView(downloadButton,LinearLayout.LayoutParams(-1,-2).apply{topMargin=OfflineUi.dp(this@CountryActivity,18);bottomMargin=OfflineUi.dp(this@CountryActivity,12)})

        val note = TextView(this)
        note.text =
            "\nMapa celého štátu sa ukladá v základnom zoome 6–12. " +
            "Routovacie dáta môžu mať ďalšie stovky MB podľa veľkosti štátu."
        note.textSize = 15f
        note.setTextColor(Color.DKGRAY)
        main.addView(note)

        val info = TextView(this)
        info.text =
            "\nPo dokončení bude Turista počítať trasu po cestách " +
            "a chodníkoch priamo v telefóne, bez ďalšej aplikácie."
        info.textSize = 16f
        info.setTextColor(Color.DKGRAY)
        main.addView(info)

        val scroll=android.widget.ScrollView(this).apply{setBackgroundColor(OfflineUi.cream);isFillViewport=true;addView(main)}
        setContentView(scroll)
        ScreenInsets.applyTo(scroll)
    }

    private fun findCountryBounds(
        countryName: String,
        countryFlag: String
    ) {
        thread {
            var connection: HttpURLConnection? = null

            try {
                val encoded =
                    URLEncoder.encode(
                        countryName,
                        "UTF-8"
                    )

                val url = URL(
                    "https://nominatim.openstreetmap.org/search" +
                    "?format=json&limit=1&featuretype=country&polygon_geojson=0&q=$encoded"
                )

                connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "User-Agent",
                    "Turista/1.0"
                )

                if (
                    connection.responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {
                    throw IllegalStateException(
                        "Nominatim HTTP ${connection.responseCode}"
                    )
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val array = JSONArray(response)

                if (array.length() == 0) {
                    throw IllegalStateException(
                        "Hranice štátu sa nenašli"
                    )
                }

                val item =
                    array.getJSONObject(0)

                val bbox =
                    item.optJSONArray("boundingbox")
                        ?: throw IllegalStateException(
                            "Štát nemá dostupný rozsah mapy"
                        )

                val south = bbox.getDouble(0)
                val north = bbox.getDouble(1)
                val west = bbox.getDouble(2)
                val east = bbox.getDouble(3)

                runOnUiThread {
                    createCountryOfflineRegion(
                        countryName,
                        countryFlag,
                        west,
                        south,
                        east,
                        north
                    )
                }

            } catch (exception: Exception) {

                runOnUiThread {
                    progress.visibility =
                        ProgressBar.GONE

                    downloadButton.isEnabled =
                        true

                    status.text =
                        "❌ Nepodarilo sa nájsť hranice štátu.\n" +
                        (exception.message ?: "Neznáma chyba")
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun createCountryOfflineRegion(
        countryName: String,
        countryFlag: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double
    ) {
        val countrySpan = maxOf(
            kotlin.math.abs(east - west),
            kotlin.math.abs(north - south)
        )

        val maximumZoom = when {
            countrySpan <= 2.0 -> 14.0
            countrySpan <= 8.0 -> 12.0
            countrySpan <= 20.0 -> 10.0
            countrySpan <= 50.0 -> 8.0
            else -> 7.0
        }

        val bounds =
            LatLngBounds.Builder()
                .include(LatLng(south, west))
                .include(LatLng(north, east))
                .build()

        val metadata =
            JSONObject().apply {
                put(
                    "name",
                    "country_$countryName"
                )
                put(
                    "displayName",
                    "$countryFlag $countryName – celý štát"
                )
                put(
                    "country",
                    countryName
                )
                put("west", west)
                put("south", south)
                put("east", east)
                put("north", north)
                put("minZoom", 6.0)
                put("maxZoom", maximumZoom)
            }
                .toString()
                .toByteArray(Charsets.UTF_8)

        status.text =
            "📦  Pripravujem offline mapu $countryName..."

        val definition =
            OfflineTilePyramidRegionDefinition(
                STYLE_URL,
                bounds,
                6.0,
                maximumZoom,
                resources.displayMetrics.density
            )

        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object :
                OfflineManager.CreateOfflineRegionCallback {

                override fun onCreate(
                    region: OfflineRegion
                ) {

                    region.setObserver(
                        object :
                            OfflineRegion.OfflineRegionObserver {

                            override fun onStatusChanged(
                                statusValue: OfflineRegionStatus
                            ) {

                                val total =
                                    statusValue
                                        .requiredResourceCount

                                val done =
                                    statusValue
                                        .completedResourceCount

                                val percent =
                                    if (total > 0) {
                                        (
                                            done.toDouble() /
                                            total.toDouble() *
                                            100.0
                                        )
                                            .toInt()
                                            .coerceIn(0, 100)
                                    } else {
                                        0
                                    }

                                runOnUiThread {
                                    progress.progress =
                                        percent

                                    status.text =
                                        "$countryName\n" +
                                        "$percent %  •  " +
                                        "$done / $total zdrojov"
                                }

                                if (
                                    total > 0 &&
                                    done >= total
                                ) {
                                    region.setDownloadState(
                                        OfflineRegion.STATE_INACTIVE
                                    )

                                    runOnUiThread {
                                        progress.progress =
                                            100

                                        status.text =
                                            " Mapa je uložená.\n" +
                                            "Pripravujem offline navigáciu..."

                                        downloadButton.text =
                                            "⏳  Sťahujem navigáciu"

                                        downloadButton.isEnabled =
                                            false

                                        downloadRoutingData(
                                            countryName,
                                            west,
                                            south,
                                            east,
                                            north
                                        )
                                    }
                                }
                            }

                            override fun onError(
                                error: OfflineRegionError
                            ) {
                                runOnUiThread {
                                    progress.visibility =
                                        ProgressBar.GONE

                                    downloadButton.isEnabled =
                                        true

                                    status.text =
                                        "❌ Sťahovanie zlyhalo: " +
                                        (
                                            error.message
                                                ?: "neznáma chyba"
                                        )
                                }
                            }

                            override fun mapboxTileCountLimitExceeded(
                                limit: Long
                            ) {
                                runOnUiThread {
                                    progress.visibility =
                                        ProgressBar.GONE

                                    downloadButton.isEnabled =
                                        true

                                    status.text =
                                        "❌ Mapa je príliš veľká pre jedno " +
                                        "offline územie (limit dlaždíc: $limit)."
                                }
                            }
                        }
                    )

                    region.setDownloadState(
                        OfflineRegion.STATE_ACTIVE
                    )
                }

                override fun onError(
                    error: String
                ) {
                    runOnUiThread {
                        progress.visibility =
                            ProgressBar.GONE

                        downloadButton.isEnabled =
                            true

                        status.text =
                            "❌ Nepodarilo sa vytvoriť offline mapu: $error"
                    }
                }
            }
        )
    }

    private fun downloadRoutingData(
        countryName: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double
    ) {
        if (routingDownloadStarted) {
            return
        }

        routingDownloadStarted = true
        progress.progress = 0

        val fileCount =
            OfflineRoutingManager.requiredSegments(
                west,
                south,
                east,
                north
            ).size

        status.text =
            " $countryName: sťahujem $fileCount " +
            "routovacie súbory..."

        thread {
            try {
                var lastPercent = -1

                OfflineRoutingManager.downloadForBounds(
                    applicationContext,
                    west,
                    south,
                    east,
                    north
                ) { routingProgress ->
                    val percent = routingProgress.percent

                    if (percent != lastPercent) {
                        lastPercent = percent

                        runOnUiThread {
                            progress.progress = percent
                            status.text =
                                " Offline navigácia: $percent %\n" +
                                routingProgress.fileName +
                                "  (${routingProgress.fileIndex + 1}/" +
                                "${routingProgress.fileCount})"
                        }
                    }
                }

                runOnUiThread {
                    progress.progress = 100
                    status.text =
                        " $countryName je kompletne offline.\n" +
                        "Mapa aj navigácia sú pripravené."
                    downloadButton.text =
                        " Mapa + navigácia uložené"
                    downloadButton.isEnabled = false
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    routingDownloadStarted = false
                    progress.visibility = ProgressBar.GONE
                    downloadButton.isEnabled = true
                    downloadButton.text =
                        " Skúsiť navigáciu znova"
                    downloadButton.setOnClickListener {
                        downloadButton.isEnabled = false
                        progress.visibility = ProgressBar.VISIBLE
                        downloadRoutingData(
                            countryName,
                            west,
                            south,
                            east,
                            north
                        )
                    }
                    status.text =
                        "Mapa je uložená, ale routovacie dáta " +
                        "sa nestiahli.\n" +
                        (exception.message ?: "Neznáma chyba")
                }
            }
        }
    }
}
