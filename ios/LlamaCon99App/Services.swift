import CallKit
import Contacts
import Foundation
import UIKit

/// Loads one contact's thumbnail at a time, on demand, so scrolling doesn't hold every photo's
/// `Data` in memory at once. Results are cached so scrolling back to a row doesn't re-fetch it.
enum ContactThumbnailLoader {
    private static let store = CNContactStore()
    private static let cache = NSCache<NSString, UIImage>()

    /// Runs the (synchronous, XPC-backed) Contacts lookup on a plain background queue rather
    /// than inline in the caller's `async` context.
    static func thumbnail(forContactID id: String) async -> UIImage? {
        if let cached = cache.object(forKey: id as NSString) {
            return cached
        }
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                let image = (try? store.unifiedContact(
                    withIdentifier: id,
                    keysToFetch: [CNContactThumbnailImageDataKey as CNKeyDescriptor]
                )).flatMap { $0.thumbnailImageData }.flatMap { UIImage(data: $0) }
                if let image {
                    cache.setObject(image, forKey: id as NSString)
                }
                continuation.resume(returning: image)
            }
        }
    }
}

/// Reads the full address book and rebuilds the CallerIDExtension's identification list from
/// it. Needs full Contacts access (`NSContactsUsageDescription`).
@Observable
final class ContactsService {
    private(set) var contacts: [DeviceContact] = []
    private(set) var isDenied = false
    /// True once the initial fetch has completed (with or without results) — lets the view tell
    /// "still loading" apart from "loaded, but no Cuban numbers found".
    private(set) var isLoaded = false

    private let store = CNContactStore()
    private var hasLoaded = false

    /// Requests access (once) and loads contacts. Safe to call from `onAppear` repeatedly.
    func loadIfNeeded() {
        guard !hasLoaded else { return }

        switch CNContactStore.authorizationStatus(for: .contacts) {
        case .authorized:
            hasLoaded = true
            isDenied = false
            fetch()
        case .notDetermined:
            hasLoaded = true
            store.requestAccess(for: .contacts) { [weak self] granted, _ in
                DispatchQueue.main.async {
                    if granted {
                        self?.isDenied = false
                        self?.fetch()
                    } else {
                        self?.isDenied = true
                        self?.isLoaded = true
                    }
                }
            }
        default:
            hasLoaded = true
            isDenied = true
            isLoaded = true
        }
    }

    /// Re-evaluates authorization status and re-fetches if permitted, or updates `isDenied`.
    func reload() {
        switch CNContactStore.authorizationStatus(for: .contacts) {
        case .authorized:
            isDenied = false
            hasLoaded = true
            fetch()
        case .notDetermined:
            hasLoaded = false
            isDenied = false
            loadIfNeeded()
        default:
            isDenied = true
            hasLoaded = true
            contacts = []
            isLoaded = true
        }
    }

    /// Triggers the system permission prompt if not determined yet, or opens system Settings if denied.
    func requestAccess() {
        if CNContactStore.authorizationStatus(for: .contacts) == .notDetermined {
            hasLoaded = false
            loadIfNeeded()
        } else if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    private func fetch() {
        let keys: [CNKeyDescriptor] = [
            CNContactFormatter.descriptorForRequiredKeys(for: .fullName),
            CNContactPhoneNumbersKey as CNKeyDescriptor,
        ]
        let request = CNContactFetchRequest(keysToFetch: keys)
        request.sortOrder = .givenName

        DispatchQueue.global(qos: .userInitiated).async { [store] in
            var results: [DeviceContact] = []
            try? store.enumerateContacts(with: request) { contact, _ in
                // Keep every Cuban number on the contact, not just the first — deduped in case
                // the same number is repeated under different labels.
                var seenNumbers = Set<String>()
                let cubanNumbers: [ContactPhoneNumber] = contact.phoneNumbers.compactMap { labeled in
                    guard let normalized = CubanPhoneNumber.normalize(labeled.value.stringValue),
                          seenNumbers.insert(normalized).inserted
                    else { return nil }
                    let label = labeled.label.map {
                        CNLabeledValue<NSString>.localizedString(forLabel: $0)
                    } ?? "otro"
                    return ContactPhoneNumber(label: label, number: normalized)
                }
                guard !cubanNumbers.isEmpty else { return }
                let name = CNContactFormatter.string(from: contact, style: .fullName) ?? String(localized: "Sin nombre")
                results.append(DeviceContact(
                    id: contact.identifier,
                    name: name,
                    numbers: cubanNumbers
                ))
            }
            let sorted = results.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
            Self.syncCallerIDExtension(with: sorted)
            DispatchQueue.main.async { [weak self] in
                self?.contacts = sorted
                self?.isLoaded = true
            }
        }
    }

    /// Rebuilds the `*99` collect-call caller-ID list (see `CallerIDStore`) from the freshly
    /// fetched contacts and asks CallKit to reload `CallerIDExtension` with it. No-op if the
    /// user has never enabled the extension in Ajustes del sistema.
    private static func syncCallerIDExtension(with contacts: [DeviceContact]) {
        // Register every Cuban number on each contact — CallKit requires every wrapped number
        // in the batch to be unique, so dedupe across the whole batch.
        var seenNumbers = Set<Int64>()
        let entries = contacts.flatMap { contact in
            contact.numbers.compactMap { number -> CallerIDEntry? in
                guard let wrapped = CallerIDStore.wrappedNumber(forLocalNumber: number.number),
                      seenNumbers.insert(wrapped).inserted else { return nil }
                return CallerIDEntry(wrappedNumber: wrapped, name: contact.name)
            }
        }
        CallerIDStore.write(entries)
        CXCallDirectoryManager.sharedInstance.reloadExtension(withIdentifier: CallerIDStore.extensionBundleID) { _ in }
    }
}

enum DialService {
    /// Builds a `tel://` URL for the given code. `#` must be percent-encoded or the URL is
    /// rejected by iOS.
    static func dialURL(for rawCode: String) -> URL? {
        let encoded = rawCode
            .replacingOccurrences(of: "#", with: "%23")
            .replacingOccurrences(of: " ", with: "")
        return URL(string: "tel://\(encoded)")
    }

    /// Hands the code to the system dialer. Returns false when the device cannot place calls
    /// (e.g. iPad, simulator).
    @discardableResult
    static func dial(_ rawCode: String) -> Bool {
        guard let url = dialURL(for: rawCode), UIApplication.shared.canOpenURL(url) else {
            return false
        }
        UIApplication.shared.open(url)
        return true
    }
}
