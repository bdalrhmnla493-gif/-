package com.rafiq.quran.domain

internal object TasbeehCounter {
    fun increment(current: Int): Int {
        val safeCurrent = current.coerceAtLeast(0)
        return if (safeCurrent == Int.MAX_VALUE) Int.MAX_VALUE else safeCurrent + 1
    }

    fun reset(): Int = 0
}
