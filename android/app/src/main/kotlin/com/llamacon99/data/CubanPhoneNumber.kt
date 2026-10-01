package com.llamacon99.data

/**
 * Cuban mobile numbers only — ETECSA's `*99` collect-call service and the caller-ID wrap
 * ([CallerIdMatcher.wrappedNumber]) both assume an 8-digit local number under the `+53` country
 * code. Direct port of iOS's `CubanPhoneNumber` (`ios/LlamaCon99/Models.swift`).
 */
object CubanPhoneNumber {

    /**
     * Leading digit(s) a valid Cuban mobile number starts with, after the country code is
     * stripped. Currently "5" or "6" — broad on purpose, narrow later if only some ranges under 6
     * turn out to be mobile.
     */
    val validMobilePrefixes = listOf("5", "6")

    /**
     * Strips formatting to bare digits, then accepts either the bare 8-digit local form or `+53`
     * plus 8 digits — stripping the country code down to those 8 digits (e.g. "+53 5 123 4567" ->
     * "51234567") — and checks the result starts with an allowed prefix. Returns `null` for
     * anything else.
     */
    fun normalize(rawNumber: String): String? {
        val digits = rawNumber.filter { it.isDigit() }

        val localNumber = when {
            digits.length == 8 -> digits
            digits.length == 10 && digits.startsWith("53") -> digits.drop(2)
            else -> return null
        }

        return localNumber.takeIf { local -> validMobilePrefixes.any { local.startsWith(it) } }
    }
}
