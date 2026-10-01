package com.llamacon99.model

/**
 * One entry [com.llamacon99.monitor.CallMonitorService] matches against: a `*99` collect call's
 * wrapped caller ID (see [com.llamacon99.data.CallerIdMatcher]) mapped to the real contact's name.
 */
data class CallerIdEntry(
    val wrappedNumber: Long,
    val name: String,
)
