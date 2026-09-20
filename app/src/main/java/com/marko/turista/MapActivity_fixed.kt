package com.marko.turista

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.content.pm.PackageManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
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
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.Surface
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
import org.maplibre.android.geometry.LatLngBounds
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

    private val rerouteGate=RerouteGate()
    private var viaOffsets:List<Double> = emptyList()
    private var nextVia=0
    private val editUndo=mutableListOf<List<LatLng>>()
    private fun rememberEdit() {editUndo.add(routeViaPoints.toList());if(editUndo.size>20) editUndo.removeAt(0)}
    private var placeSearch: PlaceSearch? = null
    private var savedTrackJson: String? = null
    private var savedTrackLoad = 0
    private var selectedPlaceName = ""
    private var manualRoute = false
    private var savedManualRoute = false
    private var drawingRoute = false
    private var strokeStart = 0
    private var routeRequest = 0
    private var progress: RouteProgress? = null
    private var roadLength = 0.0
    private var lastGuidanceTime = 0L
    private var lastSpeech = ""
    private var lastSpeechAt = 0L
    private val announced = mutableSetOf<String>()
    private var roadEndAnnounced = false
    private var routeLoading = false
    private var trafficRefresh: Runnable? = null
    private val trafficHandler = Handler(Looper.getMainLooper())
    private fun LatLng.navPoint() = RouteProgress.Point(latitude, longitude)

    companion object {

        const val MAP_STYLE_URL =
            "https://tiles.openfreemap.org/styles/liberty"
    }

    // ====================================
    // MAPA
    // ====================================

    private lateinit var mapView: MapView

    private var map: MapLibreMap? = null
    private val settings by lazy { TuristaSettings(this) }
    private var mapMenu: PopupWindow? = null
    private var navigationFollowPaused = false
    private var lastFollowSetting = true

    private lateinit var offlineManager: OfflineManager
    private var offlineMode = false
    private var offlineRegionId = -1L
    private var offlineWest = 0.0
    private var offlineSouth = 0.0
    private var offlineEast = 0.0
    private var offlineNorth = 0.0

    private lateinit var connectivityManager: ConnectivityManager
    private val connectivityHandler = Handler(Looper.getMainLooper())
    private var connectivityCallbackRegistered = false
    private var automaticOfflineActive = false
    private var automaticOfflineSwitchInProgress = false
    private var lastKnownOnline: Boolean? = null

    private val connectivityCheckRunnable = Runnable {
        handleConnectivityChange()
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                scheduleConnectivityCheck()
            }

            override fun onLost(network: Network) {
                scheduleConnectivityCheck()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                scheduleConnectivityCheck()
            }
        }

    private var searchMarkerSource: GeoJsonSource? =
        null

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

    private var selectedNavigationMode = NavigationMode.WALK
    private var navigationMode =
        NavigationMode.WALK

    private data class NavigationStep(
        val latitude: Double,
        val longitude: Double,
        val distance: Double,
        val duration: Double,
        val type: String,
        val modifier: String,
        val name: String,
        val offset: Double = 0.0,
        val exit: Int = 0
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

    private var lastVoiceConfiguration = ""
    private var lastVoiceAvailable = false
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

    private var filteredAzimuth =
        Float.NaN

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

        offlineManager = OfflineManager.getInstance(this)

        connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        setContentView(
            R.layout.activity_map
        )

        ScreenInsets.applyTo(findViewById(R.id.mapRoot))

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

                    textToSpeech?.setAudioAttributes(android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    textToSpeechReady = result != null &&
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

            navigationMode=selectedNavigationMode
            if(navigationMode!=NavigationMode.WALK) manualRoute=false
            startNavigationToSelectedPoint()
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

        val undoRoutePointButton =
            findViewById<Button>(
                R.id.undoRoutePointButton
            )

        val clearRoutePointsButton =
            findViewById<Button>(
                R.id.clearRoutePointsButton
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

        findViewById<Button>(R.id.routeModeButton).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Ako upraviť trasu?")
                .setItems(arrayOf("Po cestách a chodníkoch", "Vlastná pešia trasa mimo ciest")) { _, which ->
                    manualRoute = which == 1
                    drawingRoute = false
                    updateEditorMode()
                    updateViaPointSource()
                }.show()
        }
        findViewById<Button>(R.id.drawRouteButton).setOnClickListener {
            drawingRoute = !drawingRoute
            updateEditorMode()
        }
        findViewById<RouteDrawingView>(R.id.routeDrawing).apply {
            onStrokeStart = { rememberEdit();strokeStart = routeViaPoints.size }
            onStrokeCancel = {
                while (routeViaPoints.size > strokeStart) routeViaPoints.removeAt(routeViaPoints.lastIndex)
                if(editUndo.isNotEmpty()) editUndo.removeAt(editUndo.lastIndex)
                updateViaPointSource(); updateRouteEditInfo()
            }
            onPoint = { x, y ->
                map?.projection?.fromScreenLocation(android.graphics.PointF(x,y))?.let { point ->
                    if (routeViaPoints.size < 2000 && (routeViaPoints.isEmpty() ||
                        RouteProgress.distance(routeViaPoints.last().navPoint(), point.navPoint()) > 3)) {
                        routeViaPoints.add(point); updateViaPointSource(); updateRouteEditInfo()
                    }
                }
            }
        }
        undoRoutePointButton.setOnClickListener {
            if(editUndo.isNotEmpty()) {routeViaPoints.clear();routeViaPoints.addAll(editUndo.removeAt(editUndo.lastIndex));updateViaPointSource();updateRouteEditInfo()}
        }
        clearRoutePointsButton.setOnClickListener {rememberEdit();routeViaPoints.clear();updateViaPointSource();updateRouteEditInfo()}
        findViewById<View>(R.id.manageRoutePointsButton).setOnClickListener {showRoutePoints()}

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

        walkRouteButton.setOnClickListener {selectedNavigationMode=NavigationMode.WALK;updateModeSelection()}
        bikeRouteButton.setOnClickListener {selectedNavigationMode=NavigationMode.BIKE;updateModeSelection()}
        carRouteButton.setOnClickListener {selectedNavigationMode=NavigationMode.CAR;updateModeSelection()}
        updateModeSelection()

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

        offlineMode = intent.getBooleanExtra("offline_mode", false)
        offlineRegionId = intent.getLongExtra("offline_region_id", -1L)
        offlineWest = intent.getDoubleExtra("offline_west", 0.0)
        offlineSouth = intent.getDoubleExtra("offline_south", 0.0)
        offlineEast = intent.getDoubleExtra("offline_east", 0.0)
        offlineNorth = intent.getDoubleExtra("offline_north", 0.0)

        mapView.getMapAsync { mapInstance ->

            map = mapInstance
                mapInstance.addOnMapClickListener { point ->

                    if (
                        routeEditMode
                    ) {

                        addRouteViaPoint(
                            point
                        )

                    } else if(navigationActive) {
                        AlertDialog.Builder(this).setTitle("Vybrané miesto").setItems(arrayOf("Pridať medzibod do aktuálnej trasy","Nastaviť nový cieľ","Zrušiť")) { _, choice ->
                            if(choice==0) {
                                if(nextVia>0) {val passed=nextVia.coerceAtMost(routeViaPoints.size);repeat(passed){routeViaPoints.removeAt(0)};viaOffsets=viaOffsets.drop(passed);nextVia=0}
                                routeViaPoints.add(point);updateViaPointSource();startNavigationToSelectedPoint()
                            } else if(choice==1) selectDestination(point)
                        }.show()
                    } else selectDestination(point)
                    true
                }

            mapInstance.uiSettings.setCompassEnabled(false)
            mapInstance.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    navigationFollowPaused = true
                }
            }

            // Povoliť detailné priblíženie online aj offline mapy.
            // Zoom 20 je vhodný aj na chodníky a presný výber bodu trasy.
            mapInstance.setMinZoomPreference(2.0)
            mapInstance.setMaxZoomPreference(20.0)

            mapInstance.uiSettings.apply {
                setZoomGesturesEnabled(true)
                setDoubleTapGesturesEnabled(true)
                setQuickZoomGesturesEnabled(true)
                setScrollGesturesEnabled(true)
            }

            val installOffline = offlineMode && offlineRegionId >= 0L

            if (installOffline) {
                offlineManager.getOfflineRegion(
                    offlineRegionId,
                    object : OfflineManager.GetOfflineRegionCallback {
                        override fun onRegion(offlineRegion: org.maplibre.android.offline.OfflineRegion) {
                            mapInstance.setOfflineRegionDefinition(
                                offlineRegion.definition
                            ) {
                                initializeMapLayersAndInteraction(mapInstance)
                            }
                        }

                        override fun onRegionNotFound() {
                            mapInstance.setStyle(MAP_STYLE_URL) {
                                initializeMapLayersAndInteraction(mapInstance)
                                Toast.makeText(
                                    this@MapActivity,
                                    "⚠️ Offline mapa sa nenašla. Zobrazujem online mapu.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onError(error: String) {
                            mapInstance.setStyle(MAP_STYLE_URL) {
                                initializeMapLayersAndInteraction(mapInstance)
                                Toast.makeText(this@MapActivity, "⚠️ Offline mapa sa nenačítala: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            } else if (!isInternetAvailable()) {
                activateAutomaticOfflineMap(mapInstance, true)
            } else {
                mapInstance.setStyle(
                    MAP_STYLE_URL
                ) {
                    initializeMapLayersAndInteraction(mapInstance)
                }
            }

        }

        setupMapControls()
    }

    private fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun scheduleConnectivityCheck() {
        connectivityHandler.removeCallbacks(connectivityCheckRunnable)
        connectivityHandler.postDelayed(connectivityCheckRunnable, 700L)
    }

    private fun handleConnectivityChange() {
        val online = isInternetAvailable()

        if (lastKnownOnline == online) {
            return
        }

        lastKnownOnline = online

        val currentMap = map ?: return
        applyBackgroundMap()

        if (offlineMode) {
            return
        }

        if (online) {
            if (automaticOfflineActive) {
                automaticOfflineActive = false
                automaticOfflineSwitchInProgress = false

                currentMap.setStyle(MAP_STYLE_URL) {
                    initializeMapLayersAndInteraction(currentMap)
                    restoreMapStateAfterStyleChange()

                    Toast.makeText(
                        this,
                        "🌐 Internet je dostupný – online mapa je aktívna.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            activateAutomaticOfflineMap(currentMap, false)
        }
    }

    private fun activateAutomaticOfflineMap(
        mapInstance: MapLibreMap,
        showOnlineFallback: Boolean
    ) {
        if (automaticOfflineSwitchInProgress || automaticOfflineActive) {
            return
        }

        automaticOfflineSwitchInProgress = true

        offlineManager.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {

                override fun onList(regions: Array<org.maplibre.android.offline.OfflineRegion>?) {
                    runOnUiThread {
                        val selectedRegion = selectOfflineRegionForCurrentLocation(regions)

                        if (selectedRegion == null) {
                            automaticOfflineSwitchInProgress = false

                            if (showOnlineFallback) {
                                mapInstance.setStyle(MAP_STYLE_URL) {
                                    initializeMapLayersAndInteraction(mapInstance)
                                }
                            }

                            Toast.makeText(
                                this@MapActivity,
                                "📡 Internet nie je dostupný a pre túto polohu nie je stiahnutá offline mapa.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }

                        mapInstance.setOfflineRegionDefinition(
                            selectedRegion.definition
                        ) {
                            automaticOfflineActive = true
                            automaticOfflineSwitchInProgress = false
                            initializeMapLayersAndInteraction(mapInstance)
                            restoreMapStateAfterStyleChange()

                            Toast.makeText(
                                this@MapActivity,
                                "📴 Internet bol odpojený – používam stiahnutú offline mapu.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        automaticOfflineSwitchInProgress = false

                        if (showOnlineFallback) {
                            mapInstance.setStyle(MAP_STYLE_URL) {
                                initializeMapLayersAndInteraction(mapInstance)
                            }
                        }

                        Toast.makeText(
                            this@MapActivity,
                            "Offline mapy sa nepodarilo načítať: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun selectOfflineRegionForCurrentLocation(
        regions: Array<org.maplibre.android.offline.OfflineRegion>?
    ): org.maplibre.android.offline.OfflineRegion? {
        if (regions.isNullOrEmpty()) {
            return null
        }

        val location = lastLocation ?: return regions.firstOrNull()

        return regions.firstOrNull { region ->
            try {
                val metadata = JSONObject(String(region.metadata, Charsets.UTF_8))
                val west = metadata.optDouble("west", Double.NaN)
                val south = metadata.optDouble("south", Double.NaN)
                val east = metadata.optDouble("east", Double.NaN)
                val north = metadata.optDouble("north", Double.NaN)

                !west.isNaN() && !south.isNaN() &&
                    !east.isNaN() && !north.isNaN() &&
                    location.longitude in west..east &&
                    location.latitude in south..north
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun restoreMapStateAfterStyleChange() {
        drawOffRoadLine()
        if (selectedLatitude != null && selectedLongitude != null) targetLocationSource?.setGeoJson(createTargetGeoJson(selectedLatitude!!,selectedLongitude!!))
        updateViaPointSource()

        if (routeGeometryPoints.isNotEmpty()) {
            drawNavigationRoute()
        }

        val location = lastLocation
        if (location != null) {
            locationSource?.setGeoJson(
                createLocationGeoJson(location.latitude, location.longitude)
            )
        }
    }

    private fun initializeMapLayersAndInteraction(mapInstance: MapLibreMap) {

                // Offline definícia môže po načítaní vrátiť limit zoomu na
                // maximum stiahnutého regiónu. Znova povolíme priblíženie až
                // na úroveň 20; chýbajúce vyššie offline úrovne sa zobrazia
                // zväčšením najdetailnejšej dostupnej vektorovej dlaždice.
                mapInstance.setMinZoomPreference(2.0)
                mapInstance.setMaxZoomPreference(20.0)

                mapInstance.uiSettings.apply {
                    setZoomGesturesEnabled(true)
                    setDoubleTapGesturesEnabled(true)
                    setQuickZoomGesturesEnabled(true)
                    setScrollGesturesEnabled(true)
                }

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

                mapInstance.style?.let { style ->
                    if (style.getSource("route-preview-source") == null) {
                        style.addSource(GeoJsonSource("route-preview-source", emptyGeoJson()))
                        style.addLayer(LineLayer("route-preview-layer", "route-preview-source").withProperties(
                            PropertyFactory.lineColor("#1976D2"),PropertyFactory.lineWidth(4f),PropertyFactory.lineDasharray(arrayOf(1f,1.2f))))
                    }
                }
                applyMapSettings()
                restoreSavedTrack()
                findViewById<View>(R.id.mapRoot).post { openSavedIntent() }

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
        }

    private fun setupMapControls() {

        // ====================================
        // VYHĽADÁVANIE
        // ====================================

        val searchMap =
            findViewById<EditText>(
                R.id.searchMap
            )

        placeSearch = PlaceSearch(this, searchMap, { isInternetAvailable() }) { lat, lon, name ->
            moveToSearchResult(lat, lon, name)
            selectDestination(LatLng(lat,lon))
            selectedPlaceName = name
        }
        searchMap.setOnFocusChangeListener { _, focused ->
            if(focused) {
                targetPanel.visibility=View.GONE
                navigationPanel?.visibility=View.GONE
                routeEditPanel?.visibility=View.GONE
                placeSearch?.search()
            } else {
                placeSearch?.cancel()
                targetPanel.visibility=if(selectedLatitude!=null && !routeEditMode) View.VISIBLE else View.GONE
                navigationPanel?.visibility=if(navigationActive && !routeEditMode) View.VISIBLE else View.GONE
                routeEditPanel?.visibility=if(routeEditMode) View.VISIBLE else View.GONE
            }
        }
        findViewById<View>(R.id.savePlaceButton).setOnClickListener { saveSelectedPlace() }

        val compassOverlay = findViewById<FrameLayout>(R.id.compassOverlay)
        compassView = CompassView(this)
        compassOverlay.removeAllViews()
        compassOverlay.addView(compassView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        compassOverlay.setOnClickListener { compassOverlay.visibility = View.GONE }

        findViewById<View>(R.id.mapMenuButton).setOnClickListener { anchor ->
            showMapMenu(anchor)
        }
        findViewById<View>(R.id.locationButton).setOnClickListener {
            navigationFollowPaused = false
            if (hasLocationPermission()) centerOnMyLocation() else checkLocationPermission()
        }
        findViewById<View>(R.id.mapModesButton).setOnClickListener { showMapLayersDialog() }

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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showMapMenu(anchor: View) {
        if (mapMenu?.isShowing == true) {
            mapMenu?.dismiss()
            return
        }
        placeSearch?.cancel()
        val content = layoutInflater.inflate(R.layout.popup_map_menu, null, false)
        val availableWidth = findViewById<View>(R.id.mapRoot).width - dp(32)
        val popup = PopupWindow(content, minOf(dp(272), availableWidth.coerceAtLeast(dp(180))),
            ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.isOutsideTouchable = true
        val visible = android.graphics.Rect()
        anchor.getWindowVisibleDisplayFrame(visible)
        popup.height = minOf(dp(456), (visible.height()-dp(100)).coerceAtLeast(dp(120)))
        popup.elevation = dp(12).toFloat()
        popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        popup.setOnDismissListener {
            mapMenu = null
            anchor.contentDescription = "Otvoriť menu"
        }
        content.findViewById<View>(R.id.menuAccount).setOnClickListener{popup.dismiss();startActivity(Intent(this,AccountActivity::class.java))}
        content.findViewById<View>(R.id.menuDownloads).setOnClickListener {
            popup.dismiss()
            startActivity(Intent(this, OfflineMapsActivity::class.java))
        }
        content.findViewById<View>(R.id.menuSettings).setOnClickListener {
            popup.dismiss()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        content.findViewById<View>(R.id.menuCompass).setOnClickListener {
            popup.dismiss()
            if (rotationSensor == null) {
                Toast.makeText(this, "Toto zariadenie nemá podporovaný kompasový senzor.", Toast.LENGTH_LONG).show()
            } else {
                val overlay = findViewById<View>(R.id.compassOverlay)
                val root = findViewById<View>(R.id.mapRoot)
                val size = minOf(dp(300), root.width - root.paddingLeft - root.paddingRight - dp(100),
                    root.height - root.paddingTop - root.paddingBottom - dp(24)).coerceAtLeast(dp(120))
                overlay.layoutParams = overlay.layoutParams.apply { width = size; height = size }
                overlay.visibility = if (overlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        content.findViewById<View>(R.id.menuRecognizer).setOnClickListener {popup.dismiss();startActivity(Intent(this,RecognizerActivity::class.java))}
        listOf(R.id.menuSteps to "steps", R.id.menuRecord to "record", R.id.menuSaved to "saved", R.id.menuDonate to "donate").forEach { (id,page) ->
            content.findViewById<View>(id).setOnClickListener {
                popup.dismiss()
                startActivity(Intent(this,if(page=="saved") SavedFoldersActivity::class.java else TripActivity::class.java).putExtra("page",page))
            }
        }
        mapMenu = popup
        anchor.contentDescription = "Zavrieť menu"
        popup.showAsDropDown(anchor, 0, dp(8), Gravity.END)
    }

    private fun applyUserSettings() {
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (!settings.voiceGuidance) textToSpeech?.stop()
        if (lastFollowSetting != settings.followNavigation) navigationFollowPaused = false
        lastFollowSetting = settings.followNavigation
        applyMapSettings()
        updateTargetDistance()
        updateNavigationPanel()
    }

    private fun applyMapSettings() {
        val currentMap = map ?: return
        currentMap.uiSettings.setCompassEnabled(false)
        currentMap.uiSettings.setRotateGesturesEnabled(settings.rotateMap)
        if (!settings.rotateMap && currentMap.cameraPosition.bearing != 0.0) {
            currentMap.cameraPosition = CameraPosition.Builder(currentMap.cameraPosition).bearing(0.0).build()
        }
        applyBackgroundMap()
    }

    private fun applyBackgroundMap() {
        val style = map?.style ?: return
        if (style.getLayer("search-marker-layer") == null) return
        val key = TrafficKeyStore(this).get()
        val type = if (isInternetAvailable() && (settings.mapType != "traffic" || key.isNotBlank())) settings.mapType else "default"
        MapBackgrounds.apply(style, type, key, refresh = type == "traffic")
        findViewById<TextView>(R.id.mapAttribution).apply {
            text = Html.fromHtml(MapBackgrounds.attribution(type), Html.FROM_HTML_MODE_LEGACY)
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun showMapLayersDialog() {
        val current = if (isInternetAvailable()) settings.mapType else "default"
        val selected = MapBackgrounds.ids.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Typ mapy")
            .setSingleChoiceItems(MapBackgrounds.titles, selected) { dialog, index ->
                if (index != 0 && !isInternetAvailable()) {
                    (dialog as AlertDialog).listView.setItemChecked(selected, true)
                    Toast.makeText(this, "Satelitná, terénna a dopravná mapa potrebujú internet. Offline používam stiahnutú predvolenú mapu.", Toast.LENGTH_LONG).show()
                } else if (MapBackgrounds.ids[index] == "traffic" && TrafficKeyStore(this).get().isBlank()) {
                    dialog.dismiss()
                    AlertDialog.Builder(this).setTitle("Dopravná mapa")
                        .setMessage("Živá premávka potrebuje vlastný API kľúč TomTom. Môžeš ho overiť a uložiť v nastaveniach.")
                        .setPositiveButton("Nastavenia") { _, _ -> startActivity(Intent(this, SettingsActivity::class.java)) }
                        .setNegativeButton("Zavrieť",null).show()
                } else if (map?.style?.getLayer("search-marker-layer") == null) {
                    (dialog as AlertDialog).listView.setItemChecked(selected, true)
                    Toast.makeText(this, "Počkaj, kým sa načíta mapa.", Toast.LENGTH_SHORT).show()
                } else {
                    settings.mapType = MapBackgrounds.ids[index]
                    applyBackgroundMap()
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Zavrieť", null)
            .show()
    }

    private fun updateModeSelection() {
        listOf(Triple(R.id.walkRouteButton,NavigationMode.WALK,"Pešo"),Triple(R.id.bikeRouteButton,NavigationMode.BIKE,"Bicykel"),Triple(R.id.carRouteButton,NavigationMode.CAR,"Auto")).forEach { (id,mode,label) ->
            findViewById<Button>(id).apply {
                val chosen=selectedNavigationMode==mode
                isSelected=chosen;text=if(chosen) "✓ $label" else label
                contentDescription=label+if(chosen) ", vybrané" else ", nevybrané"
                backgroundTintList=android.content.res.ColorStateList.valueOf(if(chosen) Color.rgb(23,53,44) else Color.rgb(226,234,215))
                setTextColor(if(chosen) Color.WHITE else Color.rgb(23,53,44))
            }
        }
    }

    // ====================================
    // VÝBER CIEĽA
    // ====================================

    private fun saveSelectedPlace() {
        val lat=selectedLatitude ?: return
        val lon=selectedLongitude ?: return
        val input=EditText(this).apply {
            setSingleLine(true);setText(selectedPlaceName.ifBlank {"Moje miesto"});selectAll()
            filters=arrayOf(android.text.InputFilter.LengthFilter(120))
        }
        val dialog=AlertDialog.Builder(this).setTitle("Uložiť miesto").setView(input)
            .setPositiveButton("Uložiť",null).setNegativeButton("Zrušiť",null).create()
        dialog.setOnShowListener {dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name=input.text.toString().trim()
            if(name.isBlank()) input.error="Zadaj názov" else {
                try {TripStore.get(this).savePlace(name,lat,lon);dialog.dismiss();Toast.makeText(this,"Miesto je uložené v menu Uložené.",Toast.LENGTH_SHORT).show()}
                catch(_:Exception){input.error="Miesto sa nepodarilo uložiť."}
            }
        }};dialog.show()
    }

    override fun onNewIntent(newIntent:Intent) {
        super.onNewIntent(newIntent);setIntent(newIntent);openSavedIntent()
    }
    private fun openSavedIntent() {
        if(map?.style?.getLayer("search-marker-layer")==null) return
        if(intent.hasExtra("saved_place")) {
            val id=intent.getLongExtra("saved_place",0);intent.removeExtra("saved_place")
            val place=TripStore.get(this).places().firstOrNull {it.id==id} ?: return
            selectDestination(LatLng(place.lat,place.lon));selectedPlaceName=place.name
            moveToSearchResult(place.lat,place.lon,place.name)
        }
        if(intent.hasExtra("saved_track")) {
            val id=intent.getLongExtra("saved_track",0);intent.removeExtra("saved_track")
            val token=++savedTrackLoad
            if(id<0) {savedTrackJson=null;restoreSavedTrack();return}
            thread {
                val points=TripStore.get(this).samples(id)
                val lines=JSONArray()
                points.groupBy {it.segment}.values.forEach {segment ->
                    if(segment.size>=2) lines.put(JSONArray().apply {segment.forEach {put(JSONArray().put(it.lon).put(it.lat))}})
                }
                val features=JSONArray()
                features.put(JSONObject().put("type","Feature").put("properties",JSONObject()).put("geometry",JSONObject().put("type","MultiLineString").put("coordinates",lines)))
                points.groupBy {it.segment}.values.filter {it.size==1}.forEach { segment ->
                    val point=segment.first()
                    features.put(JSONObject().put("type","Feature").put("properties",JSONObject()).put("geometry",JSONObject().put("type","Point").put("coordinates",JSONArray().put(point.lon).put(point.lat))))
                }
                val json=JSONObject().put("type","FeatureCollection").put("features",features).toString()
                runOnUiThread {
                    if(isDestroyed || token!=savedTrackLoad) return@runOnUiThread
                    savedTrackJson=json;restoreSavedTrack();navigationFollowPaused=true;firstGpsLocation=false
                    if(points.isNotEmpty() && points.all {kotlin.math.abs(it.lat-points[0].lat)<0.00001 && kotlin.math.abs(it.lon-points[0].lon)<0.00001}) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(points[0].lat,points[0].lon),16.0))
                    else if(points.isNotEmpty()) {
                        val bounds=LatLngBounds.Builder();points.forEach {bounds.include(LatLng(it.lat,it.lon))}
                        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),dp(64)))
                    }
                    Toast.makeText(this,"Uložená trasa je zobrazená fialovou farbou.",Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun restoreSavedTrack() {
        val style=map?.style ?: return
        if(style.getSource("saved-track-source")==null) {
            style.addSource(GeoJsonSource("saved-track-source",emptyGeoJson()))
            style.addLayer(LineLayer("saved-track-layer","saved-track-source").withProperties(PropertyFactory.lineColor("#8E44AD"),PropertyFactory.lineWidth(5f)))
            style.addLayer(CircleLayer("saved-track-point","saved-track-source").withProperties(PropertyFactory.circleColor("#8E44AD"),PropertyFactory.circleRadius(5f)))
        }
        style.getSourceAs<GeoJsonSource>("saved-track-source")?.setGeoJson(savedTrackJson?:emptyGeoJson())
    }

    private fun selectDestination(
        point: LatLng
    ) {
        if (navigationActive || routeLoading) clearSelectedTarget()
        selectedPlaceName = ""

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

        if (selectedLatitude == null || selectedLongitude == null) {

            Toast.makeText(
                this,
                "Najprv vyber cieľ na mape.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        findViewById<View>(R.id.searchMap).clearFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(findViewById<View>(R.id.searchMap).windowToken,0)
        routeRequest++
        editUndo.clear()
        if(nextVia>0) {val passed=nextVia.coerceAtMost(routeViaPoints.size);repeat(passed){routeViaPoints.removeAt(0)};viaOffsets=viaOffsets.drop(passed);nextVia=0}
        routeLoading = false
        navigationActive = progress != null
        textToSpeech?.stop()
        routeEditMode = true
        applyMapSettings()

        savedManualRoute = manualRoute
        savedViaPoints = routeViaPoints.toMutableList()
        targetPanel.visibility = View.GONE
        navigationPanel?.visibility = View.GONE
        updateEditorMode()

        routeEditPanel?.visibility =
            View.VISIBLE

        updateRouteEditInfo()
        updateViaPointSource()

        Toast.makeText(
            this,
            "👆 Klikaj na mapu a pridávaj body trasy.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ====================================
    // PRIDANIE MEDZIBODU
    // ====================================

    private fun addRouteViaPoint(point: LatLng) {
        if(!routeEditMode) return
        val limit=if(manualRoute) 2000 else 98
        if(routeViaPoints.size>=limit) {Toast.makeText(this,"Dosiahnutý limit $limit bodov.",Toast.LENGTH_SHORT).show();return}
        if(routeViaPoints.lastOrNull()?.let {RouteProgress.distance(it.navPoint(),point.navPoint())<2}==true) return
        rememberEdit();routeViaPoints.add(point);updateViaPointSource();updateRouteEditInfo()
    }
    private fun showRoutePoints() {
        if(routeViaPoints.isEmpty()) {Toast.makeText(this,"Ťukni na mapu a pridaj prvý bod.",Toast.LENGTH_SHORT).show();return}
        val names=routeViaPoints.mapIndexed {i,p->"${i+1}. bod · ${String.format(Locale.US,"%.5f, %.5f",p.latitude,p.longitude)}"}.toTypedArray()
        AlertDialog.Builder(this).setTitle("Poradie bodov").setItems(names) {_,index->
            AlertDialog.Builder(this).setTitle("Bod ${index+1}").setItems(arrayOf("Ukázať","Posunúť skôr","Posunúť neskôr","Vymazať")) {_,action->
                if(index !in routeViaPoints.indices) return@setItems
                when(action) {
                    0 -> map?.animateCamera(CameraUpdateFactory.newLatLng(routeViaPoints[index]))
                    1 -> if(index>0) {rememberEdit();java.util.Collections.swap(routeViaPoints,index,index-1)}
                    2 -> if(index<routeViaPoints.lastIndex) {rememberEdit();java.util.Collections.swap(routeViaPoints,index,index+1)}
                    3 -> {rememberEdit();routeViaPoints.removeAt(index)}
                }
                updateViaPointSource();updateRouteEditInfo()
            }.show()
        }.show()
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
        routeEditInfo?.text = if (manualRoute)
            "${routeViaPoints.size} bodov • od tvojej polohy cez body až k cieľu. Bodky sú vlastná trasa; priechodnosť nie je overená."
        else "${routeViaPoints.size} medzibodov • ťukaním ich pridáš v poradí. Čiara je náhľad; Použiť vypočíta trasu po cestách."
    }

    private fun updateEditorMode() {
        findViewById<Button>(R.id.routeModeButton).text = if (manualRoute) "Režim: vlastná pešia trasa" else "Režim: po cestách a chodníkoch"
        findViewById<Button>(R.id.drawRouteButton).apply {
            visibility = if (manualRoute) View.VISIBLE else View.GONE
            text = if (drawingRoute) "Posúvať mapu / pridávať body" else "Kresliť prstom"
        }
        findViewById<View>(R.id.routeDrawing).visibility = if (routeEditMode && manualRoute && drawingRoute) View.VISIBLE else View.GONE
        updateRouteEditInfo()
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

        routeViaSource?.setGeoJson(collection.toString())
        map?.style?.getSourceAs<GeoJsonSource>("route-preview-source")?.setGeoJson(
            if (routeEditMode) lineJson(buildList {
                lastLocation?.let { add(LatLng(it.latitude, it.longitude)) }
                addAll(routeViaPoints)
                if (selectedLatitude != null && selectedLongitude != null) add(LatLng(selectedLatitude!!, selectedLongitude!!))
            }) else emptyGeoJson()
        )
    }

    // ====================================
    // DOKONČENIE ÚPRAVY
    // ====================================

    private fun finishRouteEditing() {
        routeEditMode = false
        drawingRoute = false
        updateEditorMode()
        updateViaPointSource()
        routeEditPanel?.visibility = View.GONE
        if (manualRoute) navigationMode = NavigationMode.WALK
        if (selectedLatitude != null && selectedLongitude != null) startNavigationToSelectedPoint()
    }


    // ====================================
    // ZRUŠENIE ÚPRAVY
    // ====================================

    private fun cancelRouteEditing() {
        routeEditPointsRestore()
        manualRoute = savedManualRoute
        routeEditMode = false
        drawingRoute = false
        updateEditorMode()
        routeEditPanel?.visibility = View.GONE
        targetPanel.visibility = View.VISIBLE
        navigationPanel?.visibility = if (navigationActive) View.VISIBLE else View.GONE
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

    private fun formatDistance(meters: Double): String = settings.formatDistance(meters)

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

        if (manualRoute && navigationMode != NavigationMode.WALK) {
            Toast.makeText(this, "Vlastná trasa mimo ciest je pešia. Pre auto alebo bicykel zvoľ režim po cestách.", Toast.LENGTH_LONG).show()
            return
        }
        rerouteGate.reset();nextVia=0;viaOffsets=emptyList()
        navigationActive = true
        navigationFollowPaused = false

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

    private fun requestRoadRoute(startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double, recalculate: Boolean = false) {
        val token = ++routeRequest
        val mode = if(recalculate && offRoadNavigationActive) NavigationMode.WALK else navigationMode
        val manual = manualRoute
        val vias=if(recalculate) routeViaPoints.drop(nextVia) else routeViaPoints.toList()
        val offline = offlineMode || automaticOfflineActive || !isInternetAvailable()
        val points = listOf(LatLng(startLatitude,startLongitude)) + vias + LatLng(endLatitude,endLongitude)
        routeLoading = true
        navigationInstruction?.text = "Počítam trasu…"
        navigationInstructionDistance?.text = ""
        navigationRemaining?.text = ""
        if(!recalculate) {
            progress = null
            routeSource?.setGeoJson(emptyGeoJson()); offRoadSource?.setGeoJson(emptyGeoJson())
        }
        thread {
            try {
                val result = if (manual) RouteData(emptyList(), points, emptyList(), 0.0)
                    else {
                        val road = calculateRoad(points, mode, offline)
                        val target = points.last()
                        val last = road.road.lastOrNull() ?: error("Cesta sa nenašla.")
                        val gap = RouteProgress.distance(last.navPoint(), target.navPoint())
                        val tail = if (gap > 15) {
                            if (mode == NavigationMode.CAR) {
                                val foot = try { calculateRoad(listOf(last,target), NavigationMode.WALK, offline).road } catch (_: Exception) { emptyList() }
                                // Never invent a drivable connection to an off-road destination.
                                (listOf(last) + foot + target).distinctAdjacent()
                            } else listOf(last,target)
                        } else emptyList()
                        road.copy(tail = tail)
                    }
                runOnUiThread {
                    if (token != routeRequest || !navigationActive || isDestroyed) return@runOnUiThread
                    routeLoading = false
                    if(recalculate) {routeViaPoints.clear();routeViaPoints.addAll(vias);navigationMode=mode;updateViaPointSource()}
                    viaOffsets=result.viaOffsets;nextVia=0
                    routeGeometryPoints.clear(); routeGeometryPoints.addAll(result.road)
                    offRoadGeometryPoints.clear(); offRoadGeometryPoints.addAll(result.tail)
                    navigationSteps.clear(); navigationSteps.addAll(result.steps.filter { it.type != "arrive" && it.type != "depart" })
                    currentNavigationStep = 0; announced.clear(); roadEndAnnounced = false
                    roadLength = RouteProgress(result.road.map { it.navPoint() }).length
                    offRoadDistanceMeters = RouteProgress(result.tail.map { it.navPoint() }).length
                    progress = RouteProgress((result.road + result.tail).distinctAdjacent().map { it.navPoint() })
                    currentRouteDistanceMeters = progress?.length ?: 0.0
                    currentRouteDurationSeconds = result.duration + offRoadDistanceMeters / 1.2
                    hybridRouteActive = result.tail.isNotEmpty()
                    offRoadNavigationActive = manual
                    roadEndLatitude = result.road.lastOrNull()?.latitude
                    roadEndLongitude = result.road.lastOrNull()?.longitude
                    drawNavigationRoute(); drawOffRoadLine()
                    lastGuidanceTime = 0L
                    lastLocation?.let { updateNavigationVoice(it) }
                    updateNavigationPanel()
                    if (manual) speak("Vlastná pešia trasa je pripravená. Sleduj bodkovanú čiaru.")
                    else if (hybridRouteActive && mode == NavigationMode.CAR) Toast.makeText(this,
                        "Na konci pokračuje peší úsek. Bodkované spojenia mimo chodníkov nemajú overenú priechodnosť.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (token != routeRequest || isDestroyed) return@runOnUiThread
                    routeLoading = false
                    if(recalculate) {
                        rerouteGate.failed(android.os.SystemClock.elapsedRealtime())
                        navigationInstruction?.text="Prepočet zlyhal, skúsim znova"
                        navigationInstructionDistance?.text="Pôvodná trasa zostáva na mape"
                        speak("Trasu sa zatiaľ nepodarilo prepočítať.")
                        return@runOnUiThread
                    }
                    navigationActive = false
                    navigationInstruction?.text = "Trasu sa nepodarilo vypočítať"
                    navigationInstructionDistance?.text = "Skús upraviť body alebo režim trasy."
                    Toast.makeText(this, e.message ?: "Výpočet trasy zlyhal.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private data class RouteData(val road: List<LatLng>, val tail: List<LatLng>, val steps: List<NavigationStep>, val duration: Double, val viaOffsets: List<Double> = emptyList())
    private fun List<LatLng>.distinctAdjacent(): List<LatLng> = filterIndexed { i, p -> i == 0 || RouteProgress.distance(this[i-1].navPoint(), p.navPoint()) > 0.3 }

    private fun calculateRoad(points: List<LatLng>, mode: NavigationMode, offline: Boolean): RouteData {
        if (offline) {
            val result = OfflineRoutingManager.route(applicationContext,
                points.map { OfflineRoutingManager.RoutePoint(it.latitude,it.longitude) },
                when(mode) { NavigationMode.CAR -> OfflineRoutingManager.Mode.CAR; NavigationMode.BIKE -> OfflineRoutingManager.Mode.BIKE; else -> OfflineRoutingManager.Mode.WALK })
            val road = result.points.map { LatLng(it.latitude,it.longitude) }
            val offsets = RouteProgress(road.map { it.navPoint() }).cumulative
            val steps = result.turns.mapNotNull { turn ->
                val point = road.getOrNull(turn.pointIndex) ?: return@mapNotNull null
                val command = turn.command
                if (command in listOf("C", "BL", "END", "START")) return@mapNotNull null
                val modifier = when(command) {
                    "TL", "TSLL", "TSHL", "KL", "EL" -> "left"
                    "TR", "TSLR", "TSHR", "KR", "ER" -> "right"
                    "TU", "TRU", "TLU" -> "uturn"
                    else -> "straight"
                }
                val type = if (command.startsWith("RND") || command.startsWith("RNL")) "roundabout"
                    else if(command in listOf("KL","KR")) "fork" else "turn"
                NavigationStep(point.latitude,point.longitude,0.0,0.0,type,modifier,"",offsets[turn.pointIndex],turn.exit)
            }
            var previousOffset=0.0
            val routePath=RouteProgress(road.map {it.navPoint()})
            val offsetsVia=points.drop(1).dropLast(1).map {point ->
                routePath.project(point.navPoint(),previousOffset).along.also {previousOffset=it}
            }
            return RouteData(road, emptyList(), steps, result.durationSeconds, offsetsVia)
        }
        require(points.size <= 100) { "Pre cestnú trasu použi najviac 98 medzibodov. Vlastná pešia trasa podporuje aj kreslenie." }
        val profile = when(mode) { NavigationMode.CAR -> "car"; NavigationMode.BIKE -> "bike"; else -> "foot" }
        val coordinates = points.joinToString(";") { "${it.longitude},${it.latitude}" }
        val connection = URL("https://routing.openstreetmap.de/routed-$profile/route/v1/driving/$coordinates?overview=full&geometries=geojson&steps=true").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "Turista/1.7")
            if (connection.responseCode != 200) error("Server trasy nie je dostupný (${connection.responseCode}).")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optString("code") != "Ok") error("Cesta medzi zvolenými bodmi sa nenašla.")
            val route = json.getJSONArray("routes").getJSONObject(0)
            val road = parseRouteGeometry(route.getJSONObject("geometry").getJSONArray("coordinates"))
            require(road.size >= 2) { "Server nevrátil použiteľnú trasu." }
            val length = RouteProgress(road.map { it.navPoint() }).length
            val scale = length / route.optDouble("distance", length).coerceAtLeast(1.0)
            val steps = parseNavigationSteps(route.optJSONArray("legs")).map { it.copy(offset = it.offset*scale) }
            val legs=route.getJSONArray("legs")
            var cumulative=0.0
            val offsetsVia=(0 until legs.length()-1).map {i->cumulative+=legs.getJSONObject(i).optDouble("distance",0.0)*scale;cumulative}
            return RouteData(road, emptyList(), steps, route.optDouble("duration",0.0), offsetsVia)
        } finally { connection.disconnect() }
    }

    private fun emptyGeoJson() = "{\"type\":\"FeatureCollection\",\"features\":[]}"
    private fun lineJson(points: List<LatLng>): String {
        if (points.size < 2) return emptyGeoJson()
        val coordinates = JSONArray()
        points.forEach { coordinates.put(JSONArray().put(it.longitude).put(it.latitude)) }
        return JSONObject().put("type","Feature").put("properties",JSONObject())
            .put("geometry",JSONObject().put("type","LineString").put("coordinates",coordinates)).toString()
    }
    private fun drawOffRoadLine() { offRoadSource?.setGeoJson(lineJson(offRoadGeometryPoints)) }


    // ====================================
    // ZABUDOVANÁ OFFLINE TRASA
    // ====================================

    private fun requestOfflineRoadRoute(startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double) {
        requestRoadRoute(startLatitude,startLongitude,endLatitude,endLongitude)
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

    private fun updateHybridDistanceDisplay() { updateNavigationPanel() }


    // ====================================
    // NAVIGAČNÝ PANEL
    // ====================================

    private fun updateNavigationPanel() {
        if (!navigationActive || routeLoading || routeEditMode) return
        if (arrivalSpoken) {
            navigationInstruction?.text = "Dorazil si do cieľa"
            navigationInstructionDistance?.text = ""
            navigationRemaining?.text = ""
            return
        }
        val p = progress ?: return
        navigationRemaining?.text = "Zostáva: ${formatDistance(calculateRemainingRouteDistance())}"
        if (lastLocation?.let { !it.hasAccuracy() || it.accuracy > 35 } != false) {
            navigationInstruction?.text = "Čakám na presnejšiu polohu GPS"
            navigationInstructionDistance?.text = ""
        } else if (p.crossTrack > 60) {
            navigationInstruction?.text = "Si mimo naplánovanej trasy"
            navigationInstructionDistance?.text = "Uprav trasu alebo sa k nej vráť"
        } else if (offRoadNavigationActive) {
            navigationInstruction?.text = if (manualRoute) "Sleduj vlastnú pešiu trasu" else "Pokračuj pešo po bodkovanej trase"
            navigationInstructionDistance?.text = "Úseky mimo chodníkov: priechodnosť neoverená"
        } else if (currentNavigationStep < navigationSteps.size) {
            val step = navigationSteps[currentNavigationStep]
            navigationInstruction?.text = createNavigationInstruction(step)
            navigationInstructionDistance?.text = formatDistance((step.offset-p.along).coerceAtLeast(0.0))
        } else {
            navigationInstruction?.text = if (hybridRouteActive && navigationMode == NavigationMode.CAR) "Blížiš sa ku koncu jazdy" else "Pokračuj po trase"
            navigationInstructionDistance?.text = if (hybridRouteActive && navigationMode == NavigationMode.CAR) "Potom pokračuj pešo" else ""
        }
    }


    // ====================================
    // ZOSTÁVAJÚCA VZDIALENOSŤ
    // ====================================

    private fun calculateRemainingRouteDistance(): Double = progress?.let { (it.length-it.along).coerceAtLeast(0.0) } ?: currentRouteDistanceMeters


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

    private fun parseNavigationSteps(legs: JSONArray?): List<NavigationStep> {
        val steps = mutableListOf<NavigationStep>()
        var offset = 0.0
        if (legs == null) return steps
        for (l in 0 until legs.length()) {
            val array = legs.getJSONObject(l).optJSONArray("steps") ?: continue
            for (i in 0 until array.length()) {
                val step = array.getJSONObject(i)
                val maneuver = step.optJSONObject("maneuver")
                val location = maneuver?.optJSONArray("location")
                val distance = step.optDouble("distance",0.0)
                if (location != null && location.length() >= 2) steps.add(NavigationStep(
                    location.getDouble(1),location.getDouble(0),distance,step.optDouble("duration",0.0),
                    maneuver.optString("type"),maneuver.optString("modifier"),step.optString("name"),offset,maneuver.optInt("exit",0)))
                offset += distance
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

    private fun createNavigationInstruction(step: NavigationStep): String {
        val direction = when {
            step.type == "roundabout" || step.type == "rotary" -> if (step.exit > 0) "Na kruhovom objazde použi ${step.exit}. výjazd" else "Vojdi na kruhový objazd"
            step.type == "exit roundabout" || step.type == "exit rotary" -> "Vyjdi z kruhového objazdu"
            step.modifier == "uturn" -> "Otoč sa, keď je to možné"
            step.type == "depart" -> "Vyraz po trase"
            step.type == "arrive" -> "Blížiš sa ku koncu úseku"
            step.type == "fork" -> if ("left" in step.modifier) "Drž sa vľavo" else if ("right" in step.modifier) "Drž sa vpravo" else "Pokračuj rovno"
            step.type == "merge" -> "Zaraď sa do premávky"
            "left" in step.modifier -> "Odboč doľava"
            "right" in step.modifier -> "Odboč doprava"
            else -> "Pokračuj rovno"
        }
        return if (step.name.isBlank() || step.type in listOf("roundabout","rotary")) direction else "$direction na ${step.name}"
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

    private fun updateNavigationVoice(location: Location) {
        if (!navigationActive || routeLoading || routeEditMode || arrivalSpoken) return
        val p = progress ?: return
        if (!location.hasAccuracy() || location.accuracy > 35) return
        val now = android.os.SystemClock.elapsedRealtime()
        val elapsed = if (lastGuidanceTime == 0L) 1.0 else ((now-lastGuidanceTime)/1000.0).coerceIn(1.0,30.0)
        val car = navigationMode == NavigationMode.CAR && !offRoadNavigationActive
        val speed = (if(location.hasSpeed()) location.speed.toDouble() else if(car) 8.0 else 1.4).coerceIn(0.0,55.0)
        p.update(RouteProgress.Point(location.latitude,location.longitude), (speed*elapsed*2+location.accuracy*2+50).coerceAtLeast(100.0))
        lastGuidanceTime = now
        if(p.crossTrack<=35) {
            while(nextVia<viaOffsets.size && p.along>=viaOffsets[nextVia]+10) nextVia++
        }
        if(rerouteGate.check(now,p.crossTrack,location.accuracy,car,!manualRoute && !routeLoading && !routeEditMode)) {
            val lat=selectedLatitude;val lon=selectedLongitude
            if(lat!=null && lon!=null) {
                speak("Si mimo trasy. Počítam novú cestu k cieľu.")
                requestRoadRoute(location.latitude,location.longitude,lat,lon,recalculate=true)
                return
            }
        }
        if (p.crossTrack > 60) {
            if (now-lastSpeechAt > 45000) speak(if(manualRoute) "Si mimo vlastnej trasy. Vráť sa k bodkovanej čiare alebo uprav trasu." else "Si mimo naplánovanej trasy.")
            return
        }
        if (hybridRouteActive && !offRoadNavigationActive && p.along >= roadLength-18 &&
            roadEndLatitude != null && distanceBetweenCoordinates(location.latitude,location.longitude,roadEndLatitude!!,roadEndLongitude!!) < 35) {
            offRoadNavigationActive = true
            if (!roadEndAnnounced) {
                roadEndAnnounced = true
                speak(if(navigationMode == NavigationMode.CAR) "Koniec automobilového úseku. Zaparkuj na dovolenom mieste a pokračuj pešo po bodkovanej trase." else "Pokračuj po bodkovanom úseku k cieľu.")
            }
        }
        if (p.length-p.along < 30 && distanceToFinalTarget(location) < 20 && location.accuracy <= 25) {
            arrivalSpoken = true
            speak("Dorazil si do cieľa.")
            updateNavigationPanel()
            return
        }
        if (offRoadNavigationActive) return
        while (currentNavigationStep < navigationSteps.size && navigationSteps[currentNavigationStep].offset < p.along-12) currentNavigationStep++
        val step = navigationSteps.getOrNull(currentNavigationStep) ?: return
        val distance = (step.offset-p.along).coerceAtLeast(0.0)
        val near = RouteProgress.turnDistance(speed,car)
        val advance = RouteProgress.advanceDistance(speed,car)
        val key = "$currentNavigationStep"
        if (distance <= near && "$key:turn" !in announced) {
            announced.add("$key:turn"); announced.add("$key:advance")
            speak(createNavigationInstruction(step))
        } else if (distance <= advance && "$key:advance" !in announced) {
            announced.add("$key:advance")
            val rounded = ((distance/10).toInt()*10).coerceAtLeast(10)
            speak("O $rounded metrov ${createNavigationInstruction(step).replaceFirstChar {it.lowercase()}}")
        }
    }


    // ====================================
    // HLAS
    // ====================================

    private fun speak(text: String) {
        if (!textToSpeechReady || !settings.voiceGuidance || routeEditMode) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (text == lastSpeech && now-lastSpeechAt < 3000) return
        val engine=textToSpeech ?: return
        val preferences=VoicePreferences(this)
        val online=isInternetAvailable()
        val configuration="${preferences.name}|${preferences.online}|${preferences.speed}|$online"
        if(configuration!=lastVoiceConfiguration) {
            lastVoiceAvailable=GuidanceVoice.apply(engine,preferences,online)
            lastVoiceConfiguration=configuration
        }
        if(!lastVoiceAvailable) return
        lastSpeech = text; lastSpeechAt = now
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "turista_navigation_$now")
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

    private fun drawNavigationRoute() { routeSource?.setGeoJson(lineJson(routeGeometryPoints)) }


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
        routeRequest++
        rerouteGate.reset();nextVia=0;viaOffsets=emptyList()
        routeLoading = false
        progress = null
        manualRoute = false
        drawingRoute = false
        findViewById<View>(R.id.routeDrawing).visibility = View.GONE
        map?.style?.getSourceAs<GeoJsonSource>("route-preview-source")?.setGeoJson(emptyGeoJson())
        textToSpeech?.stop()

        navigationActive =
            false

        hybridRouteActive =
            false

        offRoadNavigationActive =
            false

        routeEditMode =
            false
        applyMapSettings()

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

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        paint.style =
            Paint.Style.FILL

        paint.color =
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
            paint
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
        val age = (android.os.SystemClock.elapsedRealtimeNanos()-location.elapsedRealtimeNanos)/1_000_000_000.0
        if (age > 30 || age < -1) return
        val previous = lastLocation
        if (previous != null) {
            val delta = (location.elapsedRealtimeNanos-previous.elapsedRealtimeNanos)/1_000_000_000.0
            if (delta <= 0) return
            if (delta < 10 && location.accuracy > previous.accuracy*2 && location.accuracy > 35) return
        }


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

            if (settings.followNavigation && !navigationFollowPaused && !routeEditMode) {
                map?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)))
            }
            updateNavigationVoice(location)
            updateNavigationPanel()
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

    private fun searchPlace(query: String) { placeSearch?.search() }

    private fun moveToSearchResult(
        latitude: Double,
        longitude: Double,
        name: String
    ) {
        navigationFollowPaused = true
        firstGpsLocation = false

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
    }    // ====================================
    // ŽIVOTNÝ CYKLUS MAPY
    // ====================================

    override fun onStart() {

        super.onStart()

        mapView.onStart()

        if (!connectivityCallbackRegistered) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                connectivityCallbackRegistered = true
            } catch (_: Exception) {
            }
        }

        scheduleConnectivityCheck()
    }

    // ====================================
    // ON RESUME
    // ====================================

    override fun onResume() {

        super.onResume()

        mapView.onResume()
        applyUserSettings()
        trafficRefresh?.let { trafficHandler.removeCallbacks(it) }
        trafficRefresh = object : Runnable {
            override fun run() {
                if (settings.mapType == "traffic" && isInternetAvailable()) {
                    map?.style?.let { style -> if (style.getLayer("search-marker-layer") != null)
                        MapBackgrounds.apply(style, "traffic", TrafficKeyStore(this@MapActivity).get(), refresh = true)
                    }
                }
                trafficHandler.postDelayed(this, 120000)
            }
        }.also { trafficHandler.postDelayed(it, 120000) }

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
        placeSearch?.cancel()
        trafficRefresh?.let { trafficHandler.removeCallbacks(it) }
        trafficRefresh = null
        mapMenu?.dismiss()
        textToSpeech?.stop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mapView.onPause()

        sensorManager.unregisterListener(
            this
        )
    }

    @Deprecated("Compatibility with the project's AppCompat version")
    override fun onBackPressed() {
        val compass = findViewById<View>(R.id.compassOverlay)
        if (routeEditMode) {
            cancelRouteEditing()
        } else if (compass.visibility == View.VISIBLE) {
            compass.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    // ====================================
    // ON STOP
    // ====================================

    override fun onStop() {

        super.onStop()

        if (connectivityCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
            }

            connectivityCallbackRegistered = false
        }

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
        placeSearch?.destroy()
        savedTrackLoad++
        routeRequest++

        connectivityHandler.removeCallbacksAndMessages(null)

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

        val adjustedRotationMatrix = FloatArray(9)

        when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                adjustedRotationMatrix
            )
            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_X,
                SensorManager.AXIS_MINUS_Y,
                adjustedRotationMatrix
            )
            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_Y,
                SensorManager.AXIS_X,
                adjustedRotationMatrix
            )
            else -> System.arraycopy(rotationMatrix, 0, adjustedRotationMatrix, 0, 9)
        }

        val orientation =
            FloatArray(3)

        SensorManager.getOrientation(
            adjustedRotationMatrix,
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

        if (filteredAzimuth.isNaN()) {
            filteredAzimuth = azimuth
        } else {
            val shortestTurn = ((azimuth - filteredAzimuth + 540f) % 360f) - 180f
            filteredAzimuth = (filteredAzimuth + shortestTurn * 0.18f + 360f) % 360f
        }

        currentAzimuth = filteredAzimuth

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
    // KONIEC MAPACTIVITY
    // ====================================

}
