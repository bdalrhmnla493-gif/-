package com.rafiq.quran.data

import android.content.Context
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class EgyptAdministrativeArea(
    val id: String,
    val name: String,
    val unitType: String,
    val latitude: Double,
    val longitude: Double
)

data class EgyptGovernorate(
    val id: String,
    val name: String,
    val englishName: String,
    val latitude: Double,
    val longitude: Double,
    val areas: List<EgyptAdministrativeArea>
)

object EgyptLocationsRepository {
    fun load(context: Context): List<EgyptGovernorate> {
        val content = context.assets.open("egypt-locations.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parse(content)
    }

    internal fun parse(content: String): List<EgyptGovernorate> {
        val root = JSONObject(content)
        val sourceGovs = root.getJSONArray("governorates")
        val result = ArrayList<EgyptGovernorate>(sourceGovs.length())

        for (index in 0 until sourceGovs.length()) {
            val governorate = sourceGovs.getJSONObject(index)
            val sourceAreas = governorate.getJSONArray("areas")
            val areas = ArrayList<EgyptAdministrativeArea>(sourceAreas.length())

            for (areaIndex in 0 until sourceAreas.length()) {
                val area = sourceAreas.getJSONObject(areaIndex)
                val latitude = area.getDouble("latitude")
                val longitude = area.getDouble("longitude")
                requireCoordinates(latitude, longitude)
                areas.add(
                    EgyptAdministrativeArea(
                        id = area.getString("id"),
                        name = area.getString("name"),
                        unitType = area.getString("unitType"),
                        latitude = latitude,
                        longitude = longitude
                    )
                )
            }

            val latitude = governorate.getDouble("latitude")
            val longitude = governorate.getDouble("longitude")
            requireCoordinates(latitude, longitude)
            result.add(
                EgyptGovernorate(
                    id = governorate.getString("id"),
                    name = governorate.getString("name"),
                    englishName = governorate.getString("englishName"),
                    latitude = latitude,
                    longitude = longitude,
                    areas = areas
                )
            )
        }

        require(result.size == 27) { "بيانات المحافظات المصرية غير مكتملة." }
        require(result.sumOf { it.areas.size } == 348) { "بيانات المراكز والأقسام المصرية غير مكتملة." }
        return result
    }

    fun findNearestArea(
        governorates: List<EgyptGovernorate>,
        latitude: Double,
        longitude: Double
    ): Pair<EgyptGovernorate, EgyptAdministrativeArea>? {
        if (!isValidCoordinates(latitude, longitude)) return null
        var nearest: Pair<EgyptGovernorate, EgyptAdministrativeArea>? = null
        var nearestDistance = Double.MAX_VALUE

        governorates.forEach { governorate ->
            governorate.areas.forEach { area ->
                val distance = distanceKm(latitude, longitude, area.latitude, area.longitude)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearest = governorate to area
                }
            }
        }
        return nearest?.takeIf { nearestDistance <= 50.0 }
    }

    internal fun isValidCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun requireCoordinates(latitude: Double, longitude: Double) {
        require(isValidCoordinates(latitude, longitude)) { "إحداثيات موقع غير صالحة." }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val first = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(sqrt(first), sqrt(1 - first))
    }
}
