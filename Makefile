# LlamaCon99 root build commands for Android and iOS.

ANDROID_GRADLEW := ./android/gradlew
ANDROID_VERSION := $(shell sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' android/app/build.gradle.kts | head -1)
XCODEBUILD := xcodebuild
XCODEGEN := xcodegen
IOS_PROJECT := ios/LlamaCon99.xcodeproj
IOS_SCHEME := LlamaCon99
IOS_VERSION := $(shell sed -n 's/.*MARKETING_VERSION: \([0-9.]*\).*/\1/p' ios/project.yml | head -1)
IOS_DERIVED_DATA := ios/DerivedData
IOS_ARCHIVE := $(IOS_DERIVED_DATA)/LlamaCon99.xcarchive
IOS_EXPORT_OPTIONS ?= ios/ExportOptions.plist
IOS_EXPORT_PATH := dist/ios

.DEFAULT_GOAL := help
.PHONY: help android-version android-debug android-test android-release android-export android-clean \
	ios-version ios-project ios-build ios-device-build ios-archive ios-export ios-clean

help: ## List available commands
	@awk 'BEGIN {FS = ":.*## "}; /^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

android-version: ## Show the Android app version
	@echo "LlamaCon99 Android v$(ANDROID_VERSION)"

android-debug: ## Build a debug APK for local testing
	$(ANDROID_GRADLEW) -p android :app:assembleDebug -Pksp.incremental=false

android-test: ## Run Android unit tests
	$(ANDROID_GRADLEW) -p android :app:testDebugUnitTest -Pksp.incremental=false

android-release: ## Build the minified Android release APK
	$(ANDROID_GRADLEW) -p android :app:assembleRelease -Pksp.incremental=false

android-export: android-release ## Build release and copy it to dist/android with a versioned filename
	@mkdir -p dist/android
	cp "android/app/build/outputs/apk/release/llamacon99-v$(ANDROID_VERSION).apk" "dist/android/llamacon99-v$(ANDROID_VERSION).apk"
	@echo "Exported dist/android/llamacon99-v$(ANDROID_VERSION).apk"

android-clean: ## Remove Android build outputs
	$(ANDROID_GRADLEW) -p android clean

ios-version: ## Show the iOS app version
	@echo "LlamaCon99 iOS v$(IOS_VERSION)"

ios-project: ## Regenerate the iOS Xcode project from ios/project.yml
	cd ios && $(XCODEGEN) generate

ios-build: ## Build the iOS app and embedded Caller ID extension for Simulator
	$(XCODEBUILD) -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) -configuration Debug \
		-destination 'generic/platform=iOS Simulator' -derivedDataPath $(IOS_DERIVED_DATA) build

ios-device-build: ## Compile the iOS app for a physical device (unsigned)
	$(XCODEBUILD) -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) -configuration Release \
		-destination 'generic/platform=iOS' -derivedDataPath $(IOS_DERIVED_DATA) \
		CODE_SIGNING_ALLOWED=NO build

ios-archive: ## Create a signed iOS archive (requires signing configured in Xcode)
	$(XCODEBUILD) -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) -configuration Release \
		-destination 'generic/platform=iOS' -archivePath $(IOS_ARCHIVE) \
		-derivedDataPath $(IOS_DERIVED_DATA) archive

ios-export: ios-archive ## Export a signed IPA using IOS_EXPORT_OPTIONS
	@test -f "$(IOS_EXPORT_OPTIONS)" || { echo "Missing $(IOS_EXPORT_OPTIONS). Set IOS_EXPORT_OPTIONS to an ExportOptions.plist created for your distribution method."; exit 1; }
	@mkdir -p $(IOS_EXPORT_PATH)
	$(XCODEBUILD) -exportArchive -archivePath $(IOS_ARCHIVE) \
		-exportOptionsPlist "$(IOS_EXPORT_OPTIONS)" -exportPath $(IOS_EXPORT_PATH)
	@echo "Exported iOS package to $(IOS_EXPORT_PATH)"

ios-clean: ## Clean the iOS Xcode build products
	$(XCODEBUILD) -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) \
		-derivedDataPath $(IOS_DERIVED_DATA) clean
