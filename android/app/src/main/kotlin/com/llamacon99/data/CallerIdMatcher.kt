package com.llamacon99.data

/**
 * Wraps/unwraps ETECSA's `*99` collect-call caller ID format. Direct port of iOS's
 * `CallerIDStore.wrappedNumber` (`ios/Shared/CallerIDStore.swift`), minus the App Group / Call
 * Directory Extension IPC — on Android there's no sandboxed extension process, so
 * [com.llamacon99.monitor.CallMonitorService] reads [com.llamacon99.data.CallerIdStore] directly,
 * in the same process.
 */
object CallerIdMatcher {

    private const val PREFIX = "9953"
    private const val SUFFIX = "99"
    private const val LOCAL_NUMBER_LENGTH = 8
    private const val WRAPPED_LENGTH = PREFIX.length + LOCAL_NUMBER_LENGTH + SUFFIX.length

    /**
     * ETECSA's `*99` collect-call service shows the incoming caller ID wrapped as `99` + country
     * code (`53`) + the 8-digit local number + `99` — e.g. `51234567` becomes `99535123456799`.
     * [localNumber] must already be the normalized 8-digit form [CubanPhoneNumber.normalize]
     * produces.
     */
    fun wrappedNumber(localNumber: String): Long? =
        (PREFIX + localNumber + SUFFIX).toLongOrNull()

    /**
     * Inverse of [wrappedNumber]: validates the `"99" + "53" + 8 digits + "99"` shape and returns
     * the 8-digit local number, or `null` if [wrapped] doesn't match that shape.
     */
    fun unwrap(wrapped: String): String? {
        val digits = wrapped.filter { it.isDigit() }
        if (digits.length != WRAPPED_LENGTH) return null
        if (!digits.startsWith(PREFIX) || !digits.endsWith(SUFFIX)) return null
        return digits.substring(PREFIX.length, PREFIX.length + LOCAL_NUMBER_LENGTH)
    }
}
