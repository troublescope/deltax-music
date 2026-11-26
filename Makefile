.DEFAULT_GOAL := help

.PHONY: help
help:
	@echo "Available targets:"
	@echo "  build                 Assemble Android debug APK"
	@echo "  build-clean           Clean and assemble Android debug APK"
	@echo "  android-debug         Assemble Android debug APK"
	@echo "  android-debug-clean   Clean and assemble Android debug APK"
	@echo "  android-release       Assemble Android release APK"
	@echo "  android-release-clean Clean and assemble Android release APK"
	@echo "  test                  Run Android unit tests"
	@echo "  lint                  Run ktlint checks"
	@echo "  format                Format Kotlin files with ktlint"
	@echo "  clean                 Clean Gradle build artifacts"

.PHONY: deps
deps:
	./scripts/deps.sh

.PHONY: build build-clean
build:
	./gradlew :androidApp:assembleDebug

build-clean:
	./gradlew clean :androidApp:assembleDebug

.PHONY: flatpak-deps flatpak-build flatpak-install flatpak-bundle flatpak-run flatpak-run-installed flatpak-uninstall
flatpak-deps:
	flatpak install --user -y $(FLATPAK_REMOTE) $(FLATPAK_RUNTIME)//$(FLATPAK_RUNTIME_VERSION) $(FLATPAK_SDK)//$(FLATPAK_RUNTIME_VERSION)

flatpak-build:
	flatpak-builder --install-deps-from=$(FLATPAK_REMOTE) --disable-rofiles-fuse --force-clean $(FLATPAK_BUILD_DIR) $(FLATPAK_MANIFEST)

flatpak-install:
	flatpak-builder --install-deps-from=$(FLATPAK_REMOTE) --disable-rofiles-fuse --force-clean --user --install $(FLATPAK_BUILD_DIR) $(FLATPAK_MANIFEST)

flatpak-bundle:
	flatpak-builder --install-deps-from=$(FLATPAK_REMOTE) --disable-rofiles-fuse --force-clean --repo=$(FLATPAK_REPO_DIR) $(FLATPAK_BUILD_DIR) $(FLATPAK_MANIFEST)
	flatpak build-bundle $(FLATPAK_REPO_DIR) $(FLATPAK_APP_ID).flatpak $(FLATPAK_APP_ID)

flatpak-run:
	@if [ ! -d "$(FLATPAK_BUILD_DIR)" ]; then \
		$(MAKE) flatpak-build; \
	fi
	flatpak-builder --run $(FLATPAK_BUILD_DIR) $(FLATPAK_MANIFEST) $(FLATPAK_APP_ID)

flatpak-run-installed:
	flatpak run $(FLATPAK_APP_ID)

flatpak-uninstall:
	flatpak uninstall --user -y $(FLATPAK_APP_ID)

.PHONY: build-appimage
build-appimage:
	./scripts/build_appimage.sh

.PHONY: aur-install
aur-install:
	./aur/install-local.sh

.PHONY: android-debug android-debug-clean android-release android-release-clean
android-debug:
	./gradlew :androidApp:assembleDebug

android-debug-clean:
	./gradlew clean :androidApp:assembleDebug

android-release:
	./gradlew :androidApp:assembleRelease

android-release-clean:
	./gradlew clean :androidApp:assembleRelease

.PHONY: ios-project ios-build ios-ipa ios-sim-run ios-test ios-clean
ios-project:
	@echo "Checking Xcode project structure..."
	test -f iosApp/iosApp.xcodeproj/project.pbxproj && echo "✅ iosApp.xcodeproj is ready."

ios-build:
	./scripts/build_ios.sh --sim

ios-ipa:
	./scripts/build_ios.sh --ipa

ios-sim-run:
	./scripts/build_ios.sh --sim --run

ios-test:
	@echo "Testing iOS App Swift syntax integrity..."
	find iosApp -name "*.swift" -print0 | xargs -0 xcrun --sdk iphonesimulator swiftc -target arm64-apple-ios15.0-simulator -parse
	@echo "All iOS Swift files verified successfully."

ios-clean:
	./scripts/build_ios.sh --clean

.PHONY: test lint format
test:
	./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

.PHONY: clean clean-all
clean:
	./gradlew clean

clean-all:
	./gradlew clean
	rm -rf build/ dist/ .flatpak-builder $(FLATPAK_APP_ID).flatpak iosApp/build
