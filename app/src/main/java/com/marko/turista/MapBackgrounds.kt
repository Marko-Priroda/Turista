package com.marko.turista

import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

/** Online-only raster backgrounds; navigation layers above them are preserved. */
object MapBackgrounds {
    val ids = arrayOf("default", "satellite", "terrain", "traffic")
    val titles = arrayOf("Predvolená", "Satelitná · online", "Terénna · online", "Dopravná · TomTom online")

    fun apply(style: Style, type: String, trafficKey: String = "", refresh: Boolean = false) {
        if (refresh || type != "traffic" || trafficKey.isBlank()) {
            style.removeLayer("turista-background-traffic")
            style.removeSource("turista-background-source-traffic")
        }
        for (id in ids.drop(1)) {
            style.getLayer("turista-background-$id")?.setProperties(PropertyFactory.visibility("none"))
        }
        if (type == "default") return
        val layerId = "turista-background-$type"
        if (style.getLayer(layerId) == null) {
            val url = when (type) {
                "satellite" -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                "terrain" -> "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"
                "traffic" -> if (trafficKey.isBlank()) return else "https://api.tomtom.com/traffic/map/4/tile/flow/relative0/{z}/{x}/{y}.png?key=${java.net.URLEncoder.encode(trafficKey, "UTF-8")}&tileSize=256"
                else -> return
            }
            val tiles = TileSet("2.1.0", url)
            tiles.minZoom = 0f
            tiles.maxZoom = if (type == "terrain") 17f else if (type == "traffic") 22f else 19f
            val sourceId = "turista-background-source-$type"
            if (style.getSource(sourceId) == null) style.addSource(RasterSource(sourceId, tiles, 256))
            // This is the first application overlay. All navigation/GPS layers remain above it.
            val layer = RasterLayer(layerId, sourceId)
            style.addLayerBelow(layer, "search-marker-layer")
        }
        style.getLayer(layerId)?.setProperties(PropertyFactory.visibility("visible"))
    }

    fun attribution(type: String): String = when (type) {
        "traffic" -> "© <a href=\"https://www.tomtom.com\">TomTom</a> · ${attribution("default")}<br>Premávka: zelená plynulá · žltá spomalenie · červená kolóna. Bez farby = bez údajov. Trasa nezohľadňuje zápchy."
        "satellite" -> "Snímky © <a href=\"https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer\">Esri, Vantor, Earthstar Geographics a GIS User Community</a>"
        "terrain" -> "Dáta © <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a>, SRTM · Mapa © <a href=\"https://opentopomap.org\">OpenTopoMap</a> (<a href=\"https://creativecommons.org/licenses/by-sa/3.0/\">CC BY-SA</a>)"
        else -> "© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> · <a href=\"https://openmaptiles.org\">OpenMapTiles</a> · <a href=\"https://openfreemap.org\">OpenFreeMap</a>"
    }
}
