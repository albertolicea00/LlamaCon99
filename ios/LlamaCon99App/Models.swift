import Foundation

/// Cuban mobile numbers only — ETECSA's `*99` collect-call service and the caller-ID wrap
/// (`CallerIDStore.wrappedNumber`) both assume an 8-digit local number under the `+53` country
/// code.
enum CubanPhoneNumber {
    /// Leading digit(s) a valid Cuban mobile number starts with, after the country code is
    /// stripped. Currently "5" or "6" — broad on purpose, narrow later if only some ranges
    /// under 6 turn out to be mobile.
    static let validMobilePrefixes = ["5", "6"]

    /// Strips formatting to bare digits, then accepts either the bare 8-digit local form or
    /// `+53` plus 8 digits — stripping the country code down to those 8 digits (e.g.
    /// "+53 5 123 4567" → "51234567") — and checks the result starts with an allowed prefix.
    /// Returns `nil` for anything else.
    static func normalize(_ rawNumber: String) -> String? {
        let digits = rawNumber.filter { $0.isASCII && $0.isNumber }

        let localNumber: String
        if digits.count == 8 {
            localNumber = digits
        } else if digits.count == 10, digits.hasPrefix("53") {
            localNumber = String(digits.dropFirst(2))
        } else {
            return nil
        }

        guard validMobilePrefixes.contains(where: localNumber.hasPrefix) else { return nil }
        return localNumber
    }
}

/// One Cuban number on a contact, with iOS's own label for it ("móvil", "trabajo", "iPhone"...).
struct ContactPhoneNumber: Hashable {
    let label: String
    let number: String
}

/// One entry from the device address book: just enough to list and dial it. Contacts can carry
/// several Cuban numbers (e.g. two SIMs, a landline plus a mobile) — `numbers` keeps all of
/// them, distinctly labeled.
struct DeviceContact: Identifiable, Hashable {
    let id: String
    let name: String
    let numbers: [ContactPhoneNumber]

    /// Primary number shown by default — first one found on the contact.
    var phoneNumber: String { numbers[0].number }
}
