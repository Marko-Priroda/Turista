package com.marko.turista

import android.content.Context
import btools.mapaccess.OsmNode
import btools.router.OsmNodeNamed
import btools.router.RoutingContext
import btools.router.RoutingEngine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Zabudované offline routovanie cez jadro BRouter.
 *
 * Mapové dlaždice MapLibre a routovacie dáta sú dve rozdielne veci.
 * BRouter používa súbory RD5 rozdelené na oblasti 5 × 5 stupňov.
 * Všetky súbory sa ukladajú iba do súkromného priečinka aplikácie Turista.
 */
object OfflineRoutingManager {

    private const val BASE_DIRECTORY = "offline_routing"
    private const val SEGMENTS_DIRECTORY = "segments4"
    private const val PROFILES_DIRECTORY = "profiles2"
    private const val SEGMENTS_URL =
        "https://brouter.de/brouter/segments4"
    private const val PROFILES_FALLBACK_URL =
        "https://raw.githubusercontent.com/abrensch/brouter/v1.7.9/misc/profiles2"
    private val supportFiles = listOf(
        "lookups.dat",
        "hiking-mountain.brf",
        "fastbike.brf",
        "car-vario.brf"
    )

    enum class Mode {
        WALK,
        BIKE,
        CAR
    }

    data class RoutePoint(
        val latitude: Double,
        val longitude: Double
    )

    data class RouteResult(
        val points: List<RoutePoint>,
        val distanceMeters: Double,
        val durationSeconds: Double,
        val turns: List<RouteTurn> = emptyList()
    )
    data class RouteTurn(val pointIndex: Int, val command: String, val exit: Int)

    data class DownloadProgress(
        val fileName: String,
        val fileIndex: Int,
        val fileCount: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) {
        val percent: Int
            get() {
                val insideFile =
                    if (totalBytes > 0L) {
                        downloadedBytes.toDouble() /
                            totalBytes.toDouble()
                    } else {
                        0.0
                    }

                return (
                    (
                        fileIndex.toDouble() +
                            insideFile
                    ) /
                        fileCount.coerceAtLeast(1).toDouble() *
                        100.0
                    )
                    .roundToInt()
                    .coerceIn(0, 100)
            }
    }

    private fun baseDirectory(context: Context): File =
        File(context.filesDir, BASE_DIRECTORY).apply {
            mkdirs()
        }

    fun segmentsDirectory(context: Context): File =
        File(baseDirectory(context), SEGMENTS_DIRECTORY).apply {
            mkdirs()
        }

    private fun profilesDirectory(context: Context): File =
        File(baseDirectory(context), PROFILES_DIRECTORY).apply {
            mkdirs()
        }

    fun requiredSegments(
        west: Double,
        south: Double,
        east: Double,
        north: Double
    ): List<String> {
        val safeSouth = south.coerceIn(-90.0, 90.0)
        val safeNorth = north.coerceIn(-90.0, 90.0)

        val latitudeTiles = tileOrigins(
            safeSouth,
            safeNorth,
            -90,
            85
        )

        val longitudeTiles =
            if (west <= east) {
                tileOrigins(west, east, -180, 175)
            } else {
                tileOrigins(west, 180.0, -180, 175) +
                    tileOrigins(-180.0, east, -180, 175)
            }

        return latitudeTiles
            .flatMap { latitude ->
                longitudeTiles.map { longitude ->
                    segmentName(longitude, latitude)
                }
            }
            .distinct()
            .sorted()
    }

    fun missingSegments(
        context: Context,
        west: Double,
        south: Double,
        east: Double,
        north: Double
    ): List<String> {
        val directory = segmentsDirectory(context)

        return requiredSegments(
            west,
            south,
            east,
            north
        ).filter { name ->
            val file = File(directory, name)
            !file.isFile || file.length() == 0L
        }
    }

    fun hasRoutingDataForBounds(
        context: Context,
        west: Double,
        south: Double,
        east: Double,
        north: Double
    ): Boolean =
        missingSegments(
            context,
            west,
            south,
            east,
            north
        ).isEmpty()

