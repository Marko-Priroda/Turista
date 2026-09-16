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
        main.setBackgroundColor(Color.rgb(221, 232, 213))
        main.setPadding(24, 45, 24, 24)

        val backButton = TextView(this)
        backButton.text = "←  Späť"
        backButton.textSize = 20f
        backButton.setTextColor(Color.rgb(38, 50, 56))
        backButton.setPadding(10, 15, 10, 15)
        backButton.setOnClickListener { finish() }
        main.addView(backButton)

        val title = TextView(this)
        title.text = "$countryFlag  $countryName"
        title.textSize = 28f
        title.setTextColor(Color.rgb(38, 50, 56))
        title.gravity = Gravity.CENTER
        title.setPadding(0, 20, 0, 25)
        main.addView(title)

        status = TextView(this)
        status.text = "ℹ️  Stiahne sa mapa celého štátu."
        status.textSize = 17f
        status.setTextColor(Color.rgb(38, 50, 56))
        status.setPadding(10, 10, 10, 20)
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
        downloadButton.text = "📥  Stiahnuť celý štát"
        downloadButton.setOnClickListener {
            downloadButton.isEnabled = false
            progress.visibility = ProgressBar.VISIBLE
            status.text = "🔎  Hľadám hranice štátu..."
            findCountryBounds(countryName, countryFlag)
        }
        main.addView(downloadButton)

        val note = TextView(this)
        note.text =
            "\n📌 Mapa celého štátu sa ukladá v základnom zoome 6–12. " +
            "Pre podrobnú turistiku budeme používať menšie oblasti s vyšším zoomom."
        note.textSize = 15f
        note.setTextColor(Color.DKGRAY)
        main.addView(note)

        val info = TextView(this)
        info.text =
            "\nℹ️ Offline mapa obsahuje mapové dlaždice. " +
            "Samotné offline vedenie po cestách a chodníkoch potrebuje " +
            "navyše routingové dáta; MapLibre offline dlaždice ich neposkytujú."
        info.textSize = 16f
        info.setTextColor(Color.DKGRAY)
        main.addView(info)

        setContentView(main)
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
                put("maxZoom", 12.0)
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
                12.0,
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
                                        "📥  $countryName\n" +
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
                                            "✅  $countryName je uložené offline."

                                        downloadButton.text =
                                            "✅  Mapa uložená"

                                        downloadButton.isEnabled =
                                            false
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
}
