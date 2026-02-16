# Terminal Build And Signing Checklist

Use this file to generate the signed Android App Bundle (`.aab`) for Play Console upload.

Date baseline: February 16, 2026.

## One-time setup

- [ ] Confirm Java is installed: `java -version`
- [ ] Confirm Android SDK is available for Gradle on this machine
- [ ] Confirm project root is `/Users/pallas/AndroidStudioProjects/OhCraps`
- [ ] Confirm your upload key exists (`.jks` file)
- [ ] Confirm `/Users/pallas/AndroidStudioProjects/OhCraps/keystore.properties` exists
- [ ] Confirm `keystore.properties` has:
- [ ] `storeFile=/absolute/path/to/your-upload-key.jks`
- [ ] `storePassword=...`
- [ ] `keyAlias=...`
- [ ] `keyPassword=...`

## Versioning before every release

- [ ] Open `/Users/pallas/AndroidStudioProjects/OhCraps/app/build.gradle.kts`
- [ ] Increase `versionCode` to a higher integer than the last Play upload
- [ ] Set `versionName` to your user-facing release number
- [ ] Save file

## Pre-release validation

- [ ] Run `./gradlew clean :app:lintDebug :app:assembleDebug`
- [ ] Confirm build succeeds with no blocking errors
- [ ] Install debug build on device: `./gradlew :app:installDebug`
- [ ] Perform TalkBack verification pass on your Pixel 7

## Build Play artifact

- [ ] Run `./gradlew :app:bundleRelease`
- [ ] Confirm output file exists:
- [ ] `/Users/pallas/AndroidStudioProjects/OhCraps/app/build/outputs/bundle/release/app-release.aab`

## Optional integrity checks

- [ ] Run `./gradlew :app:lintDebug` one more time after final changes
- [ ] Record Git commit hash used for this release
- [ ] Store release artifact in a dated local folder backup

## Handoff to Play Console website

- [ ] Keep `app-release.aab` ready
- [ ] Open `02-play-console-web-submission-checklist.md`
