package com.llamacon99.model

/**
 * One entry from the device address book: just enough to list and dial it. Contacts can carry
 * several Cuban numbers (e.g. two SIMs, a landline plus a mobile) — [numbers] keeps all of them,
 * distinctly labeled.
 */
data class DeviceContact(
    val id: String,
    val name: String,
    val numbers: List<ContactPhoneNumber>,
) {
    /** Primary number shown by default — first one found on the contact. */
    val phoneNumber: String get() = numbers[0].number
}
