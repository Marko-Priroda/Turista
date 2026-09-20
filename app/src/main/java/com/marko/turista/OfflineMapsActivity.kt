package com.marko.turista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

class OfflineMapsActivity : AppCompatActivity() {

    private lateinit var countryList: LinearLayout
    private lateinit var offlineList: LinearLayout

    /*
     * MUSÍ byť rovnaký style URL ako v MapActivity.
     *
     * Dôležité:
     * MapLibre podľa tejto adresy identifikuje štýl,
     * pre ktorý boli uložené offline zdroje.
     */
    private val styleUrl =
        "https://tiles.openfreemap.org/styles/liberty"

    private val offlineManager: OfflineManager by lazy {
        OfflineManager.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Inicializácia MapLibre musí prebehnúť pred použitím
         * OfflineManager.
         */
        MapLibre.getInstance(this)

        createInterface()
    }

    private fun createInterface() {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(OfflineUi.cream);setPadding(OfflineUi.dp(this@OfflineMapsActivity,18),0,OfflineUi.dp(this@OfflineMapsActivity,18),0)}
        root.addView(OfflineUi.back(this){finish()})
        root.addView(OfflineUi.heading(this,"Offline mapy"))
        root.addView(TextView(this).apply{text="Priprav sa na cestu bez signálu.";textSize=16f;setTextColor(OfflineUi.ink);setPadding(0,0,0,OfflineUi.dp(this@OfflineMapsActivity,18))})
        val search=OfflineUi.search(this,"Hľadať krajinu…")
        root.addView(search,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=OfflineUi.dp(this@OfflineMapsActivity,16)})
        root.addView(TextView(this).apply{text="VYBER OBLASŤ";textSize=12f;letterSpacing=0.12f;setTextColor(OfflineUi.ink);setPadding(0,0,0,OfflineUi.dp(this@OfflineMapsActivity,10))})
        countryList=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,0,0,OfflineUi.dp(this@OfflineMapsActivity,24))}
        root.addView(ScrollView(this).apply{addView(countryList);isFillViewport=true},LinearLayout.LayoutParams(-1,0,1f))
        offlineList=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val selectedCountry=intent.getStringExtra("country")
        if(selectedCountry.isNullOrBlank())showContinents() else showSelectedCountryDownload(selectedCountry)
        search.addTextChangedListener(object:android.text.TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){searchCountries(s?.toString()?:"")}
            override fun afterTextChanged(s:android.text.Editable?){}
        })
        setContentView(root);ScreenInsets.applyTo(root)
    }

    private fun addSectionTitle(
        parent: LinearLayout,
        text: String
    ) {

        val title =
            TextView(this)

        title.text =
            text

        title.textSize =
            21f

        title.setTextColor(
            Color.rgb(38, 50, 56)
        )

        title.setPadding(
            5,
            25,
            5,
            10
        )

        parent.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addDescription(
        parent: LinearLayout,
        text: String
    ) {

        val description =
            TextView(this)

        description.text =
            text

        description.textSize =
            16f

        description.setTextColor(
            Color.DKGRAY
        )

        description.setPadding(
            5,
            0,
            5,
            15
        )

        parent.addView(
            description,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    // ============================================================
    // RÝCHLA OFFLINE MAPA
    // ============================================================

    private fun addQuickMapButton(
        parent: LinearLayout,
        titleText: String,
        descriptionText: String,
        regionName: String,
        displayName: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        minZoom: Double,
        maxZoom: Double
    ) {

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.setBackgroundColor(
            Color.WHITE
        )

        container.setPadding(
            20,
            15,
            20,
            15
        )

        val title =
            TextView(this)

        title.text =
            titleText

        title.textSize =
            20f

        title.setTextColor(
            Color.rgb(38, 50, 56)
        )

        container.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val description =
            TextView(this)

        description.text =
            descriptionText

        description.textSize =
            15f

        description.setTextColor(
            Color.DKGRAY
        )

        container.addView(
            description,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val button =
            Button(this)

        button.text =
            "⬇️  Stiahnuť mapu"

        button.setOnClickListener {

            button.isEnabled =
                false

            button.text =
                "⏳  Pripravujem..."

            downloadRegion(
                regionName = regionName,
                displayName = displayName,
                west = west,
                south = south,
                east = east,
                north = north,
                minZoom = minZoom,
                maxZoom = maxZoom,
                onFinished = {

                    runOnUiThread {

                        button.isEnabled =
                            true

                        button.text =
                            "⬇️  Stiahnuť mapu"

                        refreshOfflineRegions()
                    }
                }
            )
        }

        container.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            0,
            0,
            15
        )

        parent.addView(
            container,
            params
        )
    }

    // ============================================================
    // STIAHNUTIE
    // ============================================================

    private fun downloadRegion(
        regionName: String,
        displayName: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        minZoom: Double,
        maxZoom: Double,
        onFinished: () -> Unit
    ) {

        val bounds =
            LatLngBounds.Builder()
                .include(
                    LatLng(
                        south,
                        west
                    )
                )
                .include(
                    LatLng(
                        north,
                        east
                    )
                )
                .build()

        /*
         * Ukladáme aj hranice oblasti.
         *
         * Neskôr ich použijeme pri tlačidle
         * "Otvoriť offline mapu".
         */
        val metadataText =
            """
            {
              "name":"$regionName",
              "displayName":"$displayName",
              "west":$west,
              "south":$south,
              "east":$east,
              "north":$north,
              "minZoom":$minZoom,
              "maxZoom":$maxZoom
            }
            """.trimIndent()

        val metadata =
            metadataText.toByteArray(
                Charsets.UTF_8
            )

        val definition =
            OfflineTilePyramidRegionDefinition(
                styleUrl,
                bounds,
                minZoom,
                maxZoom,
                resources.displayMetrics.density
            )

        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object :
                OfflineManager.CreateOfflineRegionCallback {

                override fun onCreate(
                    offlineRegion: OfflineRegion
                ) {

                    offlineRegion.setObserver(
                        object :
                            OfflineRegion.OfflineRegionObserver {

                            override fun onStatusChanged(
                                status: OfflineRegionStatus
                            ) {

                                val total =
                                    status.requiredResourceCount

                                val completed =
                                    status.completedResourceCount

                                val percent =
                                    if (total > 0L) {

                                        (
                                            completed.toDouble() /
                                                total.toDouble() *
                                                100.0
                                            )
                                            .toInt()
                                            .coerceIn(
                                                0,
                                                100
                                            )

                                    } else {
                                        0
                                    }

                                runOnUiThread {

                                    updateDownloadProgress(
                                        displayName,
                                        percent,
                                        completed,
                                        total
                                    )
                                }

                                if (
                                    total > 0L &&
                                    completed >= total
                                ) {

                                    offlineRegion.setDownloadState(
                                        OfflineRegion.STATE_INACTIVE
                                    )

                                    runOnUiThread {

                                        updateDownloadProgress(
                                            displayName,
                                            100,
                                            total,
                                            total
                                        )

                                        onFinished()
                                    }
                                }
                            }

                            override fun onError(
                                error: OfflineRegionError
                            ) {

                                runOnUiThread {

                                    showDownloadError(
                                        displayName,
                                        error.message
                                            ?: "Neznáma chyba"
                                    )

                                    onFinished()
                                }
                            }

                            override fun mapboxTileCountLimitExceeded(
                                limit: Long
                            ) {

                                runOnUiThread {

                                    showDownloadError(
                                        displayName,
                                        "Mapa prekročila limit dlaždíc: $limit"
                                    )

                                    onFinished()
                                }
                            }
                        }
                    )

                    offlineRegion.setDownloadState(
                        OfflineRegion.STATE_ACTIVE
                    )
                }

                override fun onError(
                    error: String
                ) {

                    runOnUiThread {

                        showDownloadError(
                            displayName,
                            error
                        )

                        onFinished()
                    }
                }
            }
        )
    }

    // ============================================================
    // PROGRESS
    // ============================================================

    private fun updateDownloadProgress(
        displayName: String,
        percent: Int,
        completed: Long,
        total: Long
    ) {

        val existing =
            offlineList.findViewWithTag<View>(
                "progress_$displayName"
            )

        if (existing != null) {

            val progress =
                existing as? ProgressBar

            if (progress != null) {

                progress.progress =
                    percent
            }

            val parent =
                existing.parent as? LinearLayout

            if (parent != null) {

                val status =
                    parent.findViewWithTag<TextView>(
                        "status_$displayName"
                    )

                status?.text =
                    if (total > 0L) {

                        "⏳ $percent %   ($completed / $total)"

                    } else {

                        "⏳ Pripravujem mapu..."
                    }
            }

            return
        }

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.setBackgroundColor(
            Color.WHITE
        )

        container.setPadding(
            20,
            15,
            20,
            15
        )

        val title =
            TextView(this)

        title.text =
            "📥  $displayName"

        title.textSize =
            18f

        title.setTextColor(
            Color.rgb(38, 50, 56)
        )

        container.addView(
            title
        )

        val progress =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            )

        progress.max =
            100

        progress.progress =
            percent

        progress.tag =
            "progress_$displayName"

        container.addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val status =
            TextView(this)

        status.tag =
            "status_$displayName"

        status.text =
            if (total > 0L) {

                "⏳ $percent %   ($completed / $total)"

            } else {

                "⏳ Pripravujem mapu..."
            }

        status.textSize =
            14f

        status.setTextColor(
            Color.DKGRAY
        )

        container.addView(
            status
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            0,
            0,
            10
        )

        offlineList.addView(
            container,
            params
        )
    }

    // ============================================================
    // CHYBA
    // ============================================================

    private fun showDownloadError(
        displayName: String,
        message: String
    ) {

        val error =
            TextView(this)

        error.text =
            "❌ $displayName\n$message"

        error.textSize =
            15f

        error.setTextColor(
            Color.rgb(180, 30, 30)
        )

        error.setPadding(
            10,
            10,
            10,
            10
        )

        offlineList.addView(
            error
        )
    }

    // ============================================================
    // NAČÍTANIE OFFLINE MÁP
    // ============================================================

    private fun refreshOfflineRegions() {

        offlineManager.listOfflineRegions(
            object :
                OfflineManager.ListOfflineRegionsCallback {

                override fun onList(
                    regions: Array<OfflineRegion>?
                ) {

                    runOnUiThread {

                        offlineList.removeAllViews()

                        if (
                            regions == null ||
                            regions.isEmpty()
                        ) {

                            val empty =
                                TextView(
                                    this@OfflineMapsActivity
                                )

                            empty.text =
                                "📭  Zatiaľ nemáš stiahnuté žiadne offline mapy."

                            empty.textSize =
                                16f

                            empty.setTextColor(
                                Color.DKGRAY
                            )

                            empty.setPadding(
                                10,
                                10,
                                10,
                                20
                            )

                            offlineList.addView(
                                empty
                            )

                            return@runOnUiThread
                        }

                        for (
                            region in regions
                        ) {

                            showOfflineRegion(
                                region
                            )
                        }
                    }
                }

                override fun onError(
                    error: String
                ) {

                    runOnUiThread {

                        val errorText =
                            TextView(
                                this@OfflineMapsActivity
                            )

                        errorText.text =
                            "❌ Nepodarilo sa načítať offline mapy:\n$error"

                        errorText.textSize =
                            16f

                        errorText.setTextColor(
                            Color.rgb(
                                180,
                                30,
                                30
                            )
                        )

                        errorText.setPadding(
                            10,
                            10,
                            10,
                            20
                        )

                        offlineList.addView(
                            errorText
                        )
                    }
                }
            }
        )
    }

    // ============================================================
    // ZOBRAZENIE JEDNEJ OFFLINE MAPY
    // ============================================================

    private fun showOfflineRegion(
        region: OfflineRegion
    ) {

        region.getStatus(
            object :
                OfflineRegion.OfflineRegionStatusCallback {

                override fun onStatus(
                    statusValue: OfflineRegionStatus?
                ) {

                    if (statusValue == null) {
                        return
                    }

                    runOnUiThread {

                        val container =
                            LinearLayout(
                                this@OfflineMapsActivity
                            )

                        container.orientation =
                            LinearLayout.VERTICAL

                        container.setBackgroundColor(
                            Color.WHITE
                        )

                        container.setPadding(
                            20,
                            15,
                            20,
                            15
                        )

                        // ------------------------------------------------
                        // NÁZOV
                        // ------------------------------------------------

                        val name =
                            TextView(
                                this@OfflineMapsActivity
                            )

                        name.text =
                            "🗺️  ${getRegionDisplayName(region)}"

                        name.textSize =
                            19f

                        name.setTextColor(
                            Color.rgb(
                                38,
                                50,
                                56
                            )
                        )

                        container.addView(
                            name
                        )

                        val completed =
                            statusValue.completedResourceCount

                        val total =
                            statusValue.requiredResourceCount

                        // ------------------------------------------------
                        // STAV
                        // ------------------------------------------------

                        val progressText =
                            TextView(
                                this@OfflineMapsActivity
                            )

                        val isComplete =
                            total > 0L &&
                                completed >= total

                        progressText.text =
                            if (isComplete) {

                                "✅ Mapa je pripravená offline"

                            } else if (total > 0L) {

                                "⏳ $completed / $total zdrojov"

                            } else {

                                "⏳ Stav mapy sa zisťuje..."
                            }

                        progressText.textSize =
                            15f

                        progressText.setTextColor(
                            Color.DKGRAY
                        )

                        container.addView(
                            progressText
                        )

                        // ------------------------------------------------
                        // OTVORIŤ OFFLINE
                        // ------------------------------------------------

                        if (isComplete) {

                            val openButton =
                                Button(
                                    this@OfflineMapsActivity
                                )

                            openButton.text =
                                "🗺️  Otvoriť offline mapu"

                            openButton.setOnClickListener {

                                openOfflineRegion(
                                    region
                                )
                            }

                            container.addView(
                                openButton
                            )
                        }

                        // ------------------------------------------------
                        // VYMAZAŤ
                        // ------------------------------------------------

                        val deleteButton =
                            Button(
                                this@OfflineMapsActivity
                            )

                        deleteButton.text =
                            "🗑️  Vymazať mapu"

                        deleteButton.setOnClickListener {

                            deleteButton.isEnabled =
                                false

                            region.delete(
                                object :
                                    OfflineRegion.OfflineRegionDeleteCallback {

                                    override fun onDelete() {

                                        runOnUiThread {

                                            refreshOfflineRegions()
                                        }
                                    }

                                    override fun onError(
                                        error: String
                                    ) {

                                        runOnUiThread {

                                            deleteButton.isEnabled =
                                                true

                                            showDownloadError(
                                                getRegionDisplayName(
                                                    region
                                                ),
                                                error
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        container.addView(
                            deleteButton
                        )

                        val params =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )

                        params.setMargins(
                            0,
                            0,
                            0,
                            10
                        )

                        offlineList.addView(
                            container,
                            params
                        )
                    }
                }

                override fun onError(
                    error: String?
                ) {

                    runOnUiThread {

                        showDownloadError(
                            "Offline mapa",
                            error
                                ?: "Neznáma chyba"
                        )
                    }
                }
            }
        )
    }

    // ============================================================
    // OTVORENIE OFFLINE MAPY
    // ============================================================

    private fun openOfflineRegion(
        region: OfflineRegion
    ) {

        try {

            val metadata =
                String(
                    region.metadata,
                    Charsets.UTF_8
                )

            val west =
                getDoubleFromMetadata(
                    metadata,
                    "west"
                )

            val south =
                getDoubleFromMetadata(
                    metadata,
                    "south"
                )

            val east =
                getDoubleFromMetadata(
                    metadata,
                    "east"
                )

            val north =
                getDoubleFromMetadata(
                    metadata,
                    "north"
                )

            val intent =
                Intent(
                    this,
                    MapActivity::class.java
                )

            /*
             * MapActivity dostane informáciu,
             * že používateľ otvoril uloženú offline mapu.
             */
            intent.putExtra(
                "offline_mode",
                true
            )

            intent.putExtra(
                "offline_region_id",
                region.id
            )

            intent.putExtra(
                "offline_style_url",
                styleUrl
            )

            intent.putExtra(
                "offline_west",
                west
            )

            intent.putExtra(
                "offline_south",
                south
            )

            intent.putExtra(
                "offline_east",
                east
            )

            intent.putExtra(
                "offline_north",
                north
            )

            startActivity(
                intent
            )

        } catch (
            exception: Exception
        ) {

            showDownloadError(
                "Offline mapa",
                "Nepodarilo sa otvoriť offline mapu: ${exception.message}"
            )
        }
    }

    // ============================================================
    // METADATA
    // ============================================================

    private fun getDoubleFromMetadata(
        metadata: String,
        key: String
    ): Double {

        return try {

            val regex =
                Regex(
                    "\"$key\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)"
                )

            val match =
                regex.find(
                    metadata
                )

            match
                ?.groupValues
                ?.getOrNull(1)
                ?.toDouble()
                ?: 0.0

        } catch (
            exception: Exception
        ) {

            0.0
        }
    }

    private fun getRegionDisplayName(
        region: OfflineRegion
    ): String {

        return try {

            val metadata =
                String(
                    region.metadata,
                    Charsets.UTF_8
                )

            val marker =
                "\"displayName\":\""

            val start =
                metadata.indexOf(
                    marker
                )

            if (start >= 0) {

                val valueStart =
                    start + marker.length

                val end =
                    metadata.indexOf(
                        "\"",
                        valueStart
                    )

                if (end > valueStart) {

                    metadata.substring(
                        valueStart,
                        end
                    )

                } else {

                    "Offline mapa"
                }

            } else {

                "Offline mapa"
            }

        } catch (
            exception: Exception
        ) {

            "Offline mapa"
        }
    }

    // ============================================================
    // KRAJINY
    // ============================================================

    private fun showSelectedCountryDownload(countryName: String) {

        countryList.removeAllViews()

        val country = CountryData.countries.firstOrNull {
            it.name.equals(countryName, ignoreCase = true)
        }

        if (country == null) {
            showContinents()
            return
        }

        addDescription(
            countryList,
            "Stiahne sa mapa celého štátu. Zobrazenie bude dostupné bez internetu. Pre skutočné offline routovanie musí byť ku krajine dostupný aj lokálny routovací graf."
        )

        val title = TextView(this)
        title.text = "${country.flag}  ${country.name}"
        title.textSize = 22f
        title.setTextColor(Color.rgb(38, 50, 56))
        title.setPadding(5, 5, 5, 12)
        countryList.addView(title)

        addCountryMapDownloadButton(
            countryList,
            country.name
        )
    }

    private fun addCountryMapDownloadButton(
        parent: LinearLayout,
        countryName: String
    ) {
        val button = Button(this)
        button.text = "⬇️  Stiahnuť celý štát: $countryName"
        button.setOnClickListener {
            val country = CountryData.countries.firstOrNull {
                it.name.equals(countryName, ignoreCase = true)
            }

            val intent = Intent(
                this,
                CountryActivity::class.java
            )

            intent.putExtra("country", countryName)
            intent.putExtra("flag", country?.flag ?: "🌍")
            startActivity(intent)
        }
        parent.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    override fun onResume() {
        super.onResume()

        if (::offlineList.isInitialized) {
            refreshOfflineRegions()
        }
    }

    private fun showContinents() {

        countryList.removeAllViews()

        addContinent(
            "Európa"
        )

        addContinent(
            "Ázia"
        )

        addContinent(
            "Afrika"
        )

        addContinent(
            "Severná Amerika"
        )

        addContinent(
            "Južná Amerika"
        )

        addContinent(
            "Oceánia"
        )

        addContinent(
            "Antarktída"
        )
    }

    private fun searchCountries(
        query: String
    ) {

        val text =
            query.trim()

        if (text.isEmpty()) {

            showContinents()

            return
        }

        countryList.removeAllViews()

        val results =
            CountryData.countries.filter {

                it.name.contains(
                    text,
                    ignoreCase = true
                )
            }

        for (
            country in results
        ) {

            addCountryResult(
                country
            )
        }

        if (results.isEmpty()) {

            val empty =
                TextView(this)

            empty.text =
                "🔎  Štát sa nenašiel"

            empty.textSize =
                18f

            empty.setTextColor(
                Color.DKGRAY
            )

            empty.gravity =
                Gravity.CENTER

            empty.setPadding(
                20,
                40,
                20,
                40
            )

            countryList.addView(
                empty
            )
        }
    }

    private fun addCountryResult(country:Country) {
        OfflineUi.row(countryList,country.name,country.continent+" · mapa a navigácia") {
            startActivity(Intent(this,CountryActivity::class.java).putExtra("country",country.name).putExtra("flag",country.flag))
        }
    }
    private fun addContinent(name:String) {
        val count=CountryData.countries.count{it.continent==name}
        OfflineUi.row(countryList,name,"Krajiny a oblasti: $count") {
            startActivity(Intent(this,CountryListActivity::class.java).putExtra("continent",name))
        }
    }

    private fun getContinentEmoji(
        name: String
    ): String {

        return when (name) {

            "Európa" ->
                "🌍"

            "Ázia" ->
                "🌏"

            "Afrika" ->
                "🌍"

            "Severná Amerika" ->
                "🌎"

            "Južná Amerika" ->
                "🌎"

            "Oceánia" ->
                "🏝️"

            "Antarktída" ->
                "🧊"

            else ->
                "🌍"
        }
    }
}
