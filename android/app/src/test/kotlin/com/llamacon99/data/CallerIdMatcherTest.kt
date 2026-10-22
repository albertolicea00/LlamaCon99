package com.llamacon99.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallerIdMatcherTest {

    @Test
    fun `reported incoming number preserves 53 at the start of the local number`() {
        assertEquals("53702413", CallerIdMatcher.unwrap("99535370241399"))
        assertEquals("53702413", CallerIdMatcher.unwrap("+99 53 53702413 99"))
        assertEquals(99535370241399L, CallerIdMatcher.wrappedNumber("53702413"))
        assertEquals("53702413", CubanPhoneNumber.normalize("+53 53702413"))
    }

    @Test
    fun `ordinary numbers are not treated as collect calls`() {
        assertNull(CallerIdMatcher.unwrap("+5353702413"))
        assertNull(CallerIdMatcher.unwrap("53702413"))
    }

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
