package com.marko.turista

data class CountryBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
) {
    companion object {
        fun boundsFor(country: String): CountryBounds? = when (country) {
            "Slovensko" -> CountryBounds(16.83, 47.73, 22.57, 49.61)
            "Česko" -> CountryBounds(12.09, 48.55, 18.86, 51.06)
            "Poľsko" -> CountryBounds(14.12, 49.00, 24.15, 54.84)
            "Rakúsko" -> CountryBounds(9.48, 46.37, 17.17, 49.02)
            "Maďarsko" -> CountryBounds(16.11, 45.74, 22.90, 48.59)
            "Nemecko" -> CountryBounds(5.87, 47.27, 15.04, 55.06)
            "Taliansko" -> CountryBounds(6.63, 36.62, 18.52, 47.10)
            "Francúzsko" -> CountryBounds(-5.14, 41.33, 9.56, 51.09)
            "Rakúsko" -> CountryBounds(9.48, 46.37, 17.17, 49.02)
            else -> null
        }
    }
}
