package com.llamacon99.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallerIdMatcherTest {

    @Test
    fun `wraps local number into etecsa format`() {
        assertEquals(99535123456799L, CallerIdMatcher.wrappedNumber("51234567"))
    }

    @Test
    fun `unwrap is the exact inverse of wrappedNumber`() {
        val local = "51234567"
        val wrapped = CallerIdMatcher.wrappedNumber(local)!!
        assertEquals(local, CallerIdMatcher.unwrap(wrapped.toString()))
    }

    @Test
    fun `unwrap rejects wrong length`() {
        assertNull(CallerIdMatcher.unwrap("995351234567"))
        assertNull(CallerIdMatcher.unwrap("9953512345679900"))
    }

    @Test
    fun `unwrap rejects wrong prefix or suffix`() {
        assertNull(CallerIdMatcher.unwrap("12535123456799"))
        assertNull(CallerIdMatcher.unwrap("99535123456712"))
    }

    @Test
    fun `unwrap tolerates formatting characters`() {
        assertEquals("51234567", CallerIdMatcher.unwrap("+99535123456799"))
    }

    @Test
    fun `unwrap rejects malformed non numeric string`() {
        assertNull(CallerIdMatcher.unwrap("not-a-number"))
    }
}
