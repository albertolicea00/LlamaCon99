package com.llamacon99.data

import com.llamacon99.data.SettingsRepository.DialMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DialModePolicyTest {
    @Test
    fun `phone app stays selected regardless of SIM count`() {
        listOf(null, 0, 1, 2, 3).forEach { count ->
            assertEquals(DialMode.PREFILL, effectiveDialMode(DialMode.PREFILL, count))
        }
    }

    @Test
    fun `direct calls require exactly one verified active SIM`() {
        assertEquals(DialMode.DIRECT, effectiveDialMode(DialMode.DIRECT, 1))
        listOf(null, 0, 2, 3).forEach { count ->
            assertEquals(DialMode.PREFILL, effectiveDialMode(DialMode.DIRECT, count))
        }
    }
}
