package com.marko.turista

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

/**
 * Lokálny správca routovacích dát.
 *
 * Dôležité: MapLibre offline balík obsahuje mapové zdroje, nie routovací graf.
 * Táto trieda preto pripravuje oddelené miesto pre routovací balík ku krajine.
 * Samotný routovací súbor môže byť v ďalšom kroku dodaný ako prebuilt OSM/PBF
 * alebo pripravený lokálny graph engine.
 */
object OfflineRoutingManager {

    private const val DIRECTORY_NAME = "offline_routing"
    private const val MANIFEST_SUFFIX = ".routing.json"

    data class RoutingPackage(
        val country: String,
        val file: File
    )

    fun getDirectory(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun packageFile(context: Context, country: String): File {
        val safeName = country
            .lowercase()
            .replace("[^a-z0-9áäčďéíĺľňóôŕšťúýž]+".toRegex(), "_")
            .trim('_')
        return File(getDirectory(context), "$safeName.routing.pbf")
    }

    fun manifestFile(context: Context, country: String): File {
        val safeName = country
            .lowercase()
            .replace("[^a-z0-9áäčďéíĺľňóôŕšťúýž]+".toRegex(), "_")
            .trim('_')
        return File(getDirectory(context), "$safeName$MANIFEST_SUFFIX")
    }

    fun hasPackage(context: Context, country: String): Boolean {
        return packageFile(context, country).exists() &&
            packageFile(context, country).length() > 0L
    }

    fun deletePackage(context: Context, country: String) {
        packageFile(context, country).delete()
        manifestFile(context, country).delete()
    }

    fun listPackages(context: Context): List<RoutingPackage> {
        val dir = getDirectory(context)
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "pbf" }
            ?.map { file ->
                RoutingPackage(
                    country = file.nameWithoutExtension.removeSuffix(".routing"),
                    file = file
                )
            }
            ?: emptyList()
    }
}
