# Llama con 99 [Android]

## Overview

LlamaCon99 is a Kotlin and Jetpack Compose app for Cuban mobile contacts. It lets users find a
contact, place an ETECSA `*99` collect call, optionally place an anonymous `#31#` call, and identify
incoming `*99` calls from saved contacts with a notification. The app does not act as the default
dialer, manage calls, or use a backend for contact matching. The incoming-call monitor is an
explicit opt-in and runs as a foreground service.

The Android app follows the same product behavior as the iOS app where the platforms allow it,
but Android has no Call Directory Extension equivalent. See [`../ios/ARCHITECTURE.md`](../ios/ARCHITECTURE.md)
for the iOS-specific CallKit design.

## Find code quickly

| If you need to change… | Start here |
| --- | --- |
| App startup, edge-to-edge window, root theme | `app/src/main/kotlin/com/llamacon99/MainActivity.kt` |
| Screen destinations and screen analytics | `ui/navigation/NavGraph.kt` |
| Contacts loading, permission state, search, list and row interactions | `ui/contacts/` |
| Cuban number validation and `*99` number wrapping | `data/CubanPhoneNumber.kt`, `data/CallerIdMatcher.kt` |
| Contact provider query and caller-ID table refresh | `data/ContactsRepository.kt`, `data/CallerIdStore.kt` |
| Dial prefixes and launching the phone app | `ui/contacts/ContactsListViewModel.kt`, `data/DialRepository.kt` |
| Active SIM detection and direct-call availability | `data/SimRepository.kt`, `data/SettingsRepository.kt` |
| Preferences and persisted app settings | `data/SettingsRepository.kt`, `di/DataStoreModule.kt` |
| Caller-ID monitoring lifecycle and telephony events | `monitor/CallMonitorService.kt` |
| Incoming-call notification content and channels | `monitor/CallerIdNotifier.kt` |
| Restarting monitoring after reboot | `monitor/BootCompletedReceiver.kt`, `AndroidManifest.xml` |
| Settings rows, bottom sheets, and permission prompts | `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt` |
| App appearance profiles and component geometry | `ui/theme/AppThemeCatalog.kt`, `ui/theme/Theme.kt` |
| Welcome reminder | `ui/welcome/WelcomeSheet.kt`, `ui/welcome/WelcomeViewModel.kt` |
| GitHub release check and update prompt | `service/UpdateChecker.kt`, `ui/components/UpdateAvailableDialog.kt` |
| Firebase Analytics | `telemetry/AppTelemetry.kt`, `app/build.gradle.kts` |
| Launcher shortcuts | `res/xml/shortcuts.xml`, `MainActivity.kt` |

## Project layout

```text
app/src/main/
  kotlin/com/llamacon99/
    MainActivity.kt                 Activity and Compose entry point
    LlamaCon99Application.kt        Hilt and telemetry initialization
    data/                           Android providers, persistence, dialing, number rules
    di/                             Dependency injection modules
    model/                          Contact and caller-ID data models
    monitor/                        Foreground monitor, notifications, boot receiver
    service/                        GitHub update lookup
    telemetry/                      Firebase wrapper and opt-in configuration check
    ui/
      components/                   Shared dialogs
      contacts/                     Contacts screen, rows, search, scroll controls
      navigation/                   Navigation graph and screen-view tracking
      settings/                     Preferences and selection sheets
      theme/                        Brand palette, theme catalog, shapes and typography
      welcome/                      First-launch and recurring setup reminder
  res/                              Android strings, launcher assets, shortcuts and themes
app/src/test/kotlin/                Local JVM tests for number normalization and matching
build.gradle.kts                    Android module, dependencies, signing and release options
```

The app uses a small MVVM-style split: Compose screens render state and send user actions to
ViewModels; repositories own Android APIs and persistence. Hilt supplies shared repositories.
Settings and caller-ID entries are stored in Preferences DataStore; the device Contacts Provider is
the source of contact names, phone labels, and photos.

## Contact list and dialing

`ContactsRepository.reload()` checks `READ_CONTACTS`, queries `ContactsContract` on `Dispatchers.IO`,
normalizes phone numbers, removes duplicate numbers within each contact, and retains contacts with at
least one supported Cuban mobile number. It publishes the result as a `StateFlow` and replaces the
caller-ID lookup table at the same time. Without contact permission, both the list and lookup table
are empty. `ContactsListScreen` requests permission and shows the permission explanation state when
access is missing.

