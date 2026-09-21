import AppIntents
import Contacts

/// One dialable Cuban contact, exposed to Siri/Shortcuts so it can resolve "mamá", "mi hermano",
/// etc. against the same address book `ContactsService` reads — kept separate from
/// `DeviceContact` because Siri only needs a name and a single number to dial, not every label.
struct DialableContactEntity: AppEntity {
    let id: String
    let name: String
    let number: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Contacto"
    static var defaultQuery = DialableContactQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

struct DialableContactQuery: EntityQuery, EntityStringQuery {
    func entities(for identifiers: [DialableContactEntity.ID]) async -> [DialableContactEntity] {
        Self.fetchAll().filter { identifiers.contains($0.id) }
    }

    func entities(matching string: String) async -> [DialableContactEntity] {
        Self.fetchAll().filter { $0.name.localizedCaseInsensitiveContains(string) }
    }

    func suggestedEntities() async -> [DialableContactEntity] {
        Self.fetchAll()
    }

    /// Same `CNContactStore` query `ContactsService` runs, trimmed to one Cuban number per
    /// contact — Siri only needs somewhere to dial, not the full label list.
    private static func fetchAll() -> [DialableContactEntity] {
        guard CNContactStore.authorizationStatus(for: .contacts) == .authorized else { return [] }

        let keys: [CNKeyDescriptor] = [
            CNContactFormatter.descriptorForRequiredKeys(for: .fullName),
            CNContactPhoneNumbersKey as CNKeyDescriptor,
        ]
        let request = CNContactFetchRequest(keysToFetch: keys)
        var results: [DialableContactEntity] = []
        try? CNContactStore().enumerateContacts(with: request) { contact, _ in
            guard let number = contact.phoneNumbers.lazy
                .compactMap({ CubanPhoneNumber.normalize($0.value.stringValue) })
                .first
            else { return }
            let name = CNContactFormatter.string(from: contact, style: .fullName) ?? "Sin nombre"
            results.append(DialableContactEntity(id: contact.identifier, name: name, number: number))
        }
        return results
    }
}

/// "Llama con 99 a mamá" — dials `*99` + the contact's number, the same collect-call code the
/// main list's row tap uses, so the receiving end sees the wrapped caller ID.
struct CallWith99Intent: AppIntent {
    static var title: LocalizedStringResource = "Llamar con 99"
    static var description = IntentDescription(
        "Marca *99 seguido del número de un contacto para hacer una llamada por cobrar identificada."
    )

    @Parameter(title: "Contacto")
    var contact: DialableContactEntity

    static var parameterSummary: some ParameterSummary {
        Summary("Llamar con 99 a \(\.$contact)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard DialService.dial("*99\(contact.number)") else {
            throw $contact.needsValueError("No se pudo iniciar la llamada.")
        }
        return .result(dialog: "Llamando con 99 a \(contact.name)")
    }
}

struct LlamaCon99Shortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CallWith99Intent(),
            phrases: [
                "\(.applicationName) a \(\.$contact)",
                "\(.applicationName), pagando \(\.$contact)",
                "\(.applicationName), pagando a \(\.$contact)",
            ],
            shortTitle: "Llamar con 99",
            systemImageName: "phone.badge.checkmark"
        )
    }
}
