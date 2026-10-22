package com.llamacon99.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticUpdateScheduleTest {
    private val day = 24 * 60 * 60 * 1_000L
    private val now = 10 * day

    @Test
    fun `first launch checks immediately`() {
        assertEquals(0L, nextAutomaticUpdateCheckDelay(null, now))
    }

    @Test
    fun `reopening before a day waits only the remaining time`() {
        assertEquals(day / 4, nextAutomaticUpdateCheckDelay(now - day * 3 / 4, now))
    }

    @Test
    fun `checks again at exactly twenty four hours`() {
        assertEquals(0L, nextAutomaticUpdateCheckDelay(now - day, now))
    }

    @Test
    fun `returning after several days checks immediately`() {
        assertEquals(0L, nextAutomaticUpdateCheckDelay(now - 3 * day, now))
    }

    @Test
    fun `moving clock backwards does not suppress updates`() {
        assertEquals(0L, nextAutomaticUpdateCheckDelay(now + day, now))
    }
}
