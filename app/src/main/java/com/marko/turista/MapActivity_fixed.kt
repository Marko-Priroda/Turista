package com.marko.turista

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sqrt


class MapActivity :
    AppCompatActivity(),
    SensorEventListener {

    companion object {

        const val MAP_STYLE_URL =
            "https://tiles.openfreemap.org/styles/liberty"
    }

    // ====================================
    // MAPA
    // ====================================

    private lateinit var mapView: MapView

    private var map: MapLibreMap? =
        null

    private var searchMarkerSource: GeoJsonSource? =
        null

    // ====================================
    // OFFLINE MAPA
    // ====================================

    private val offlineManager: OfflineManager by lazy {
        OfflineManager.getInstance(this)
    }

    private var offlineMode =
        false

    private var offlineRegionId =
        -1L

    private var offlineWest =
        0.0

    private var offlineSouth =
        0.0

    private var offlineEast =
        0.0

    private var offlineNorth =
        0.0

    // ====================================
    // VYBRANÝ CIEĽ
    // ====================================

    private var targetLocationSource: GeoJsonSource? =
        null

    private var selectedLatitude: Double? =
        null

    private var selectedLongitude: Double? =
        null

    private lateinit var targetPanel: View

    private lateinit var targetCoordinates: TextView

    private lateinit var targetDistance: TextView

    // ====================================
    // NAVIGÁCIA
    // ====================================

    private var routeSource: GeoJsonSource? =
        null

    private var offRoadSource: GeoJsonSource? =
        null

    private var navigationActive =
        false

    private var currentRouteDistanceMeters =
        0.0

    private var currentRouteDurationSeconds =
        0.0

    private val routeGeometryPoints =
        mutableListOf<LatLng>()

    // ====================================
    // OFF-ROAD GEOMETRIA
    // ====================================

    private val offRoadGeometryPoints =
        mutableListOf<LatLng>()

    private var offRoadDistanceMeters =
        0.0

    // ====================================
    // ÚPRAVA TRASY
    // ====================================

    private val routeViaPoints =
        mutableListOf<LatLng>()

    private var routeEditMode =
        false

    private var savedViaPoints =
        mutableListOf<LatLng>()

    private var routeViaSource: GeoJsonSource? =
        null

    // ====================================
    // NAVIGAČNÝ PANEL
    // ====================================

    private var navigationPanel: View? =
        null

    private var navigationInstruction: TextView? =
        null

    private var navigationInstructionDistance: TextView? =
        null

    private var navigationRemaining: TextView? =
        null

    // ====================================
    // PANEL ÚPRAVY TRASY
    // ====================================

    private var routeEditPanel: View? =
        null

    private var routeEditInfo: TextView? =
        null

    // ====================================
    // HYBRIDNÁ NAVIGÁCIA
    // ====================================

    private var hybridRouteActive =
        false

    private var offRoadNavigationActive =
        false

    private var roadEndLatitude: Double? =
        null

    private var roadEndLongitude: Double? =
        null

    private var offRoadStage =
        0

    private var offRoadLastDistance =
        Double.MAX_VALUE

    private enum class NavigationMode {
        WALK,
        CAR,
        BIKE
    }

    private var navigationMode =
        NavigationMode.WALK

    private data class NavigationStep(
        val latitude: Double,
        val longitude: Double,
        val distance: Double,
        val duration: Double,
        val type: String,
        val modifier: String,
        val name: String
    )

    private val navigationSteps =
        mutableListOf<NavigationStep>()

    private var currentNavigationStep =
        0

    private var navigationStage =
        0

    private var arrivalSpoken =
        false

    // ====================================
    // HLASOVÁ NAVIGÁCIA
    // ====================================

    private var textToSpeech: TextToSpeech? =
        null

    private var textToSpeechReady =
        false

    // ====================================
    // GPS
    // ====================================

    private var locationSource: GeoJsonSource? =
        null

    private var locationLayer: SymbolLayer? =
        null

    private lateinit var locationManager: LocationManager

    private var lastLocation: Location? =
        null

    private var firstGpsLocation =
        true

    private var trackingLocation =
        false

    private var gpsAnimator: ValueAnimator? =
        null

    // ====================================
    // KOMPAS
    // ====================================

    private lateinit var sensorManager: SensorManager

    private var rotationSensor: Sensor? =
        null

    private lateinit var compassView: CompassView

    private var currentAzimuth =
        0f

    // ====================================
    // GPS OPRÁVNENIE
    // ====================================

    private val LOCATION_REQUEST_CODE =
        1001

    // ====================================
    // GPS LISTENER
    // ====================================

    private val locationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {
                updateMyLocation(location)
            }

            override fun onProviderEnabled(
                provider: String
            ) {
            }

            override fun onProviderDisabled(
                provider: String
            ) {

                Toast.makeText(
                    this@MapActivity,
                    "📡 GPS je vypnuté",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ====================================
    // ON CREATE
    // ====================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        MapLibre.getInstance(this)

        setContentView(
            R.layout.activity_map
        )

        mapView =
            findViewById(
                R.id.mapView
            )

        locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        // ====================================
        // TEXT TO SPEECH
        // ====================================

        textToSpeech =
            TextToSpeech(
                this
            ) { status ->

                if (
                    status ==
                    TextToSpeech.SUCCESS
                ) {

                    val result =
                        textToSpeech?.setLanguage(
                            Locale(
                                "sk",
                                "SK"
                            )
                        )

                    textToSpeechReady =
                        result !=
                        TextToSpeech.LANG_MISSING_DATA &&
                        result !=
                        TextToSpeech.LANG_NOT_SUPPORTED
                }
            }

        // ====================================
        // CIEĽOVÝ PANEL
        // ====================================

        targetPanel =
            findViewById(
                R.id.targetPanel
            )

        targetCoordinates =
            findViewById(
                R.id.targetCoordinates
            )

        targetDistance =
            findViewById(
                R.id.targetDistance
            )

        val navigateButton =
            findViewById<Button>(
                R.id.navigateButton
            )

        val cancelTargetButton =
            findViewById<Button>(
                R.id.cancelTargetButton
            )

        navigateButton.setOnClickListener {

            showNavigationModeDialog()
        }

        cancelTargetButton.setOnClickListener {

            if (
                navigationActive
            ) {

                AlertDialog.Builder(this)
                    .setTitle(
                        "Zrušiť navigáciu?"
                    )
                    .setMessage(
                        "Naozaj chceš ukončiť navigáciu?"
                    )
                    .setPositiveButton(
                        "Áno"
                    ) { _, _ ->

                        clearSelectedTarget()
                    }
                    .setNegativeButton(
                        "Nie",
                        null
                    )
                    .show()

            } else {

                clearSelectedTarget()
            }
        }

        // ====================================
        // PANELY
        // ====================================

        navigationPanel =
            findViewById(
                R.id.navigationPanel
            )

        navigationInstruction =
            findViewById(
                R.id.navigationInstruction
            )

        navigationInstructionDistance =
            findViewById(
                R.id.navigationInstructionDistance
            )

        navigationRemaining =
            findViewById(
                R.id.navigationRemaining
            )

        routeEditPanel =
            findViewById(
                R.id.routeEditPanel
            )

        routeEditInfo =
            findViewById(
                R.id.routeEditInfo
            )

        val addRoutePointButton =
            findViewById<Button>(
                R.id.addRoutePointButton
            )

        val finishRouteEditButton =
            findViewById<Button>(
                R.id.finishRouteEditButton
            )

        val cancelRouteEditButton =
            findViewById<Button>(
                R.id.cancelRouteEditButton
            )

        val editNavigationRouteButton =
            findViewById<Button>(
                R.id.editNavigationRouteButton
            )

        val stopNavigationButton =
            findViewById<Button>(
                R.id.stopNavigationButton
            )

        addRoutePointButton.setOnClickListener {

            enterRouteEditMode()
        }

        finishRouteEditButton.setOnClickListener {

            finishRouteEditing()
        }

        cancelRouteEditButton.setOnClickListener {

            cancelRouteEditing()
        }

        editNavigationRouteButton.setOnClickListener {

            enterRouteEditMode()
        }

        stopNavigationButton.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle(
                    "Ukončiť navigáciu?"
                )
                .setMessage(
                    "Naozaj chceš ukončiť navigáciu?"
                )
                .setPositiveButton(
                    "Áno"
                ) { _, _ ->

                    clearSelectedTarget()
                }
                .setNegativeButton(
                    "Nie",
                    null
                )
                .show()
        }

        // ====================================
        // PRIAME TLAČIDLÁ
        // ====================================

        val walkRouteButton =
            findViewById<Button>(
                R.id.walkRouteButton
            )

        val bikeRouteButton =
            findViewById<Button>(
                R.id.bikeRouteButton
            )

        val carRouteButton =
            findViewById<Button>(
                R.id.carRouteButton
            )

        walkRouteButton.setOnClickListener {

            navigationMode =
                NavigationMode.WALK

            startNavigationToSelectedPoint()
        }

        bikeRouteButton.setOnClickListener {

            navigationMode =
                NavigationMode.BIKE

            startNavigationToSelectedPoint()
        }

        carRouteButton.setOnClickListener {

            navigationMode =
                NavigationMode.CAR

            startNavigationToSelectedPoint()
        }

        targetPanel.visibility =
            View.GONE

        navigationPanel?.visibility =
            View.GONE

        routeEditPanel?.visibility =
            View.GONE

        // ====================================
        // MAPVIEW
        // ====================================

        mapView.onCreate(
            savedInstanceState
        )

        offlineMode =
            intent.getBooleanExtra(
                "offline_mode",
                false
            )

        offlineRegionId =
            intent.getLongExtra(
                "offline_region_id",
                -1L
            )

        offlineWest =
            intent.getDoubleExtra(
                "offline_west",
                0.0
            )

        offlineSouth =
            intent.getDoubleExtra(
                "offline_south",
                0.0
            )

        offlineEast =
            intent.getDoubleExtra(
                "offline_east",
                0.0
            )

        offlineNorth =
            intent.getDoubleExtra(
                "offline_north",
                0.0
            )

        mapView.getMapAsync { mapInstance ->

            map =
                mapInstance

            val installOffline =
                offlineMode &&
                offlineRegionId >= 0L

            if (
                installOffline
            ) {

                offlineManager.getOfflineRegion(
                    offlineRegionId,
                    object :
                        OfflineManager.GetOfflineRegionCallback {

                        override fun onRegion(
                            offlineRegion:
                                org.maplibre.android.offline.OfflineRegion
                        ) {

                            mapInstance.setOfflineRegionDefinition(
                                offlineRegion.definition
                            ) {

                                initializeMapLayersAndInteraction(
                                    mapInstance
                                )
                            }
                        }
override fun onRegionNotFound() {

    mapInstance.setStyle(
        MAP_STYLE_URL
    ) {

        initializeMapLayersAndInteraction(
            mapInstance
        )

        Toast.makeText(
            this@MapActivity,
            "⚠️ Offline mapa sa nenašla.",
            Toast.LENGTH_LONG
        ).show()
    }
}
                        override fun onError(
                            error: String
                        ) {

                            mapInstance.setStyle(
                                MAP_STYLE_URL
                            ) {

                                initializeMapLayersAndInteraction(
                                    mapInstance
                                )

                                Toast.makeText(
                                    this@MapActivity,
                                    "⚠️ Offline mapa sa nenačítala: $error",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )

            } else {

                mapInstance.setStyle(
                    MAP_STYLE_URL
                ) {

                    initializeMapLayersAndInteraction(
                        mapInstance
                    )
                }
            }
        }
    }

    // ====================================
    // INICIALIZÁCIA MAPY
    // ====================================

    private fun initializeMapLayersAndInteraction(
        mapInstance: MapLibreMap
    ) {

        mapInstance.cameraPosition =
            CameraPosition.Builder()
                .target(
                    LatLng(
                        48.7,
                        19.7
                    )
                )
                .zoom(6.0)
                .build()

        // ====================================
        // SEARCH MARKER
        // ====================================

        val searchSource =
            GeoJsonSource(
                "search-marker-source"
            )

        mapInstance.style?.addSource(
            searchSource
        )

        searchMarkerSource =
            searchSource

        val searchLayer =
            SymbolLayer(
                "search-marker-layer",
                "search-marker-source"
            )

        searchLayer.withProperties(
            PropertyFactory.iconImage(
                "marker-15"
            ),
            PropertyFactory.iconSize(
                1.5f
            ),
            PropertyFactory.iconAllowOverlap(
                true
            ),
            PropertyFactory.iconIgnorePlacement(
                true
            )
        )

        mapInstance.style?.addLayer(
            searchLayer
        )

        // ====================================
        // CIEĽ
        // ====================================

        val targetSource =
            GeoJsonSource(
                "target-location-source"
            )

        mapInstance.style?.addSource(
            targetSource
        )

        targetLocationSource =
            targetSource

        val targetLayer =
            CircleLayer(
                "target-location-layer",
                "target-location-source"
            )

        targetLayer.withProperties(
            PropertyFactory.circleRadius(
                9f
            ),
            PropertyFactory.circleColor(
                "#E53935"
            ),
            PropertyFactory.circleStrokeColor(
                "#FFFFFF"
            ),
            PropertyFactory.circleStrokeWidth(
                3f
            )
        )

        mapInstance.style?.addLayer(
            targetLayer
        )

        // ====================================
        // HLAVNÁ NAVIGAČNÁ TRASA
        // ====================================

        val route =
            GeoJsonSource(
                "navigation-route-source"
            )

        mapInstance.style?.addSource(
            route
        )

        routeSource =
            route

        val routeLayer =
            LineLayer(
                "navigation-route-layer",
                "navigation-route-source"
            )

        routeLayer.withProperties(
            PropertyFactory.lineColor(
                "#1976D2"
            ),
            PropertyFactory.lineWidth(
                6f
            ),
            PropertyFactory.lineOpacity(
                0.9f
            ),
            PropertyFactory.lineCap(
                "round"
            ),
            PropertyFactory.lineJoin(
                "round"
            )
        )

        mapInstance.style?.addLayer(
            routeLayer
        )

        // ====================================
        // OFF-ROAD TRASA
        // ====================================

        val offRoad =
            GeoJsonSource(
                "off-road-route-source"
            )

        mapInstance.style?.addSource(
            offRoad
        )

        offRoadSource =
            offRoad

        val offRoadLayer =
            LineLayer(
                "off-road-route-layer",
                "off-road-route-source"
            )

        offRoadLayer.withProperties(
            PropertyFactory.lineColor(
                "#1976D2"
            ),
            PropertyFactory.lineWidth(
                5.5f
            ),
            PropertyFactory.lineOpacity(
                0.9f
            ),
            PropertyFactory.lineCap(
                "round"
            ),
            PropertyFactory.lineJoin(
                "round"
            ),
            PropertyFactory.lineDasharray(
                arrayOf(
                    1.0f,
                    1.2f
                )
            )
        )

        mapInstance.style?.addLayer(
            offRoadLayer
        )

        // ====================================
        // MEDZIBODY
        // ====================================

        val viaSource =
            GeoJsonSource(
                "route-via-source"
            )

        mapInstance.style?.addSource(
            viaSource
        )

        routeViaSource =
            viaSource

        val viaLayer =
            CircleLayer(
                "route-via-layer",
                "route-via-source"
            )

        viaLayer.withProperties(
            PropertyFactory.circleRadius(
                7f
            ),
            PropertyFactory.circleColor(
                "#FF9800"
            ),
            PropertyFactory.circleStrokeColor(
                "#FFFFFF"
            ),
            PropertyFactory.circleStrokeWidth(
                2.5f
            )
        )

        mapInstance.style?.addLayer(
            viaLayer
        )

        // ====================================
        // GPS ŠÍPKA
        // ====================================

        val gpsArrow =
            createGpsArrowBitmap()

        mapInstance.style?.addImage(
            "turista-gps-arrow",
            gpsArrow
        )

        val gpsSource =
            GeoJsonSource(
                "gps-location-source"
            )

        mapInstance.style?.addSource(
            gpsSource
        )

        locationSource =
            gpsSource

        // ====================================
        // GPS BOD
        // ====================================

        val gpsDotLayer =
            CircleLayer(
                "gps-location-dot-layer",
                "gps-location-source"
            )

        gpsDotLayer.withProperties(
            PropertyFactory.circleRadius(
                5f
            ),
            PropertyFactory.circleColor(
                "#1976D2"
            ),
            PropertyFactory.circleStrokeColor(
                "#FFFFFF"
            ),
            PropertyFactory.circleStrokeWidth(
                2.5f
            )
        )

        mapInstance.style?.addLayer(
            gpsDotLayer
        )

        // ====================================
        // GPS ŠÍPKA
        // ====================================

        val gpsLayer =
            SymbolLayer(
                "gps-location-layer",
                "gps-location-source"
            )

        gpsLayer.withProperties(
            PropertyFactory.iconImage(
                "turista-gps-arrow"
            ),
            PropertyFactory.iconSize(
                1.15f
            ),
            PropertyFactory.iconAnchor(
                "center"
            ),
            PropertyFactory.iconAllowOverlap(
                true
            ),
            PropertyFactory.iconIgnorePlacement(
                true
            ),
            PropertyFactory.iconRotate(
                currentAzimuth
            ),
            PropertyFactory.iconPitchAlignment(
                "map"
            ),
            PropertyFactory.iconRotationAlignment(
                "map"
            )
        )

        mapInstance.style?.addLayer(
            gpsLayer
        )

        locationLayer =
            gpsLayer

        // ====================================
        // OBNOVA GPS
        // ====================================

        val savedLocation =
            lastLocation

        if (
            savedLocation != null
        ) {

            firstGpsLocation =
                false

            gpsSource.setGeoJson(
                createLocationGeoJson(
                    savedLocation.latitude,
                    savedLocation.longitude
                )
            )
        }

        // ====================================
        // KLIKNUTIE NA MAPU
        // ====================================

        mapInstance.addOnMapClickListener { point ->

            if (
                routeEditMode
            ) {

                addRouteViaPoint(
                    point
                )

            } else {

                selectDestination(
                    point
                )
            }

            true
        }

        checkLocationPermission()

        if (
            lastLocation != null
        ) {

            val current =
                lastLocation

            if (
                current != null
            ) {

                locationSource?.setGeoJson(
                    createLocationGeoJson(
                        current.latitude,
                        current.longitude
                    )
                )

                mapInstance.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            current.latitude,
                            current.longitude
                        ),
                        15.0
                    )
                )
            }
        }

        // ====================================
        // VYHĽADÁVANIE
        // ====================================

        val searchMap =
            findViewById<EditText>(
                R.id.searchMap
            )

        searchMap.isSingleLine =
            true

        searchMap.imeOptions =
            EditorInfo.IME_ACTION_SEARCH

        searchMap.setOnEditorActionListener {
            _,
            actionId,
            event ->

            val searchText =
                searchMap.text
                    .toString()
                    .trim()

            val enterPressed =
                event != null &&
                event.keyCode ==
                KeyEvent.KEYCODE_ENTER

            if (
                searchText.isNotEmpty() &&
                (
                    actionId ==
                    EditorInfo.IME_ACTION_SEARCH ||
                    actionId ==
                    EditorInfo.IME_ACTION_DONE ||
                    enterPressed
                )
            ) {

                searchPlace(
                    searchText
                )

                true

            } else {

                false
            }
        }

        // ====================================
        // ZÁLOŽNÉ VYHĽADÁVANIE PRI ENTER
        // ====================================

        searchMap.setOnKeyListener {
            _,
            keyCode,
            event ->

            if (
                keyCode ==
                KeyEvent.KEYCODE_ENTER &&
                event.action ==
                KeyEvent.ACTION_DOWN
            ) {

                val searchText =
                    searchMap.text
                        .toString()
                        .trim()

                if (
                    searchText.isNotEmpty()
                ) {

                    searchPlace(
                        searchText
                    )
                }

                true

            } else {

                false
            }
        }

        // ====================================
        // KOMPAS
        // ====================================

        val compassButton =
            findViewById<Button>(
                R.id.compassButton
            )

        val compassOverlay =
            findViewById<FrameLayout>(
                R.id.compassOverlay
            )

        compassView =
            CompassView(
                this
            )

        compassOverlay.removeAllViews()

        compassOverlay.addView(
            compassView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        compassButton.setOnClickListener {

            if (
                compassOverlay.visibility ==
                View.VISIBLE
            ) {

                compassOverlay.visibility =
                    View.GONE

            } else {

                compassOverlay.visibility =
                    View.VISIBLE
            }
        }

        // ====================================
        // MOJA POLOHA
        // ====================================

        val locationButton =
            findViewById<Button>(
                R.id.locationButton
            )

        locationButton.setOnClickListener {

            if (
                hasLocationPermission()
            ) {

                centerOnMyLocation()

            } else {

                checkLocationPermission()
            }
        }

        // ====================================
        // OFFLINE MAPY
        // ====================================

        val offlineMapsButton =
            findViewById<Button>(
                R.id.offlineMapsButton
            )

        offlineMapsButton.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    OfflineMapsActivity::class.java
                )
            )
        }

        // ====================================
        // NASTAVENIA
        // ====================================

        val settingsButton =
            findViewById<Button>(
                R.id.settingsButton
            )

        settingsButton.setOnClickListener {

            Toast.makeText(
                this,
                "⚙️ Nastavenia – pripravujeme",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ====================================
        // REŽIMY MAPY
        // ====================================

        val mapModesButton =
            findViewById<Button>(
                R.id.mapModesButton
            )

        mapModesButton.setOnClickListener {

            Toast.makeText(
                this,
                "🗺️ Režimy mapy – pripravujeme",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ====================================
        // SENZOR
        // ====================================

        sensorManager =
            getSystemService(
                SENSOR_SERVICE
            ) as SensorManager

        rotationSensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR
            )
    }

    // ====================================
    // VÝBER REŽIMU NAVIGÁCIE
    // ====================================

    private fun showNavigationModeDialog() {

        val modes =
            arrayOf(
                "🚶 Pešo",
                "🚲 Bicykel",
                "🚗 Auto"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "Vyber spôsob navigácie"
            )
            .setItems(
                modes
            ) { _, which ->

                when (which) {

                    0 -> {

                        navigationMode =
                            NavigationMode.WALK

                        startNavigationToSelectedPoint()
                    }

                    1 -> {

                        navigationMode =
                            NavigationMode.BIKE

                        startNavigationToSelectedPoint()
                    }

                    2 -> {

                        navigationMode =
                            NavigationMode.CAR

                        startNavigationToSelectedPoint()
                    }
                }
            }
            .show()
    }

    // ====================================
    // VÝBER CIEĽA
    // ====================================

    private fun selectDestination(
        point: LatLng
    ) {

        selectedLatitude =
            point.latitude

        selectedLongitude =
            point.longitude

        targetLocationSource?.setGeoJson(
            createTargetGeoJson(
                point.latitude,
                point.longitude
            )
        )

        targetCoordinates.text =
            String.format(
                Locale.US,
                "%.6f, %.6f",
                point.latitude,
                point.longitude
            )

        updateTargetDistance()

        targetPanel.visibility =
            View.VISIBLE

        Toast.makeText(
            this,
            "📍 Cieľ vybraný",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ====================================
    // ÚPRAVA TRASY
    // ====================================

    private fun enterRouteEditMode() {

        if (
            !navigationActive
        ) {

            Toast.makeText(
                this,
                "Najprv spusti navigáciu.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        routeEditMode =
            true

        savedViaPoints =
            routeViaPoints.toMutableList()

        routeEditPanel?.visibility =
            View.VISIBLE

        updateRouteEditInfo()

        Toast.makeText(
            this,
            "👆 Klikaj na mapu a pridávaj body trasy.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ====================================
    // PRIDANIE MEDZIBODU
    // ====================================

    private fun addRouteViaPoint(
        point: LatLng
    ) {

        if (
            !routeEditMode
        ) {
            return
        }

        routeViaPoints.add(
            point
        )

        updateViaPointSource()

        updateRouteEditInfo()
    }

    // ====================================
    // HĽADANIE NAJBLIŽŠIEHO MEDZIBODU
    // ====================================

    private fun findNearbyViaPoint(
        point: LatLng
    ): Int {

        var closestIndex =
            -1

        var closestDistance =
            Double.MAX_VALUE

        for (
            i in routeViaPoints.indices
        ) {

            val via =
                routeViaPoints[i]

            val distance =
                distanceBetweenCoordinates(
                    point.latitude,
                    point.longitude,
                    via.latitude,
                    via.longitude
                )

            if (
                distance < closestDistance
            ) {

                closestDistance =
                    distance

                closestIndex =
                    i
            }
        }

        return closestIndex
    }

    // ====================================
    // INFORMÁCIE O ÚPRAVE TRASY
    // ====================================

    private fun updateRouteEditInfo() {

        routeEditInfo?.text =
            if (
                routeViaPoints.isEmpty()
            ) {

                "Medzibody: 0"

            } else {

                "Medzibody: ${routeViaPoints.size}"
            }
    }

    // ====================================
    // ZOBRAZENIE MEDZIBODOV
    // ====================================

    private fun updateViaPointSource() {

        val features =
            JSONArray()

        for (
            point in routeViaPoints
        ) {

            val feature =
                JSONObject()

            feature.put(
                "type",
                "Feature"
            )

            val geometry =
                JSONObject()

            geometry.put(
                "type",
                "Point"
            )

            val coordinates =
                JSONArray()

            coordinates.put(
                point.longitude
            )

            coordinates.put(
                point.latitude
            )

            geometry.put(
                "coordinates",
                coordinates
            )

            feature.put(
                "geometry",
                geometry
            )

            features.put(
                feature
            )
        }

        val collection =
            JSONObject()

        collection.put(
            "type",
            "FeatureCollection"
        )

        collection.put(
            "features",
            features
        )

        routeViaSource?.setGeoJson(
            collection.toString()
        )
    }

    // ====================================
    // DOKONČENIE ÚPRAVY
    // ====================================

    private fun finishRouteEditing() {

        routeEditMode =
            false

        routeEditPanel?.visibility =
            View.GONE

        if (
            selectedLatitude == null ||
            selectedLongitude == null
        ) {

            return
        }

        startNavigationToSelectedPoint()
    }

    // ====================================
    // ZRUŠENIE ÚPRAVY
    // ====================================

    private fun cancelRouteEditing() {

        routeEditPointsRestore()

        routeEditMode =
            false

        routeEditPanel?.visibility =
            View.GONE

        updateViaPointSource()
    }

    private fun routeEditPointsRestore() {

        routeViaPoints.clear()

        routeViaPoints.addAll(
            savedViaPoints
        )
    }

    // ====================================
    // GEOJSON CIEĽ
    // ====================================

    private fun createTargetGeoJson(
        latitude: Double,
        longitude: Double
    ): String {

        return """
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [
                  $longitude,
                  $latitude
                ]
              },
              "properties": {}
            }
        """.trimIndent()
    }

    // ====================================
    // FORMÁT VZDIALENOSTI
    // ====================================

    private fun formatDistance(
        meters: Double
    ): String {

        return if (
            meters < 1000
        ) {

            "${meters.toInt()} m"

        } else {

            String.format(
                Locale.US,
                "%.1f km",
                meters / 1000.0
            )
        }
    }

    // ====================================
    // SPUSTENIE NAVIGÁCIE
    // ====================================

    private fun startNavigationToSelectedPoint() {

        val targetLat =
            selectedLatitude

        val targetLon =
            selectedLongitude

        if (
            targetLat == null ||
            targetLon == null
        ) {

            Toast.makeText(
                this,
                "Najprv vyber cieľ na mape.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val current =
            lastLocation

        if (
            current == null
        ) {

            Toast.makeText(
                this,
                "Čakám na GPS poloha.",
                Toast.LENGTH_SHORT
            ).show()

            checkLocationPermission()

            return
        }

        navigationActive =
            true

        hybridRouteActive =
            false

        offRoadNavigationActive =
            false

        navigationStage =
            0

        currentNavigationStep =
            0

        arrivalSpoken =
            false

        navigationSteps.clear()

        routeGeometryPoints.clear()

        offRoadGeometryPoints.clear()

        offRoadDistanceMeters =
            0.0

        navigationPanel?.visibility =
            View.VISIBLE

        targetPanel.visibility =
            View.VISIBLE

        requestRoadRoute(
            current.latitude,
            current.longitude,
            targetLat,
            targetLon
        )
    }

    // ====================================
    // CESTNÁ TRASA
    // ====================================

    private fun requestRoadRoute(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ) {

        thread {

            var connection:
                HttpURLConnection? =
                null

            try {

                val profile =
                    when (
                        navigationMode
                    ) {

                        NavigationMode.WALK ->
                            "foot"

                        NavigationMode.BIKE ->
                            "bike"

                        NavigationMode.CAR ->
                            "car"
                    }

                val waypoints =
                    mutableListOf<String>()

                waypoints.add(
                    "$startLongitude,$startLatitude"
                )

                for (
                    via in routeViaPoints
                ) {

                    waypoints.add(
                        "${via.longitude},${via.latitude}"
                    )
                }

                waypoints.add(
                    "$endLongitude,$endLatitude"
                )

                val url =
                    URL(
                        "https://routing.openstreetmap.de/" +
                        "$profile/route/v1/" +
                        "$profile/" +
                        waypoints.joinToString(";") +
                        "?overview=full" +
                        "&geometries=geojson" +
                        "&steps=true"
                    )

                connection =
                    url.openConnection()
                        as HttpURLConnection

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    15000

                connection.setRequestProperty(
                    "User-Agent",
                    "Turista/1.0"
                )

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {

                    runOnUiThread {

                        if (
                            offlineMode
                        ) {

                            Toast.makeText(
                                this,
                                "📡 Offline mapa je dostupná, ale offline routovací graf pre túto krajinu zatiaľ nie je nainštalovaný.",
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            Toast.makeText(
                                this,
                                "❌ Trasu sa nepodarilo vypočítať.",
                                Toast.LENGTH_LONG
                            ).show()

                            requestOffRoadRoute(
                                startLatitude,
                                startLongitude,
                                endLatitude,
                                endLongitude
                            )
                        }
                    }

                    return@thread
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                val json =
                    JSONObject(
                        response
                    )

                val routes =
                    json.optJSONArray(
                        "routes"
                    )

                if (
                    routes == null ||
                    routes.length() == 0
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "❌ Cesta sa nenašla.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    requestOffRoadRoute(
                        startLatitude,
                        startLongitude,
                        endLatitude,
                        endLongitude
                    )

                    return@thread
                }

                val route =
                    routes.getJSONObject(
                        0
                    )

                currentRouteDistanceMeters =
                    route.optDouble(
                        "distance",
                        0.0
                    )

                currentRouteDurationSeconds =
                    route.optDouble(
                        "duration",
                        0.0
                    )

                val geometry =
                    route.optJSONObject(
                        "geometry"
                    )

                val coordinates =
                    geometry?.optJSONArray(
                        "coordinates"
                    )

                val parsedPoints =
                    parseRouteGeometry(
                        coordinates
                    )

                val steps =
                    parseNavigationSteps(
                        route.optJSONArray(
                            "legs"
                        )
                    )

                runOnUiThread {

                    routeGeometryPoints.clear()

                    routeGeometryPoints.addAll(
                        parsedPoints
                    )

                    navigationSteps.clear()

                    navigationSteps.addAll(
                        steps
                    )

                    drawNavigationRoute()

                    updateNavigationPanel()

                    speakFirstNavigationInstruction()

                    Toast.makeText(
                        this,
                        "🧭 Trasa: ${
                            formatDistance(
                                currentRouteDistanceMeters
                            )
                        }",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (
                _: Exception
            ) {

                runOnUiThread {

                    if (
                        offlineMode
                    ) {

                        Toast.makeText(
                            this,
                            "📡 Offline mapa funguje, ale pre túto krajinu zatiaľ chýbajú lokálne routovacie dáta.",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "❌ Chyba pri výpočte trasy.",
                            Toast.LENGTH_LONG
                        ).show()

                        requestOffRoadRoute(
                            startLatitude,
                            startLongitude,
                            endLatitude,
                            endLongitude
                        )
                    }
                }

            } finally {

                connection?.disconnect()
            }
        }
    }

    // ====================================
    // OFF-ROAD TRASA
    // ====================================

    private fun requestOffRoadRoute(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ) {

        drawStraightOffRoadRoute(
            startLatitude,
            startLongitude,
            endLatitude,
            endLongitude
        )
    }

    // ====================================
    // PARSOVANIE OFF-ROAD GEOMETRIE
    // ====================================

    private fun parseOffRoadGeometry(
        coordinates: JSONArray?
    ): List<LatLng> {

        val points =
            mutableListOf<LatLng>()

        if (
            coordinates == null
        ) {

            return points
        }

        for (
            i in 0 until coordinates.length()
        ) {

            val pair =
                coordinates.optJSONArray(
                    i
                ) ?: continue

            if (
                pair.length() < 2
            ) {

                continue
            }

            points.add(
                LatLng(
                    pair.getDouble(1),
                    pair.getDouble(0)
                )
            )
        }

        return points
    }

    // ====================================
    // PRIAMA OFF-ROAD TRASA
    // ====================================

    private fun drawStraightOffRoadRoute(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ) {

        offRoadGeometryPoints.clear()

        offRoadGeometryPoints.add(
            LatLng(
                startLatitude,
                startLongitude
            )
        )

        offRoadGeometryPoints.add(
            LatLng(
                endLatitude,
                endLongitude
            )
        )

        offRoadDistanceMeters =
            distanceBetweenCoordinates(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude
            )

        hybridRouteActive =
            true

        offRoadNavigationActive =
            true

        roadEndLatitude =
            endLatitude

        roadEndLongitude =
            endLongitude

        runOnUiThread {

            val coordinates =
                JSONArray()

            for (
                point in offRoadGeometryPoints
            ) {

                val pair =
                    JSONArray()

                pair.put(
                    point.longitude
                )

                pair.put(
                    point.latitude
                )

                coordinates.put(
                    pair
                )
            }

            val geometry =
                JSONObject()

            geometry.put(
                "type",
                "LineString"
            )

            geometry.put(
                "coordinates",
                coordinates
            )

            val feature =
                JSONObject()

            feature.put(
                "type",
                "Feature"
            )

            feature.put(
                "geometry",
                geometry
            )

            val collection =
                JSONObject()

            collection.put(
                "type",
                "FeatureCollection"
            )

            val features =
                JSONArray()

            features.put(
                feature
            )

            collection.put(
                "features",
                features
            )

            offRoadSource?.setGeoJson(
                collection.toString()
            )

            updateHybridDistanceDisplay()

            updateNavigationPanel()
        }
    }

    // ====================================
    // HYBRIDNÁ VZDIALENOSŤ
    // ====================================

    private fun updateHybridDistanceDisplay() {

        val roadDistance =
            currentRouteDistanceMeters

        val total =
            roadDistance +
            offRoadDistanceMeters

        navigationRemaining?.text =
            "Zostáva: ${formatDistance(total)}"
    }

    // ====================================
    // NAVIGAČNÝ PANEL
    // ====================================

    private fun updateNavigationPanel() {

        if (
            !navigationActive
        ) {

            return
        }

        val remaining =
            calculateRemainingRouteDistance()

        navigationRemaining?.text =
            "Zostáva: ${formatDistance(remaining)}"

        if (
            navigationSteps.isNotEmpty() &&
            currentNavigationStep <
            navigationSteps.size
        ) {

            val step =
                navigationSteps[
                    currentNavigationStep
                ]

            navigationInstruction?.text =
                createNavigationInstruction(
                    step
                )

            navigationInstructionDistance?.text =
                formatDistance(
                    step.distance
                )

        } else {

            navigationInstruction?.text =
                "Pokračuj k cieľu"

            navigationInstructionDistance?.text =
                ""
        }
    }

    // ====================================
    // ZOSTÁVAJÚCA VZDIALENOSŤ
    // ====================================

    private fun calculateRemainingRouteDistance():
        Double {

        val current =
            lastLocation

        if (
            current == null
        ) {

            return currentRouteDistanceMeters
        }

        if (
            routeGeometryPoints.isEmpty()
        ) {

            return currentRouteDistanceMeters
        }

        var nearestIndex =
            0

        var nearestDistance =
            Double.MAX_VALUE

        for (
            i in routeGeometryPoints.indices
        ) {

            val point =
                routeGeometryPoints[i]

            val distance =
                distanceBetweenCoordinates(
                    current.latitude,
                    current.longitude,
                    point.latitude,
                    point.longitude
                )

            if (
                distance < nearestDistance
            ) {

                nearestDistance =
                    distance

                nearestIndex =
                    i
            }
        }

        var remaining =
            0.0

        for (
            i in nearestIndex until
            routeGeometryPoints.size - 1
        ) {

            val a =
                routeGeometryPoints[i]

            val b =
                routeGeometryPoints[
                    i + 1
                ]

            remaining +=
                distanceBetweenCoordinates(
                    a.latitude,
                    a.longitude,
                    b.latitude,
                    b.longitude
                )
        }

        return remaining
    }

    // ====================================
    // ZOSTÁVAJÚCA OFF-ROAD VZDIALENOSŤ
    // ====================================

    private fun calculateRemainingOffRoadDistance():
        Double {

        val current =
            lastLocation

        if (
            current == null
        ) {

            return offRoadDistanceMeters
        }

        val targetLat =
            roadEndLatitude

        val targetLon =
            roadEndLongitude

        if (
            targetLat == null ||
            targetLon == null
        ) {

            return offRoadDistanceMeters
        }

        return distanceBetweenCoordinates(
            current.latitude,
            current.longitude,
            targetLat,
            targetLon
        )
    }

    // ====================================
    // PARSOVANIE TRASY
    // ====================================

    private fun parseRouteGeometry(
        coordinates: JSONArray?
    ): List<LatLng> {

        val points =
            mutableListOf<LatLng>()

        if (
            coordinates == null
        ) {

            return points
        }

        for (
            i in 0 until coordinates.length()
        ) {

            val pair =
                coordinates.optJSONArray(
                    i
                ) ?: continue

            if (
                pair.length() < 2
            ) {

                continue
            }

            points.add(
                LatLng(
                    pair.getDouble(1),
                    pair.getDouble(0)
                )
            )
        }

        return points
    }

    // ====================================
    // PROJEKCIA BODU
    // ====================================

    private data class RouteProjection(
        val point: LatLng,
        val distance: Double,
        val segmentIndex: Int
    )

    private fun projectPointOntoSegment(
        point: LatLng,
        start: LatLng,
        end: LatLng
    ): RouteProjection {

        val dx =
            end.longitude -
            start.longitude

        val dy =
            end.latitude -
            start.latitude

        if (
            dx == 0.0 &&
            dy == 0.0
        ) {

            return RouteProjection(
                start,
                distanceBetweenCoordinates(
                    point.latitude,
                    point.longitude,
                    start.latitude,
                    start.longitude
                ),
                0
            )
        }

        val t =
            (
                (
                    point.longitude -
                    start.longitude
                ) * dx +
                (
                    point.latitude -
                    start.latitude
                ) * dy
            ) /
            (
                dx * dx +
                dy * dy
            )

        val clamped =
            t.coerceIn(
                0.0,
                1.0
            )

        val projected =
            LatLng(
                start.latitude +
                dy * clamped,
                start.longitude +
                dx * clamped
            )

        val distance =
            distanceBetweenCoordinates(
                point.latitude,
                point.longitude,
                projected.latitude,
                projected.longitude
            )

        return RouteProjection(
            projected,
            distance,
            0
        )
    }

    // ====================================
    // VZDIALENOSŤ K CIEĽU
    // ====================================

    private fun distanceToFinalTarget(
        location: Location
    ): Double {

        val lat =
            selectedLatitude

        val lon =
            selectedLongitude

        if (
            lat == null ||
            lon == null
        ) {

            return Double.MAX_VALUE
        }

        return distanceBetweenCoordinates(
            location.latitude,
            location.longitude,
            lat,
            lon
        )
    }

    // ====================================
    // NAVIGAČNÉ KROKY
    // ====================================

    private fun parseNavigationSteps(
        legs: JSONArray?
    ): List<NavigationStep> {

        val steps =
            mutableListOf<NavigationStep>()

        if (
            legs == null
        ) {

            return steps
        }

        for (
            legIndex in 0 until legs.length()
        ) {

            val leg =
                legs.optJSONObject(
                    legIndex
                ) ?: continue

            val legSteps =
                leg.optJSONArray(
                    "steps"
                ) ?: continue

            for (
                i in 0 until legSteps.length()
            ) {

                val step =
                    legSteps.optJSONObject(
                        i
                    ) ?: continue

                val maneuver =
                    step.optJSONObject(
                        "maneuver"
                    )

                val location =
                    maneuver?.optJSONArray(
                        "location"
                    )

                if (
                    location == null ||
                    location.length() < 2
                ) {

                    continue
                }

                steps.add(
                    NavigationStep(
                        latitude =
                            location.getDouble(1),
                        longitude =
                            location.getDouble(0),
                        distance =
                            step.optDouble(
                                "distance",
                                0.0
                            ),
                        duration =
                            step.optDouble(
                                "duration",
                                0.0
                            ),
                        type =
                            maneuver.optString(
                                "type",
                                ""
                            ),
                        modifier =
                            maneuver.optString(
                                "modifier",
                                ""
                            ),
                        name =
                            step.optString(
                                "name",
                                ""
                            )
                    )
                )
            }
        }

        return steps
    }

    // ====================================
    // OTOČENIE
    // ====================================

    private fun isTurnInstruction(
        step: NavigationStep
    ): Boolean {

        return step.type in
            listOf(
                "turn",
                "new name",
                "roundabout",
                "rotary",
                "fork",
                "merge",
                "on ramp",
                "off ramp"
            )
    }

    // ====================================
    // PRVÁ INŠTRUKCIA
    // ====================================

    private fun speakFirstNavigationInstruction() {

        if (
            navigationSteps.isEmpty()
        ) {

            return
        }

        currentNavigationStep =
            0

        val step =
            navigationSteps[0]

        val instruction =
            createNavigationInstruction(
                step
            )

        navigationInstruction?.text =
            instruction

        navigationInstructionDistance?.text =
            formatDistance(
                step.distance
            )

        speak(
            instruction
        )
    }

    // ====================================
    // VYTVORENIE INŠTRUKCIE
    // ====================================

    private fun createNavigationInstruction(
        step: NavigationStep
    ): String {

        val direction =
            when {

                step.type == "depart" ->
                    "Vyraz"

                step.type == "arrive" ->
                    "Dorazíš do cieľa"

                step.modifier.contains(
                    "left"
                ) ->
                    "Odboč doľava"

                step.modifier.contains(
                    "right"
                ) ->
                    "Odboč doprava"

                step.modifier.contains(
                    "straight"
                ) ->
                    "Pokračuj rovno"

                step.type == "roundabout" ||
                step.type == "rotary" ->
                    "Na kruhovom objazde"

                step.type == "merge" ->
                    "Zaraď sa"

                step.type == "fork" ->
                    "Na rozdvojke"

                else ->
                    "Pokračuj"
            }

        return if (
            step.name.isNotBlank()
        ) {

            "$direction na ${step.name}"

        } else {

            direction
        }
    }

    // ====================================
    // HLÁSENIE ODBOČENIA
    // ====================================

    private fun speakTurnMilestone(
        step: NavigationStep
    ) {

        if (
            !isTurnInstruction(step)
        ) {

            return
        }

        speak(
            createNavigationInstruction(
                step
            )
        )
    }

    // ====================================
    // AKTUALIZÁCIA HLASU
    // ====================================

    private fun updateNavigationVoice(
        location: Location
    ) {

        if (
            !navigationActive
        ) {

            return
        }

        if (
            navigationSteps.isEmpty()
        ) {

            return
        }

        if (
            currentNavigationStep >=
            navigationSteps.size
        ) {

            return
        }

        val step =
            navigationSteps[
                currentNavigationStep
            ]

        val distance =
            distanceBetweenCoordinates(
                location.latitude,
                location.longitude,
                step.latitude,
                step.longitude
            )

        if (
            distance < 50.0
        ) {

            speakTurnMilestone(
                step
            )

            currentNavigationStep++

            if (
                currentNavigationStep <
                navigationSteps.size
            ) {

                val next =
                    navigationSteps[
                        currentNavigationStep
                    ]

                navigationInstruction?.text =
                    createNavigationInstruction(
                        next
                    )

                navigationInstructionDistance?.text =
                    formatDistance(
                        next.distance
                    )
            }
        }

        val finalDistance =
            distanceToFinalTarget(
                location
            )

        if (
            finalDistance < 25.0 &&
            !arrivalSpoken
        ) {

            arrivalSpoken =
                true

            speak(
                "Dorazil si do cieľa."
            )

            navigationInstruction?.text =
                "🎯 Dorazil si do cieľa"

            navigationInstructionDistance?.text =
                ""
        }
    }

    // ====================================
    // HLAS
    // ====================================

    private fun speak(
        text: String
    ) {

        if (
            !textToSpeechReady
        ) {

            return
        }

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "turista_navigation"
        )
    }

    // ====================================
    // VZDIALENOSŤ MEDZI SÚRADNICAMI
    // ====================================

    private fun distanceBetweenCoordinates(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {

        val earthRadius =
            6371000.0

        val dLat =
            Math.toRadians(
                latitude2 -
                latitude1
            )

        val dLon =
            Math.toRadians(
                longitude2 -
                longitude1
            )

        val a =
            kotlin.math.sin(
                dLat / 2
            ) *
            kotlin.math.sin(
                dLat / 2
            ) +
            kotlin.math.cos(
                Math.toRadians(
                    latitude1
                )
            ) *
            kotlin.math.cos(
                Math.toRadians(
                    latitude2
                )
            ) *
            kotlin.math.sin(
                dLon / 2
            ) *
            kotlin.math.sin(
                dLon / 2
            )

        val c =
            2.0 *
            kotlin.math.atan2(
                sqrt(a),
                sqrt(1.0 - a)
            )

        return earthRadius * c
    }

    // ====================================
    // KONIEC TRASY
    // ====================================

    private fun getRouteEndPoint():
        LatLng? {

        if (
            routeGeometryPoints.isEmpty()
        ) {

            return null
        }

        return routeGeometryPoints.last()
    }

    // ====================================
    // VYKRESLENIE TRASY
    // ====================================

    private fun drawNavigationRoute() {

        if (
            routeGeometryPoints.isEmpty()
        ) {

            return
        }

        val coordinates =
            JSONArray()

        for (
            point in routeGeometryPoints
        ) {

            val pair =
                JSONArray()

            pair.put(
                point.longitude
            )

            pair.put(
                point.latitude
            )

            coordinates.put(
                pair
            )
        }

        val geometry =
            JSONObject()

        geometry.put(
            "type",
            "LineString"
        )

        geometry.put(
            "coordinates",
            coordinates
        )

        val feature =
            JSONObject()

        feature.put(
            "type",
            "Feature"
        )

        feature.put(
            "geometry",
            geometry
        )

        val features =
            JSONArray()

        features.put(
            feature
        )

        val collection =
            JSONObject()

        collection.put(
            "type",
            "FeatureCollection"
        )

        collection.put(
            "features",
            features
        )

        routeSource?.setGeoJson(
            collection.toString()
        )

        val boundsPoints =
            routeGeometryPoints

        if (
            boundsPoints.isNotEmpty()
        ) {

            val middle =
                boundsPoints[
                    boundsPoints.size / 2
                ]

            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    middle,
                    12.0
                )
            )
        }
    }

    // ====================================
    // VYMAZANIE OFF-ROAD TRASY
    // ====================================

    private fun clearOffRoadRoute() {

        offRoadGeometryPoints.clear()

        offRoadDistanceMeters =
            0.0

        offRoadSource?.setGeoJson(
            """
            {
              "type":"FeatureCollection",
              "features":[]
            }
            """.trimIndent()
        )

        offRoadNavigationActive =
            false
    }

    // ====================================
    // FORMÁT ČASU
    // ====================================

    private fun formatDuration(
        seconds: Double
    ): String {

        val totalMinutes =
            (seconds / 60.0)
                .toInt()

        if (
            totalMinutes < 60
        ) {

            return "$totalMinutes min"
        }

        val hours =
            totalMinutes / 60

        val minutes =
            totalMinutes % 60

        return "${hours} h ${minutes} min"
    }

    // ====================================
    // VYMAZANIE TRASY
    // ====================================

    private fun clearRoute() {

        routeGeometryPoints.clear()

        navigationSteps.clear()

        currentNavigationStep =
            0

        routeSource?.setGeoJson(
            """
            {
              "type":"FeatureCollection",
              "features":[]
            }
            """.trimIndent()
        )

        clearOffRoadRoute()

        currentRouteDistanceMeters =
            0.0

        currentRouteDurationSeconds =
            0.0
    }

    // ====================================
    // VYMAZANIE CIEĽA
    // ====================================

    private fun clearSelectedTarget() {

        navigationActive =
            false

        hybridRouteActive =
            false

        offRoadNavigationActive =
            false

        routeEditMode =
            false

        selectedLatitude =
            null

        selectedLongitude =
            null

        roadEndLatitude =
            null

        roadEndLongitude =
            null

        arrivalSpoken =
            false

        clearRoute()

        routeViaPoints.clear()

        savedViaPoints.clear()

        updateViaPointSource()

        targetLocationSource?.setGeoJson(
            """
            {
              "type":"FeatureCollection",
              "features":[]
            }
            """.trimIndent()
        )

        searchMarkerSource?.setGeoJson(
            """
            {
              "type":"FeatureCollection",
              "features":[]
            }
            """.trimIndent()
        )

        targetPanel.visibility =
            View.GONE

        navigationPanel?.visibility =
            View.GONE

        routeEditPanel?.visibility =
            View.GONE
    }

    // ====================================
    // GPS ŠÍPKA
    // ====================================

    private fun createGpsArrowBitmap():
        Bitmap {

        val size =
            100

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        val arrowPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        arrowPaint.style =
            Paint.Style.FILL

        arrowPaint.color =
            android.graphics.Color.rgb(
                25,
                118,
                210
            )

        val path =
            Path()

        path.moveTo(
            size / 2f,
            8f
        )

        path.lineTo(
            size * 0.25f,
            size * 0.85f
        )

        path.lineTo(
            size / 2f,
            size * 0.68f
        )

        path.lineTo(
            size * 0.75f,
            size * 0.85f
        )

        path.close()

        canvas.drawPath(
            path,
            arrowPaint
        )

        return bitmap
    }

    // ====================================
    // GPS OPRÁVNENIE
    // ====================================

    private fun hasLocationPermission():
        Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED
    }

    // ====================================
    // KONTROLA GPS OPRÁVNENIA
    // ====================================

    private fun checkLocationPermission() {

        if (
            hasLocationPermission()
        ) {

            startLocationTracking()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        }
    }

    // ====================================
    // VÝSLEDOK OPRÁVNENIA
    // ====================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            LOCATION_REQUEST_CODE
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it ==
                    PackageManager.PERMISSION_GRANTED
                }
            ) {

                startLocationTracking()

            } else {

                Toast.makeText(
                    this,
                    "📍 Povolenie polohy nebolo udelené.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ====================================
    // SPUSTENIE GPS
    // ====================================

    private fun startLocationTracking() {

        if (
            !hasLocationPermission()
        ) {

            return
        }

        if (
            trackingLocation
        ) {

            return
        }

        trackingLocation =
            true

        try {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                locationListener
            )

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                3000L,
                5f,
                locationListener
            )

            val gpsLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )

            val networkLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )

            val bestLocation =
                gpsLocation
                    ?: networkLocation

            if (
                bestLocation != null
            ) {

                updateMyLocation(
                    bestLocation
                )
            }

        } catch (
            _: SecurityException
        ) {

            trackingLocation =
                false
        }
    }

    // ====================================
    // AKTUALIZÁCIA MOJEJ POLOHY
    // ====================================

    private fun updateMyLocation(
        location: Location
    ) {

        lastLocation =
            location

        val latitude =
            location.latitude

        val longitude =
            location.longitude

        locationSource?.setGeoJson(
            createLocationGeoJson(
                latitude,
                longitude
            )
        )

        locationLayer?.setProperties(
            PropertyFactory.iconRotate(
                currentAzimuth
            )
        )

        updateTargetDistance()

        if (
            firstGpsLocation
        ) {

            firstGpsLocation =
                false

            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        latitude,
                        longitude
                    ),
                    15.0
                )
            )
        }

        if (
            navigationActive
        ) {

            updateNavigationPanel()

            updateNavigationVoice(
                location
            )
        }

        if (
            hybridRouteActive
        ) {

            updateHybridDistanceDisplay()
        }
    }

    // ====================================
    // VZDIALENOSŤ K CIEĽU
    // ====================================

    private fun updateTargetDistance() {

        val targetLat =
            selectedLatitude

        val targetLon =
            selectedLongitude

        val current =
            lastLocation

        if (
            targetLat == null ||
            targetLon == null ||
            current == null
        ) {

            targetDistance.text =
                ""

            return
        }

        val distance =
            distanceBetweenCoordinates(
                current.latitude,
                current.longitude,
                targetLat,
                targetLon
            )

        targetDistance.text =
            "📏 ${formatDistance(distance)}"
    }

    // ====================================
    // GEOJSON MOJEJ POLOHY
    // ====================================

    private fun createLocationGeoJson(
        latitude: Double,
        longitude: Double
    ): String {

        return """
            {
              "type":"Feature",
              "geometry":{
                "type":"Point",
                "coordinates":[
                  $longitude,
                  $latitude
                ]
              },
              "properties":{}
            }
        """.trimIndent()
    }

    // ====================================
    // CENTROVANIE NA MOJU POLOHU
    // ====================================

    private fun centerOnMyLocation() {

        val location =
            lastLocation

        if (
            location == null
        ) {

            Toast.makeText(
                this,
                "📡 Čakám na GPS poloha.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    location.latitude,
                    location.longitude
                ),
                15.0
            )
        )
    }

    // ====================================
    // VYHĽADÁVANIE
    // ====================================

    private fun searchPlace(
        query: String
    ) {

        Toast.makeText(
            this,
            "🔍 Hľadám: $query",
            Toast.LENGTH_SHORT
        ).show()

        thread {

            var connection:
                HttpURLConnection? =
                null

            try {

                val encodedQuery =
                    URLEncoder.encode(
                        query,
                        "UTF-8"
                    )

                val url =
                    URL(
                        "https://nominatim.openstreetmap.org/search" +
                        "?q=$encodedQuery" +
                        "&format=jsonv2" +
                        "&limit=1" +
                        "&accept-language=sk"
                    )

                connection =
                    url.openConnection()
                        as HttpURLConnection

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    15000

                connection.setRequestProperty(
                    "User-Agent",
                    "Turista/1.0 (Android hiking application)"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept-Language",
                    "sk-SK,sk;q=0.9,en;q=0.8"
                )

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "❌ Vyhľadávanie sa nepodarilo. Internet alebo server.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@thread
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                val results =
                    JSONArray(
                        response
                    )

                if (
                    results.length() == 0
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "❌ Miesto sa nenašlo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@thread
                }

                val result =
                    results.getJSONObject(
                        0
                    )

                val latitude =
                    result.getDouble(
                        "lat"
                    )

                val longitude =
                    result.getDouble(
                        "lon"
                    )

                val displayName =
                    result.optString(
                        "display_name",
                        query
                    )

                runOnUiThread {

                    moveToSearchResult(
                        latitude,
                        longitude,
                        displayName
                    )
                }

            } catch (
                exception: Exception
            ) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "❌ Chyba vyhľadávania: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                connection?.disconnect()
            }
        }
    }

    // ====================================
    // PRESUN NA VÝSLEDOK VYHĽADÁVANIA
    // ====================================

    private fun moveToSearchResult(
        latitude: Double,
        longitude: Double,
        name: String
    ) {

        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    latitude,
                    longitude
                ),
                14.0
            )
        )

        searchMarkerSource?.setGeoJson(
            """
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [
                  $longitude,
                  $latitude
                ]
              },
              "properties": {
                "name": ${JSONObject.quote(name)}
              }
            }
            """.trimIndent()
        )

        Toast.makeText(
            this,
            "📍 $name",
            Toast.LENGTH_LONG
        ).show()
    }

    // ====================================
    // ŽIVOTNÝ CYKLUS MAPY
    // ====================================

    override fun onStart() {

        super.onStart()

        mapView.onStart()
    }

    // ====================================
    // ON RESUME
    // ====================================

    override fun onResume() {

        super.onResume()

        mapView.onResume()

        rotationSensor?.let { sensor ->

            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        if (
            hasLocationPermission()
        ) {

            startLocationTracking()
        }
    }

    // ====================================
    // ON PAUSE
    // ====================================

    override fun onPause() {

        super.onPause()

        mapView.onPause()

        sensorManager.unregisterListener(
            this
        )
    }

    // ====================================
    // ON STOP
    // ====================================

    override fun onStop() {

        super.onStop()

        mapView.onStop()
    }

    // ====================================
    // NÍZKA PAMÄŤ
    // ====================================

    override fun onLowMemory() {

        super.onLowMemory()

        mapView.onLowMemory()
    }

    // ====================================
    // ON DESTROY
    // ====================================

    override fun onDestroy() {

        gpsAnimator?.cancel()

        gpsAnimator =
            null

        stopLocationTracking()

        sensorManager.unregisterListener(
            this
        )

        textToSpeech?.stop()

        textToSpeech?.shutdown()

        textToSpeech =
            null

        mapView.onDestroy()

        super.onDestroy()
    }

    // ====================================
    // ZASTAVENIE GPS
    // ====================================

    private fun stopLocationTracking() {

        if (
            !trackingLocation
        ) {

            return
        }

        try {

            locationManager.removeUpdates(
                locationListener
            )

        } catch (
            _: SecurityException
        ) {
        }

        trackingLocation =
            false
    }

    // ====================================
    // ULOŽENIE STAVU
    // ====================================

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        mapView.onSaveInstanceState(
            outState
        )

        super.onSaveInstanceState(
            outState
        )
    }

    // ====================================
    // KOMPAS
    // ====================================

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        if (
            event.sensor.type !=
            Sensor.TYPE_ROTATION_VECTOR
        ) {

            return
        }

        val rotationMatrix =
            FloatArray(9)

        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            event.values
        )

        val orientation =
            FloatArray(3)

        SensorManager.getOrientation(
            rotationMatrix,
            orientation
        )

        var azimuth =
            Math.toDegrees(
                orientation[0].toDouble()
            ).toFloat()

        if (
            azimuth < 0
        ) {

            azimuth +=
                360f
        }

        currentAzimuth =
            azimuth

        compassView.setAzimuth(
            currentAzimuth
        )

        locationLayer?.setProperties(
            PropertyFactory.iconRotate(
                currentAzimuth
            )
        )
    }

    // ====================================
    // PRESNOSŤ KOMPASU
    // ====================================

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }

    // ====================================
    // KOMPAS – VIZUÁL
    // ====================================

    private class CompassView(
        context: android.content.Context
    ) : View(context) {

        private val compassPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        private var azimuth =
            0f

        init {

            compassPaint.strokeWidth =
                4f

            compassPaint.textAlign =
                Paint.Align.CENTER
        }

        // ====================================
        // NASTAVENIE SMERU
        // ====================================

        fun setAzimuth(
            value: Float
        ) {

            azimuth =
                value

            invalidate()
        }

        // ====================================
        // KRESLENIE KOMPASU
        // ====================================

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(
                canvas
            )

            val width =
                width.toFloat()

            val height =
                height.toFloat()

            val centerX =
                width / 2f

            val centerY =
                height / 2f

            val radius =
                minOf(
                    width,
                    height
                ) * 0.32f

            // ====================================
            // POZADIE
            // ====================================

            compassPaint.style =
                Paint.Style.FILL

            compassPaint.color =
                android.graphics.Color.argb(
                    190,
                    0,
                    0,
                    0
                )

            canvas.drawCircle(
                centerX,
                centerY,
                radius + 25f,
                compassPaint
            )

            // ====================================
            // KRUH
            // ====================================

            compassPaint.style =
                Paint.Style.STROKE

            compassPaint.strokeWidth =
                5f

            compassPaint.color =
                android.graphics.Color.WHITE

            canvas.drawCircle(
                centerX,
                centerY,
                radius,
                compassPaint
            )

            // ====================================
            // OTÁČANIE KOMPASU
            // ====================================

            canvas.save()

            canvas.rotate(
                -azimuth,
                centerX,
                centerY
            )

            // ====================================
            // ZNAČKY
            // ====================================

            compassPaint.strokeWidth =
                3f

            for (
                i in 0 until 360 step 30
            ) {

                val angle =
                    Math.toRadians(
                        i.toDouble()
                    )

                val outerX =
                    centerX +
                    cos(angle).toFloat() *
                    radius

                val outerY =
                    centerY +
                    kotlin.math.sin(
                        angle
                    ).toFloat() *
                    radius

                val innerRadius =
                    if (
                        i % 90 == 0
                    ) {

                        radius - 22f

                    } else {

                        radius - 12f
                    }

                val innerX =
                    centerX +
                    cos(angle).toFloat() *
                    innerRadius

                val innerY =
                    centerY +
                    kotlin.math.sin(
                        angle
                    ).toFloat() *
                    innerRadius

                canvas.drawLine(
                    outerX,
                    outerY,
                    innerX,
                    innerY,
                    compassPaint
                )
            }

            // ====================================
            // SLOVENSKÉ SMERY
            // ====================================

            compassPaint.style =
                Paint.Style.FILL

            compassPaint.textSize =
                28f

            compassPaint.typeface =
                android.graphics.Typeface.DEFAULT_BOLD

            drawDirectionText(
                canvas,
                "S",
                centerX,
                centerY - radius + 42f
            )

            drawDirectionText(
                canvas,
                "V",
                centerX + radius - 42f,
                centerY + 10f
            )

            drawDirectionText(
                canvas,
                "J",
                centerX,
                centerY + radius - 12f
            )

            drawDirectionText(
                canvas,
                "Z",
                centerX - radius + 42f,
                centerY + 10f
            )

            // ====================================
            // STRED
            // ====================================

            compassPaint.color =
                android.graphics.Color.WHITE

            canvas.drawCircle(
                centerX,
                centerY,
                8f,
                compassPaint
            )

            // ====================================
            // SEVERNÁ ŠÍPKA
            // ====================================

            val arrow =
                Path()

            arrow.moveTo(
                centerX,
                centerY - radius + 65f
            )

            arrow.lineTo(
                centerX - 12f,
                centerY - radius + 92f
            )

            arrow.lineTo(
                centerX,
                centerY - radius + 84f
            )

            arrow.lineTo(
                centerX + 12f,
                centerY - radius + 92f
            )

            arrow.close()

            compassPaint.color =
                android.graphics.Color.RED

            canvas.drawPath(
                arrow,
                compassPaint
            )

            canvas.restore()

            // ====================================
            // AKTUÁLNY AZIMUT
            // ====================================

            compassPaint.color =
                android.graphics.Color.WHITE

            compassPaint.textSize =
                22f

            compassPaint.typeface =
                android.graphics.Typeface.DEFAULT

            val azimuthText =
                "${azimuth.toInt()}°"

            canvas.drawText(
                azimuthText,
                centerX,
                centerY + radius + 55f,
                compassPaint
            )
        }

        // ====================================
        // TEXT SMERU
        // ====================================

        private fun drawDirectionText(
            canvas: Canvas,
            text: String,
            x: Float,
            y: Float
        ) {

            compassPaint.color =
                android.graphics.Color.WHITE

            compassPaint.textSize =
                28f

            compassPaint.typeface =
                android.graphics.Typeface.DEFAULT_BOLD

            canvas.drawText(
                text,
                x,
                y,
                compassPaint
            )
        }
    }

    // ====================================
    // KONIEC MAPACTIVITY
    // ====================================
}