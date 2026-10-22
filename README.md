# Llama con 99

Source code for Llama con 99: a minimal contacts app with caller ID for saved contacts.

## What it does

Lists the device's contacts and identifies incoming calls from them, showing the contact's real name instead of just a number.

## Platforms

| Platform | Status | Path |
| --- | --- | --- |
| iOS | Done | [`ios/`](ios/) |
| Android | Done | [`android/`](android/) |

### iOS

- `ContactsApp` — SwiftUI app listing contacts with a Cuban number (+53, 8 digits), grouped alphabetically with search. Tapping a contact dials a `*99` collect call; a `#31#` hidden-caller-ID swipe action can be enabled from Settings (off by default). Follows the device's language (Spanish source, English translation).
- `CallerIDExtension` — a CallKit Call Directory Extension that labels incoming `*99` collect calls with the real contact name, read from a list the main app writes via an App Group.

See [`ios/ARCHITECTURE.md`](ios/ARCHITECTURE.md) for build instructions and architecture.

### Android

Native Kotlin and Jetpack Compose app. Includes Cuban contacts, anonymous swipe-to-call, and incoming caller ID. For build, test, and optimized release APK export instructions, see [`android/ARCHITECTURE.md`](android/ARCHITECTURE.md) or run `make help` from the repository root.

## Known limitations

**Android Firebase telemetry from Cuba — verified on 2026-10-22.** The app targets Cuban users.
On a Samsung SM-A155M in Cuba without VPN, Analytics successfully sent the test event (HTTP 204),
but Crashlytics and Performance Monitoring uploads were rejected (HTTP 403). With VPN enabled,
both services successfully sent their test data (HTTP 200), and Analytics continued to work.
Crash and performance reports may therefore be missing for users connecting from Cuba without
VPN. These results describe the tested connection; they do not establish behavior on every Cuban
network. See [Android architecture](android/ARCHITECTURE.md#verified-delivery-and-cuba-network-limitation-2026-10-22)
for verification details.

## Repo layout

```
code/
├── ios/        # iOS app + CallerID extension (XcodeGen project)
├── android/    # Android app, architecture, and build guide
└── Makefile    # Android build commands
```