`ContactsListViewModel` filters contacts by name or normalized number, groups them alphabetically,
and exposes one `ContactsListUiState`. The Compose list, search field, voice-search action, alphabet
scrollbar, current-letter bubble, and scroll-to-top control are implemented under `ui/contacts/`.
`SwipeableContactRow` only exposes its anonymous-call action when that preference is enabled.

Contact taps dial `*99` followed by the selected local number. The dial mode is configurable:

- **Prefill** is the default. It opens the user's phone app with the number entered (`ACTION_DIAL`),
  where the user confirms the call. It does not need `CALL_PHONE`.
- **Direct** uses `ACTION_CALL` only with `CALL_PHONE` permission and exactly one verified active
  SIM. `READ_PHONE_STATE` is requested when enabling it so `SimRepository` can count active
  subscriptions rather than physical slots. If the count is unavailable, zero, or greater than one,
  or call permission is missing, `DialRepository` falls back to `ACTION_DIAL` before placing a call.
- With two or more active SIMs, the direct option is disabled and the effective mode is **Phone app**.
  If Direct was selected, `SettingsRepository` saves Prefill and shows an informational dialog once
  explaining that the user should choose the SIM in the phone app. Removing the second SIM does
  not automatically re-enable Direct. SIM changes are observed during the process lifetime and
  refreshed on resume and immediately before a direct call. The same guard covers anonymous calls
  and launcher shortcuts. Prefill remains the default on a fresh installation.

Anonymous calls add `#31#` before the number. Dynamic shortcut intents pass `dial_mode=collect` or
`dial_mode=anonymous` through `MainActivity`; shortcut resources and labels live in
`res/xml/shortcuts.xml` and `res/values*/strings.xml`.

## Incoming caller ID

ETECSA's `*99` collect-call format transforms an eight-digit local number into a displayed incoming
ID of `99` + `53` + the local number + `99`. `CubanPhoneNumber.normalize()` accepts either the
eight-digit local number or a ten-digit number beginning with country code `53`, then permits
mobile prefixes `5` and `6`. `CallerIdMatcher` creates and validates the wrapped form.

The data and event flow is:

```text
ContactsRepository.reload()
  -> ContactsContract query
  -> CubanPhoneNumber.normalize()
  -> CallerIdMatcher.wrappedNumber()
  -> CallerIdStore.replaceAll() in Preferences DataStore

CallMonitorService (foreground)
  -> listens for ringing state and incoming number
  -> CallerIdMatcher.unwrap()
  -> CallerIdLookup.nameFor() queries live Contacts, then the cached CallerIdStore
  -> CallerIdNotifier.showIncomingCallBanner(contact name or unsaved label, real number)
  -> CallerIdOverlay.show() if display-over-other-apps permission is granted
```

Unlike iOS, Android has no separate Call Directory Extension or App Group boundary here: the
service and app share the same process and DataStore. The service is started after the user
enables caller-ID monitoring in Settings, and restored when the app opens with that opt-in saved.
`READ_PHONE_STATE`, `READ_CALL_LOG`, and, on Android 13+,
`POST_NOTIFICATIONS` are required. The manifest declares the foreground service as `specialUse`
with a caller-ID monitoring subtype; keep its type and subtype aligned if the service's purpose ever
changes. It is passive caller identification, not a phone-call service and not a default dialer.

The notification includes both the identity and the recovered number; a valid collect call with
no matching contact still shows its real number. For `99535370241399`, remove only the `9953`
prefix and `99` suffix: the local number is `53702413` (international `+53 53702413`). Do not strip
another `53` from a local number that happens to begin with those digits.

Heads-up notification display depends on Android and channel settings; posting a notification
does not guarantee that it appears over the system dialer. Settings > Show over the call opens
the optional `SYSTEM_ALERT_WINDOW` permission screen. When granted, `CallerIdOverlay` shows a
small dismissible name/number card while ringing. It does not change the system dialer's number
or cover the answer/hang-up controls. Secure system screens may suppress overlays. Both the
notification and overlay are cleared when ringing ends or the service stops; the card also has
a two-minute safety timeout. Duplicate updates for the same ringing number do not repost it.

