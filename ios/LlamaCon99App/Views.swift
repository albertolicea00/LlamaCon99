import CallKit
import SwiftUI

/// The whole app: the device's address book, listed alphabetically with a search bar. Tapping
/// a row places a `*99` collect call (so the receiving end shows the wrapped caller ID that
/// CallerIDExtension unwraps back to the contact's real name); swiping reveals a `#31#` hidden
/// caller-ID call as a second option.
struct ContactsListView: View {
    @State private var service = ContactsService()
    @State private var searchText = ""
    @State private var showingInstallGuide = false
    @Environment(\.scenePhase) private var scenePhase

    private var entries: [ContactListEntry] {
        service.contacts.flatMap { contact in
            contact.numbers.map { ContactListEntry(contact: contact, number: $0) }
        }
    }

    private var filteredEntries: [ContactListEntry] {
        guard !searchText.isEmpty else { return entries }
        return entries.filter { entry in
            entry.contact.name.localizedCaseInsensitiveContains(searchText)
                || entry.number.number.localizedCaseInsensitiveContains(searchText)
        }
    }

    private var groupedEntries: [(letter: String, entries: [ContactListEntry])] {
        let groups = Dictionary(grouping: filteredEntries) { entry in
            String(entry.contact.name.prefix(1)).uppercased()
        }
        return groups.keys.sorted().map { letter in
            (letter, groups[letter]!.sorted {
                $0.contact.name.localizedCaseInsensitiveCompare($1.contact.name) == .orderedAscending
            })
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if service.isDenied {
                    ContentUnavailableView {
                        Label("Sin Acceso a Contactos", systemImage: "person.crop.circle.badge.exclamationmark")
                    } description: {
                        Text("Activa el permiso de Contactos para poder llamarlos directamente desde la app.")
                    } actions: {
                        Button("Permitir Acceso") { service.requestAccess() }
                            .buttonStyle(.borderedProminent)
                    }
                } else if !service.isLoaded {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if service.contacts.isEmpty {
                    ContentUnavailableView(
                        "Sin Contactos Cubanos",
                        systemImage: "person.crop.circle.badge.questionmark",
                        description: Text("No se encontró ningún contacto con número cubano (+53, 8 dígitos).")
                    )
                } else {
                    List {
                        ForEach(groupedEntries, id: \.letter) { group in
                            Section(group.letter) {
                                ForEach(group.entries) { entry in
                                    ContactCallRowView(entry: entry)
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .searchable(text: $searchText, prompt: "Buscar")
                }
            }
            .navigationTitle("Contactos")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showingInstallGuide = true
                    } label: {
                        Image(systemName: "info.circle")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .sheet(isPresented: $showingInstallGuide) {
            InstallGuideView()
        }
        .onAppear { service.reload() }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                service.reload()
            }
        }
    }
}

/// One contact + one of its Cuban numbers — a contact with several lines yields several entries.
private struct ContactListEntry: Identifiable, Hashable {
    let contact: DeviceContact
    let number: ContactPhoneNumber

    var id: String { "\(contact.id)-\(number.number)" }
}

/// One row per contact number: photo, name + labeled number. Tapping dials a `*99` collect
/// call; swiping trailing offers the same, leading offers a `#31#` hidden-caller-ID call.
private struct ContactCallRowView: View {
    let entry: ContactListEntry

    private var number: String { entry.number.number }

    var body: some View {
        HStack(spacing: 12) {
            ContactAvatarView(contact: entry.contact)

            VStack(alignment: .leading, spacing: 2) {
                Text(entry.contact.name)
                    .font(.body.weight(.medium))
                (Text(number)
                    + Text(entry.contact.numbers.count > 1 ? " (\(entry.number.label))" : "")
                        .font(.caption)
                        .italic())
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Image(systemName: "phone.fill")
                .foregroundStyle(.tint)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
        .onTapGesture {
            DialService.dial("*99\(number)")
        }
        .swipeActions(edge: .leading) {
            Button {
                DialService.dial("#31#\(number)")
            } label: {
                Label("Anónimo", systemImage: "shield.lefthalf.filled")
            }
            .tint(.gray)
        }
    }
}

/// Round contact photo pulled from the device address book, falling back to the contact's
/// initials on a tinted circle when there's no photo.
private struct ContactAvatarView: View {
    let contact: DeviceContact
    var size: CGFloat = 40

    @State private var uiImage: UIImage?

    private var initials: String {
        let words = contact.name.split(separator: " ")
        let letters = words.prefix(2).compactMap { $0.first }
        return letters.isEmpty ? "?" : String(letters).uppercased()
    }

    var body: some View {
        Group {
            if let uiImage {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
            } else {
                ZStack {
                    Color.accentColor.opacity(0.2)
                    Text(initials)
                        .font(.system(size: size * 0.4, weight: .semibold))
                        .foregroundStyle(Color.accentColor)
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .task(id: contact.id) {
            uiImage = await ContactThumbnailLoader.thumbnail(forContactID: contact.id)
        }
    }
}

/// Bottom-sheet walkthrough for the one step iOS never lets an app do for itself: enabling the
/// CallerID Call Directory Extension under Settings > Phone.
private struct InstallGuideView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase
    @State private var status: CXCallDirectoryManager.EnabledStatus = .unknown
    @State private var isChecking = true

    private let enableSteps: [(icon: String, title: String, detail: String)] = [
        ("gear", "Abre Ajustes", "Ve a la app de Ajustes de tu iPhone."),
        ("phone.fill", "Entra a Teléfono", "Baja hasta encontrar Teléfono y tócalo."),
        ("person.crop.circle.badge.checkmark", "Identificación y bloqueo de llamadas",
         "Dentro de Teléfono, entra a esta sección."),
        ("switch.2", "Activa CallerID", "Enciende el interruptor junto a CallerID, la extensión de esta app."),
        ("checkmark.seal.fill", "Listo", "Ya verás el nombre real del contacto en llamadas entrantes desde números cubanos guardados."),
    ]

    private let disableSteps: [(icon: String, title: String, detail: String)] = [
        ("gear", "Abre Ajustes", "Ve a la app de Ajustes de tu iPhone."),
        ("phone.fill", "Entra a Teléfono", "Baja hasta encontrar Teléfono y tócalo."),
        ("person.crop.circle.badge.checkmark", "Identificación y bloqueo de llamadas",
         "Dentro de Teléfono, entra a esta sección."),
        ("switch.2", "Apaga CallerID", "Apaga el interruptor junto a CallerID, la extensión de esta app."),
        ("checkmark.seal.fill", "Listo", "Dejarás de ver el nombre del contacto en llamadas *99, solo el número envuelto."),
    ]

    private var steps: [(icon: String, title: String, detail: String)] {
        status == .enabled ? disableSteps : enableSteps
    }

    var body: some View {
        NavigationStack {
            List {
                Section("Estado de la Extensión") {
                    HStack(spacing: 12) {
                        Image(systemName: statusIcon)
                            .font(.title3)
                            .foregroundStyle(statusColor)

                        VStack(alignment: .leading, spacing: 3) {
                            Text(statusTitle)
                                .font(.subheadline.weight(.semibold))
                            Text(statusSubtitle)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        Spacer()

                        if isChecking {
                            ProgressView()
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section(status == .enabled ? "Cómo Desactivarla" : "Cómo Activarla") {
                    ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("\(index + 1). \(step.title)")
                                    .font(.body.weight(.semibold))
                                Text(step.detail)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: step.icon)
                                .foregroundStyle(.tint)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .navigationTitle("Cómo Activar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Listo") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .onAppear { checkStatus() }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                checkStatus()
            }
        }
    }

    private var statusTitle: String {
        switch status {
        case .enabled: return "Extensión Activada"
        case .disabled: return "Extensión Desactivada"
        default: return "Estado No Disponible"
        }
    }

    private var statusSubtitle: String {
        switch status {
        case .enabled: return "Las llamadas *99 mostrarán el nombre de tu contacto."
        case .disabled: return "Actívala en Ajustes › Teléfono para que funcione."
        default: return "Solo verificable en un iPhone físico real."
        }
    }

    private var statusIcon: String {
        switch status {
        case .enabled: return "checkmark.circle.fill"
        case .disabled: return "exclamationmark.triangle.fill"
        default: return "questionmark.circle.fill"
        }
    }

    private var statusColor: Color {
        switch status {
        case .enabled: return .green
        case .disabled: return .orange
        default: return .secondary
        }
    }

    private func checkStatus() {
        isChecking = true
        CXCallDirectoryManager.sharedInstance.getEnabledStatusForExtension(withIdentifier: CallerIDStore.extensionBundleID) { newStatus, _ in
            DispatchQueue.main.async {
                self.status = newStatus
                self.isChecking = false
            }
        }
    }
}

#Preview {
    ContactsListView()
}
