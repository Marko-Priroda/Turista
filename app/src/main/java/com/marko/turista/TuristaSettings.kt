package com.marko.turista

import android.content.Context
import java.util.Locale

/** One preference store shared by the map, settings screen and layer picker. */
class TuristaSettings(context: Context) {
    var mapType: String
        get() = preferences.getString("map_type", "default") ?: "default"
        set(value) { preferences.edit().putString("map_type", value).apply() }

    private val preferences = context.applicationContext
        .getSharedPreferences("turista_settings", Context.MODE_PRIVATE)

    var voiceGuidance: Boolean
        get() = preferences.getBoolean("voice_guidance", true)
        set(value) { preferences.edit().putBoolean("voice_guidance", value).apply() }

    var keepScreenOn: Boolean
        get() = preferences.getBoolean("keep_screen_on", false)
        set(value) { preferences.edit().putBoolean("keep_screen_on", value).apply() }

    var followNavigation: Boolean
        get() = preferences.getBoolean("follow_navigation", true)
        set(value) { preferences.edit().putBoolean("follow_navigation", value).apply() }

    var rotateMap: Boolean
        get() = preferences.getBoolean("rotate_map", true)
        set(value) { preferences.edit().putBoolean("rotate_map", value).apply() }

    var imperialUnits: Boolean
        get() = preferences.getBoolean("imperial_units", false)
        set(value) { preferences.edit().putBoolean("imperial_units", value).apply() }

    fun reset() { preferences.edit().clear().apply() }

    fun formatDistance(meters: Double): String {
        val distance = meters.coerceAtLeast(0.0)
        return if (imperialUnits) {
            val miles = distance / 1609.344
            if (miles < 0.1) "${(distance / 0.3048).toInt()} ft"
            else String.format(Locale.getDefault(), "%.1f mi", miles)
        } else {
            if (distance < 1000.0) "${distance.toInt()} m"
            else String.format(Locale.getDefault(), "%.1f km", distance / 1000.0)
        }
    }
}
