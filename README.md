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

### Android themes

Choose a visual style from **Settings → Customization**. All themes support light and dark mode and retain the brand green. They change screen structure, controls, and visual navigation.

| Theme | Inspired by | Style |
| --- | --- | --- |
| SamUI | Samsung One UI | Large titles, rounded groups, and thumb-friendly controls. |
| ManzanaUI | Apple iOS | Continuous list, collapsing header, bottom search, and a side A–Z index. |
| MaterialUI | Google Pixel / Material You (Material 3) | Top pill-shaped search and open contact rows with avatars. |
| HorizonUI | Huawei HarmonyOS | Top search and modular contact groups. |
| HyperUI | Xiaomi HyperOS | Large header, top search, and a compact list with a side index. |
| MonoUI | Nothing OS | A monochrome base with the app’s green accent, technical typography, a dot pattern, and a side index. |
| FluentUI | Microsoft Fluent Design | Command bar, top search, a list panel, and sidebar navigation on wide screens. |

## Known limitations

**Android Firebase telemetry from Cuba — verified on 2026-10-22.** The app targets Cuban users.
On a Samsung SM-A155M in Cuba without VPN, Analytics successfully sent the test event (HTTP 204),
but Crashlytics and Performance Monitoring uploads were rejected (HTTP 403). With VPN enabled,
both services successfully sent their test data (HTTP 200), and Analytics continued to work.
The Android app now retains only Analytics; Crashlytics and Performance Monitoring are commented
out for future reactivation, so crash and performance reports are no longer collected. These results describe the
tested connection; they do not establish behavior on every Cuban network. See [Android architecture](android/ARCHITECTURE.md#verified-delivery-and-cuba-network-limitation-2026-10-22)
for verification details and reactivation instructions.

## Repo layout

```
code/
├── ios/        # iOS app + CallerID extension (XcodeGen project)
├── android/    # Android app, architecture, and build guide
└── Makefile    # Android build commands
```
