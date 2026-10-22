# Contributing to LlamaCon99

Thanks for helping improve LlamaCon99. Contributions to the Android and iOS apps are welcome.

## Before you start

- For a bug, check existing issues and open a [bug report](.github/ISSUE_TEMPLATE/bug_report.md) if
  the problem has not been reported.
- For a feature, open a [feature request](.github/ISSUE_TEMPLATE/feature_request.md) first so its
  scope and platform behavior can be discussed before substantial implementation work.
- For small fixes, you can open a pull request directly.
- Follow the project's [Code of Conduct](CODE_OF_CONDUCT.md).

## Development setup

Use the root `Makefile` to see the available commands:

```sh
make help
```

Android requires JDK 17 and Android SDK Platform 36. Useful commands are `make android-debug`,
`make android-test`, and `make android-release`. See [Android architecture and build notes](android/ARCHITECTURE.md).

iOS requires Xcode and XcodeGen. Useful commands are `make ios-project`, `make ios-build`, and
`make ios-device-build`. Creating an archive or IPA also requires the appropriate Apple signing
configuration and export options. See [iOS architecture and build notes](ios/ARCHITECTURE.md).

You do not need to build both platforms for a platform-specific change. State which platform(s) you
changed and which commands or device checks you ran.

## Implementation guidelines

- Keep platform-specific behavior in the matching `android/` or `ios/` project, and document shared
  product behavior consistently across both apps where appropriate.
- Keep user-facing strings in the platform's localization resources.
- Do not add personal contact data, credentials, signing keys, provisioning profiles, or private
  Firebase configuration to commits.
- Do not send contact names or phone numbers to analytics, logs, issue reports, or test fixtures.
- Prefer focused changes that are straightforward to review.
- Update the relevant architecture or build documentation when behavior or workflows change.

## Pull requests

1. Create a focused branch from the current default branch.
2. Implement the change and add or update meaningful tests when the behavior can be tested locally.
3. Run the relevant build and test commands. If a check cannot run, explain why in the pull request.
4. Complete the [pull request template](.github/PULL_REQUEST_TEMPLATE.md), including platform
   coverage and user-visible changes.
5. Keep the discussion respectful and respond to review feedback with context.

By submitting a contribution, you confirm that you have the right to submit it under the project's
license, if one is provided.