`BootCompletedReceiver` receives boot events and checks both saved opt-ins (“monitor calls” and
“monitor from boot”) and the required permissions before restarting the
service. If the service cannot start, the ViewModel/service logs the failure, clears the enabled
setting, and reports the problem to the user.

## Screens, preferences, and themes

`NavGraph.kt` has two destinations: `contacts` and `settings`. The welcome reminder is a bottom sheet
over the contacts destination; it appears on first launch and weekly while monitoring remains off.
Settings includes dialing behavior, anonymous swipe, caller-ID monitoring and its permissions,
monitor-on-boot, appearance, update checking, app version, and legal links. Theme and dial-mode
choices use bottom sheets implemented in `SettingsScreen.kt`.

`AppThemeCatalog` is the registry for appearance profiles. A profile has an ID, localized name and
inspiration description, Material shapes, and component-specific measurements for contact rows,
search placement, and the scrollbar. UI components read the profile through `LocalAppTheme`; add a
theme by registering another `AppThemeDefinition` and providing its string resources, then extend
component behavior only where the catalog needs a new reusable property. Avoid spreading theme-ID
conditionals across screens. Brand colors remain the same across profiles; themes change component
geometry and layout. `SettingsRepository` persists only the profile ID and falls back to the catalog
default if an unknown value is encountered.

## Localization and telemetry

Android UI strings are resource-backed: Spanish defaults in `res/values/strings.xml`, English
translations in `res/values-en/strings.xml`. Add or update both when adding user-visible copy.

`AppTelemetry` is a narrow wrapper around Firebase Analytics. It stays dormant unless Firebase
has initialized from this app's own `google-services.json`. Navigation logs screen views and
initialization sets the `build_type` user property. Crashlytics and Performance Monitoring have
been disabled by commenting out their SDK dependencies, Gradle plugins, traces, and synthetic error tests,
because their uploads failed from Cuba without VPN in the test below. Contact refresh failures
are logged locally with Android Logcat; they are not sent to Firebase.
Never copy another app's Firebase configuration into this project. Add LlamaCon99's configuration at
`app/google-services.json`; without it, the app can build but Firebase reporting is inactive.

### Firebase smoke test (debug builds only)

`src/debug/` includes an Analytics-only ADB test receiver protected by `android.permission.DUMP`.
It is excluded from release builds. With a debug APK installed, open the app, then run:

```sh
adb shell setprop debug.firebase.analytics.app com.llamacon99
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb shell am broadcast -n com.llamacon99/.debug.TelemetryTestReceiver --es kind event --es test_id manual-check
adb logcat -d -s TelemetryTest FA FA-SVC
adb shell setprop debug.firebase.analytics.app .none.
```

The event is named `telemetry_test` and includes its test ID. Local event logging alone does not
confirm delivery; check the Analytics upload response and matching event in Firebase DebugView.

### Verified delivery and Cuba network limitation (2026-10-22)

The Firebase configuration targets project `llamacon99`, package `com.llamacon99`. On 2026-10-22,
debug build 1.0.1 passed compilation and JVM tests on the development machine; delivery was tested
on a Samsung SM-A155M in Cuba before Crashlytics and Performance were commented out:

| Service | Without VPN | With VPN |
| --- | --- | --- |
| Analytics | Test event upload accepted: HTTP 204 | Test event upload accepted: HTTP 204 |
| Crashlytics | Report upload rejected: HTTP 403 | Synthetic nonfatal report upload accepted: HTTP 200 |
| Performance Monitoring | Metric upload rejected: HTTP 403; Remote Config reported an authorization failure | Remote Config recovered; test trace upload accepted: HTTP 200 |

The VPN retest used `vpn_20261022_01` for the Analytics event and nonfatal error, and
`vpn_20261022_02` for the Performance trace. Crashlytics received HTTP 200 from
`crashlyticsreports-pa.googleapis.com`; Performance received HTTP 200 from
`firebaselogging-pa.googleapis.com`. Early Performance traces were dropped by SDK sampling;
the later test trace was logged and its transport batch accepted after Remote Config recovered.
No code or Firebase configuration changes were needed for the VPN retest. Server acceptance is
confirmed; appearance of the processed data in Firebase Console was not verified.

