package com.llamacon99.model

/** One Cuban number on a contact, with the device's own label for it ("móvil", "trabajo"...). */
data class ContactPhoneNumber(
    val label: String,
    val number: String,
)
