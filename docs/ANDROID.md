# Spend Tap for Android: build, install, publish

## Just want the app?

Download **SpendTap.apk** from the [latest release](../../../releases/latest) and open it on your phone. See the root README for the install steps.

## Build it yourself

You need JDK 17 and the Android SDK (platform 37.2 and build-tools 36.1.0). Command-line tools are enough; Android Studio is optional.

```bash
cd android-app
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew test               # logic tests, run on the JVM, no phone needed
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease bundleRelease
```

On Windows use `gradlew.bat`. If Gradle times out downloading, force IPv4: `export JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`.

## Install on a phone with a cable

1. Phone: Settings, About phone, tap **Build number** 7 times. Then Developer options, turn on **USB debugging**.
2. Plug in by USB, set the USB mode to **File transfer**, and accept the debugging prompt.
3. `adb devices`, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

No cable: copy the APK to the phone and open it.

## Quick log (Android's Back Tap)

- Settings, then **Add the tile**, then pull down Quick Settings and tap **Log spend**. A keypad floats over whatever app is open.
- Long-press the app icon for the **Log spend** shortcut.
- Pixel phones: Settings, System, Gestures, **Quick Tap**.

## Releases and signing

GitHub Actions (`.github/workflows/android.yml`) runs the tests on every push. Pushing a tag like `v0.1.0` also builds a **signed** APK and bundle and publishes them to a GitHub Release.

Signing uses four repository secrets, read through environment variables so no key is ever in the code:

| Secret | Meaning |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | the `.jks` keystore, base64-encoded |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Create a key once and keep a backup somewhere safe. Losing it means users cannot update in place and must uninstall first:

```bash
keytool -genkeypair -v -keystore release.jks -alias spendtap -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks     # paste the output into ANDROID_KEYSTORE_BASE64
```

Never commit the `.jks` (`.gitignore` already excludes `*.jks` and `*.keystore`).

## Before publishing on Google Play

Items marked (check) depend on rules to confirm in the Play Console.

1. **Package name.** `com.austin.spendtap` is permanent once uploaded. Change `applicationId` and `namespace` first.
2. **Signing.** Play wants an upload key. The key above can serve, or create a separate one.
3. **Target API level (check).** The app targets API 36. Play requires a recent target for new apps.
4. **Version.** `versionCode` must rise on every upload. CI sets it from the run number.
5. **Privacy policy and data safety form (check).** Data stays on the device. The only upload is the yearly report to a Google Sheet the user links.
6. **Developer account (check).** One-time fee. New personal accounts may need a closed test first.
7. **Store listing.** 512 px icon is in `docs/store/play-icon-512.png`. You still need a feature graphic and screenshots.
8. **Strings.** Text is hardcoded in English; move it to `strings.xml` for other languages.

## What has not been verified

- The screens have been compiled but not reviewed on many devices.
- The release build uses R8 shrinking. Room and kotlinx.serialization normally work with their built-in rules, but test a signed release build on a phone.
- The Google Sheet export has not been tested against a real sheet.
