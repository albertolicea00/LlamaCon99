# Llama con 99

Source code for Llama con 99: a minimal contacts app with caller ID for saved contacts.

## What it does

Lists the device's contacts and identifies incoming calls from them, showing the contact's real name instead of just a number.

## Platforms

| Platform | Status | Path |
| --- | --- | --- |
| iOS | Done | [`ios/`](ios/) |
| Android | Not started | [`android/`](android/) |

### iOS

- `ContactsApp` — SwiftUI app listing contacts with a Cuban number (+53, 8 digits), grouped alphabetically with search. Tapping a contact dials a `*99` collect call; a `#31#` hidden-caller-ID swipe action can be enabled from Settings (off by default). Follows the device's language (Spanish source, English translation).
- `CallerIDExtension` — a CallKit Call Directory Extension that labels incoming `*99` collect calls with the real contact name, read from a list the main app writes via an App Group.

See [`ios/ARCHITECTURE.md`](ios/ARCHITECTURE.md) for build instructions and architecture.

### Android

Not started yet.

## Repo layout

```
code/
├── ios/        # iOS app + CallerID extension (XcodeGen project)
└── android/    # Android app (pending)
```
