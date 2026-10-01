package com.llamacon99.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CubanPhoneNumberTest {

    @Test
    fun `8 digit local number with valid mobile prefix normalizes as is`() {
        assertEquals("51234567", CubanPhoneNumber.normalize("51234567"))
        assertEquals("61234567", CubanPhoneNumber.normalize("61234567"))
    }

    @Test
    fun `plus 53 with 10 digits strips country code`() {
        assertEquals("51234567", CubanPhoneNumber.normalize("+53 5 123 4567"))
        assertEquals("51234567", CubanPhoneNumber.normalize("5351234567"))
    }

    @Test
    fun `invalid leading prefix is rejected`() {
        assertNull(CubanPhoneNumber.normalize("71234567"))
        assertNull(CubanPhoneNumber.normalize("5371234567"))
    }

    @Test
    fun `wrong digit count is rejected`() {
        assertNull(CubanPhoneNumber.normalize("1234567"))
        assertNull(CubanPhoneNumber.normalize("123456789"))
        assertNull(CubanPhoneNumber.normalize(""))
    }

    @Test
    fun `non numeric characters are stripped before validation`() {
        assertEquals("51234567", CubanPhoneNumber.normalize("+53-5-123-4567"))
    }

    @Test
    fun `10 digits not prefixed by country code is rejected`() {
        assertNull(CubanPhoneNumber.normalize("1234567890"))
    }
}
