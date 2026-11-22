package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EgyptLocationsRepositoryTest {
    @Test
    fun coordinateValidationAcceptsEarthBounds() {
        assertTrue(EgyptLocationsRepository.isValidCoordinates(30.0444, 31.2357))
        assertTrue(EgyptLocationsRepository.isValidCoordinates(90.0, 180.0))
        assertTrue(EgyptLocationsRepository.isValidCoordinates(-90.0, -180.0))
    }

    @Test
    fun coordinateValidationRejectsInvalidValues() {
        assertFalse(EgyptLocationsRepository.isValidCoordinates(91.0, 31.0))
        assertFalse(EgyptLocationsRepository.isValidCoordinates(30.0, 181.0))
        assertFalse(EgyptLocationsRepository.isValidCoordinates(Double.NaN, 31.0))
        assertFalse(EgyptLocationsRepository.isValidCoordinates(30.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun nearestAreaFindsNearestAndRejectsFarAwayLocations() {
        val area = EgyptAdministrativeArea(
            id = "EG0001",
            name = "منطقة اختبار",
            unitType = "مركز",
            latitude = 30.0444,
            longitude = 31.2357
        )
        val governorate = EgyptGovernorate(
            id = "EG01",
            name = "القاهرة",
            englishName = "Cairo",
            latitude = 30.0444,
            longitude = 31.2357,
            areas = listOf(area)
        )

        val nearest = EgyptLocationsRepository.findNearestArea(
            listOf(governorate), 30.0450, 31.2350
        )
        assertEquals("EG0001", nearest?.second?.id)
        assertNull(EgyptLocationsRepository.findNearestArea(listOf(governorate), 25.0, 31.0))
    }
}
