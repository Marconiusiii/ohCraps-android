# Oh Craps Android Play Store Release Runbook

## 1. Create Upload Key (one-time)

Run from terminal:

```bash
keytool -genkeypair \
	-v \
	-keystore ohcraps-upload-key.jks \
	-keyalg RSA \
	-keysize 4096 \
	-validity 10000 \
	-alias ohcrapsUpload
```

Move the generated `.jks` to a safe location outside the repo.

## 2. Configure Signing (local only)

Copy the template:

```bash
cp keystore.properties.example keystore.properties
```

Edit `keystore.properties` with real values:

```properties
storeFile=/absolute/path/to/ohcraps-upload-key.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`keystore.properties` is gitignored and must never be committed.

## 3. Bump App Version

Update in `/Users/pallas/AndroidStudioProjects/OhCraps/app/build.gradle.kts`:

- `versionCode` must increase every release.
- `versionName` should match user-facing version (for example `1.0.1`).

## 4. Build Validation

Run:

```bash
./gradlew clean :app:lintDebug :app:assembleDebug
```

Install to device:

```bash
./gradlew :app:installDebug
```

## 5. Build Play Upload Artifact (AAB)

Run:

```bash
./gradlew :app:bundleRelease
```

Output:

- `/Users/pallas/AndroidStudioProjects/OhCraps/app/build/outputs/bundle/release/app-release.aab`

## 6. Play Console Submission Steps

In Play Console:

1. Create app (if first release) with package name `com.marconius.ohcraps`.
2. Enroll in Play App Signing and upload your upload key certificate when prompted.
3. Complete App Content forms:
4. Privacy policy (if required by selected declarations).
5. Data safety.
6. Ads declaration.
7. Content rating questionnaire.
8. Target audience and news status.
9. Upload `app-release.aab` to production track.
10. Add release notes.
11. Review pre-launch report and policy warnings.
12. Start rollout.

## 7. Pre-Submission Manual QA

On a physical device:

1. Verify all four tabs: Strategies, Rules, Create Strategy, About.
2. Validate TalkBack focus return flows (list to detail and back, dialogs, actions).
3. Validate large text and display scaling.
4. Validate portrait and landscape orientation behavior.
5. Validate native sharing flow for core and user strategies.
6. Validate About links, feedback email/share fallback, and Strategy submission flow.

## 8. Release Branch/Tag Hygiene

Recommended:

1. Commit version bump and release notes.
2. Tag release commit (`v1.0.0`, `v1.0.1`, etc.).
3. Keep a changelog entry with date and Play release ID.
