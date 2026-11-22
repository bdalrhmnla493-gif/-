package com.rafiq.quran.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TasbeehCounterTest {
    @Test
    fun incrementsFromZero() {
        assertEquals(1, TasbeehCounter.increment(0))
    }

    @Test
    fun preventsIntegerOverflow() {
        assertEquals(Int.MAX_VALUE, TasbeehCounter.increment(Int.MAX_VALUE))
    }

    @Test
    fun resetReturnsZero() {
        assertEquals(0, TasbeehCounter.reset())
    }
}
