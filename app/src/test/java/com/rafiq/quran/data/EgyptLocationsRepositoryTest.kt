package com.rafiq.quran.data

import org.junit.Assert.assertFalse
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
}