    /**
     * Volá sa na pracovnom vlákne. Už existujúce súbory sa znova nesťahujú.
     */
    fun downloadForBounds(
        context: Context,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        onProgress: (DownloadProgress) -> Unit
    ) {
        ensureSupportFiles(context)

        val missing = missingSegments(
            context,
            west,
            south,
            east,
            north
        )

        val directory = segmentsDirectory(context)

        missing.forEachIndexed { index, name ->
            downloadFile(
                "$SEGMENTS_URL/$name",
                File(directory, name)
            ) { downloaded, total ->
                onProgress(
                    DownloadProgress(
                        fileName = name,
                        fileIndex = index,
                        fileCount = missing.size.coerceAtLeast(1),
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                )
            }
        }
    }

    fun route(
        context: Context,
        waypoints: List<RoutePoint>,
        mode: Mode
    ): RouteResult {
        require(waypoints.size >= 2) {
            "Trasa potrebuje aspoň začiatok a cieľ."
        }

        val profile = profileFile(context, mode)

        if (!profile.isFile || profile.length() == 0L) {
            throw IllegalStateException(
                "Chýba offline profil navigácie. Stiahni mapu štátu znova."
            )
        }

        val nodes = waypoints.mapIndexed { index, point ->
            OsmNodeNamed(
                OsmNode(
                    longitudeToInternal(point.longitude),
                    latitudeToInternal(point.latitude)
                )
            ).apply {
                name = when (index) {
                    0 -> "from"
                    waypoints.lastIndex -> "to"
                    else -> "via$index"
                }
            }
        }

        val routingContext = RoutingContext().apply {
            localFunction = profile.absolutePath
            outputFormat = "geojson"
            turnInstructionMode = 3
            memoryclass = (
                Runtime.getRuntime().maxMemory() /
                    (1024L * 1024L)
                )
                .toInt()
                .coerceIn(64, 256)
        }

        val engine = RoutingEngine(
            null,
            null,
            segmentsDirectory(context),
            nodes,
            routingContext
        )

        engine.doRun(120_000L)

        val error = engine.getErrorMessage()

        if (!error.isNullOrBlank()) {
            throw IllegalStateException(translateRoutingError(error))
        }

        val track = engine.getFoundTrack()

        if (track.nodes.isEmpty()) {
            throw IllegalStateException(
                "Offline trasa sa v stiahnutých dátach nenašla."
            )
        }

        val points = track.nodes.map { node ->
            RoutePoint(
                latitude =
                    (node.getILat() - 90_000_000) /
                        1_000_000.0,
                longitude =
                    (node.getILon() - 180_000_000) /
                        1_000_000.0
            )
        }

        return RouteResult(
            points = points,
            distanceMeters = track.distance.toDouble(),
            durationSeconds = track.getTotalSeconds().toDouble(),
            turns = points.indices.mapNotNull { index ->
                track.getVoiceHint(index)?.let { RouteTurn(index, it.getCommandString(3), kotlin.math.abs(it.getExitNumber())) }
            }
        )
    }

    private fun ensureSupportFiles(context: Context) {
        val directory = profilesDirectory(context)

        supportFiles.forEach { name ->
            val target = File(directory, name)

            if (!target.isFile || target.length() == 0L) {
                try {
                    context.assets
                        .open("brouter/profiles2/$name")
                        .use { input ->
                            FileOutputStream(target, false)
                                .buffered()
                                .use { output ->
                                    input.copyTo(output)
                                }
                        }
                } catch (_: IOException) {
                    // Niektoré správcovia súborov pri kopírovaní projektu
                    // vynechajú nový priečinok assets. Vtedy sa presne tá istá
                    // verzia profilu bezpečne stiahne pri opakovaní.
                    downloadFile(
                        "$PROFILES_FALLBACK_URL/$name",
                        target
                    ) { _, _ -> }
                }
            }
        }
    }

    private fun profileFile(
        context: Context,
        mode: Mode
    ): File {
        val name = when (mode) {
            Mode.WALK -> "hiking-mountain.brf"
            Mode.BIKE -> "fastbike.brf"
            Mode.CAR -> "car-vario.brf"
        }

        return File(profilesDirectory(context), name)
    }

    private fun downloadFile(
        source: String,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ) {
        target.parentFile?.mkdirs()

        val partial = File(
            target.parentFile,
            "${target.name}.part"
        )

        var connection: HttpURLConnection? = null

        try {
            connection =
                URL(source).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "User-Agent",
                "Turista/1.0"
            )

            val response = connection.responseCode

            if (response !in 200..299) {
                throw IllegalStateException(
                    "Server routovacích dát odpovedal HTTP $response."
                )
            }

            val total = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.buffered().use { input ->
                FileOutputStream(partial, false)
                    .buffered()
                    .use { output ->
                        val buffer = ByteArray(64 * 1024)

                        while (true) {
                            val count = input.read(buffer)

                            if (count < 0) {
                                break
                            }

                            output.write(buffer, 0, count)
                            downloaded += count.toLong()
                            onProgress(downloaded, total)
                        }
                    }
            }

            if (total > 0L && partial.length() != total) {
                throw IllegalStateException(
                    "Routovací súbor sa nestiahol celý."
                )
            }

            if (target.exists() && !target.delete()) {
                throw IllegalStateException(
                    "Starý routovací súbor sa nedá nahradiť."
                )
            }

            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
        } catch (exception: Exception) {
            partial.delete()
            throw exception
        } finally {
            connection?.disconnect()
        }
    }

    private fun tileOrigins(
        minimum: Double,
        maximum: Double,
        lowest: Int,
        highest: Int
    ): List<Int> {
        val start =
            (floor(minimum / 5.0).toInt() * 5)
                .coerceIn(lowest, highest)

        val adjustedMaximum =
            if (maximum > minimum) {
                maximum - 0.0000001
            } else {
                maximum
            }

        val end =
            (floor(adjustedMaximum / 5.0).toInt() * 5)
                .coerceIn(lowest, highest)

        if (end < start) {
            return emptyList()
        }

        return (start..end step 5).toList()
    }

    private fun segmentName(
        longitude: Int,
        latitude: Int
    ): String {
        val longitudePart =
            if (longitude < 0) {
                "W${abs(longitude)}"
            } else {
                "E$longitude"
            }

        val latitudePart =
            if (latitude < 0) {
                "S${abs(latitude)}"
            } else {
                "N$latitude"
            }

        return "${longitudePart}_${latitudePart}.rd5"
    }

    private fun longitudeToInternal(longitude: Double): Int =
        (
            (longitude + 180.0) *
                1_000_000.0
            )
            .roundToInt()

    private fun latitudeToInternal(latitude: Double): Int =
        (
            (latitude + 90.0) *
                1_000_000.0
            )
            .roundToInt()

    private fun translateRoutingError(error: String): String {
        val lower = error.lowercase()

        return when {
            "datafile" in lower ||
                "segment" in lower ||
                "file not found" in lower ->
                "Pre túto trasu chýba stiahnutá routovacia oblasť."

            "not found" in lower ||
                "target island" in lower ->
                "Cesta alebo chodník medzi bodmi sa nenašiel."

            "timeout" in lower ->
                "Výpočet offline trasy trval príliš dlho."

            else ->
                "Offline trasu sa nepodarilo vypočítať: $error"
        }
    }
}