The app targets Cuban users, so successful delivery with VPN does not establish delivery on their
normal connections. Analytics worked without VPN in this test; Crashlytics and Performance did
not. Treat this as an observed network limitation as of 2026-10-22, rather than a guarantee about
every Cuban network. The app now retains only Analytics; the results above document the former
integration. Their code is preserved as comments for reactivation. See the [known limitations](../README.md#known-limitations).


### Reactivating Crashlytics and Performance Monitoring

Only Analytics is active. Crashlytics and Performance are preserved as comments because their
uploads failed from Cuba without VPN on 2026-10-22. Once delivery is available from Cuba, restore
these pieces together:

1. Uncomment the Crashlytics and Performance plugin aliases in `gradle/libs.versions.toml` and
   `build.gradle.kts`, and their plugin applications and SDK dependencies in `app/build.gradle.kts`.
2. In `telemetry/AppTelemetry.kt`, uncomment the SDK imports, Crashlytics `build_type` key,
   `recordNonFatal()`, and `startTrace()`.
3. In `ui/contacts/ContactsListViewModel.kt`, uncomment the imports, injected application context,
   contacts-loading trace, nonfatal reporting, and `finally` block that stops the trace.
4. In `src/debug/AndroidManifest.xml`, restore the Performance logging metadata. In
   `debug/TelemetryTestReceiver.kt`, uncomment the imports, Crashlytics test key, synthetic error
   branches, and Performance trace; update the log and accepted-kind message accordingly.
5. Build debug and release and run the JVM tests. On a test device in Cuba without VPN, verify a
   synthetic nonfatal report and trace: check successful transport responses and the matching data
   in Firebase Console. Recheck Analytics and update both READMEs to reflect the active services.

The Crashlytics Gradle plugin resumes release mapping uploads when re-enabled. While disabled,
keep the R8 mapping file for each distributed release for manual stack-trace deobfuscation.

## Tests

Local JVM tests cover the pure phone-number rules and ETECSA wrapper parsing:

```sh
make android-test
```

When changing contact-provider queries, service permissions, notification behavior, or Compose
interactions, also validate on a device or emulator with the corresponding Android permissions and
telephony capabilities. A simulator cannot fully reproduce incoming cellular call-state events.

## Build and export

Commands are defined in the repository-root `Makefile`; run `make help` to list them. The Gradle
Wrapper is included under `android/`.

```sh
make android-version  # show the configured version
make android-debug    # build a debug APK
make android-test     # run local unit tests
make android-release  # build the optimized release APK
make android-export   # build and copy the APK to dist/android/
make android-bundle   # build the versioned AAB for Google Play
make android-bundle-export # build and copy the AAB to dist/android/
make android-clean    # remove Android build outputs
```

The release build enables R8 code shrinking/optimization and unused-resource shrinking. The APK is
named from `versionName`, for example `llamacon99-v1.0.0.apk`, and is initially written to
`app/build/outputs/apk/release/`. `make android-export` copies it to
`dist/android/llamacon99-v<VERSION>.apk`.

App Bundles have a separate naming rule: the final `SingleArtifact.BUNDLE` is copied
without changing its signed contents to `app/build/outputs/bundle/release/llamacon99-v<VERSION>.aab`.
This also applies when generating a signed bundle from Android Studio. Use
`make android-bundle-export` to export it as `dist/android/llamacon99-v<VERSION>.aab`.
The APK naming rule alone does not rename AABs.

### Release signing

To sign a release for distribution, create a private keystore at `android/keys/release.jks` and a
local-only `android/keys/release.properties`:

```properties
storeFile=keys/release.jks
storePassword=...
keyAlias=llamacon99
keyPassword=...
```

The `android/keys/` directory is ignored by Git. Without these files Gradle can build an unsigned
release APK, but that APK cannot be used as a signed update to an existing installation. Keep the
keystore backed up securely; published updates must use the same signing key.

Update `versionName` and `versionCode` in `app/build.gradle.kts` before publishing. The exported
filename automatically follows `versionName`.

## Project configuration

`settings.gradle.kts`, the root `build.gradle.kts`, and `gradle/libs.versions.toml` configure the
Android project and version catalog. Keep dependency versions in the catalog where possible.
`app/build.gradle.kts` conditionally applies the Google Services Gradle plugin only when the app-specific
`google-services.json` is present, enabling local builds without Firebase secrets. The module uses
JDK 17, compile/target SDK 36, minimum SDK 26, Jetpack Compose, Navigation Compose, DataStore, Hilt,
Coil, and Firebase libraries.
