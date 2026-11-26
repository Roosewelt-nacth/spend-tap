# Spend Tap for Android: build, install, publish

Everything runs from this Windows PC. The toolchain lives in `Desktop\Austin\android-toolchain` (JDK 17, Android SDK, Gradle caches) and nothing is installed system-wide.

## Build and test

Git Bash:

```bash
source <local-path>   # sets JAVA_HOME, ANDROID_HOME, caches
cd <local-path>
cmd //c ".\\gradlew.bat" test                 # logic tests (JVM, no phone needed)
cmd //c ".\\gradlew.bat" assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
cmd //c ".\\gradlew.bat" assembleRelease bundleRelease
```

`env.sh` also forces Java to use IPv4. Without that, Gradle timed out reaching its download servers from this PC.

## Install on your phone (USB)

1. Phone: Settings, About phone, tap **Build number** 7 times. Then Settings, System, Developer options, turn on **USB debugging**.
2. Plug in by USB and accept the "Allow USB debugging" prompt.
3. Check it is seen, then install:

```bash
source <local-path>
adb devices
adb install -r <local-path>
```

Without a cable you can copy `app-debug.apk` to the phone and open it. Android asks you to allow installs from that app (Files, Chrome, etc.).

## Using the quick log (Android's Back Tap)

- Open the app, then Settings, then **Add the tile**. Pull down Quick Settings: the **Log spend** tile opens a keypad over whatever app is open.
- Long-press the app icon for the **Log spend** shortcut.
- Pixel phones: Settings, System, Gestures, **Quick Tap**, then choose to open the app or its shortcut.

## Before you publish

These are the things that must change. The ones marked (check) depend on rules I could not verify here, so confirm them in the Play Console.

1. **Package name.** `com.austin.spendtap` is permanent once uploaded. Change `applicationId` and `namespace` in `app/build.gradle.kts` to one you own, for example a domain you control reversed.
2. **App icon.** The current icon is placeholder artwork. Replace `drawable/ic_launcher_foreground.xml` and `ic_tile.xml`.
3. **Signing key.** Create an upload key once and keep it and its passwords safe. If it is lost you must go through Play's key reset.

```bash
source <local-path>
keytool -genkeypair -v -keystore spendtap-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

   Keep the `.jks` outside the repository (`.gitignore` already excludes `*.jks` and `*.keystore`). Add a `signingConfigs` block reading the passwords from `~/.gradle/gradle.properties`, not from files in the project.
4. **Target API level (check).** The app targets API 36. Play requires new apps and updates to target a recent level. Check the current deadline.
5. **Version.** Raise `versionCode` for every upload. It is 1 now.
6. **Strings.** Text is hardcoded in English. Move it to `strings.xml` if you want other languages.
7. **Privacy policy (check).** Required. State that data stays on the device and the only upload is the yearly report to a Google Sheet link the user supplies.
8. **Data safety form (check).** Declare no data collected by you, and the optional user-initiated upload to their own Google Sheet.
9. **Developer account (check).** One-time fee and identity verification. New personal accounts may have to run a closed test with a minimum number of testers before production.
10. **Store listing.** 512px icon, feature graphic, at least two phone screenshots, short and full description.
11. **Upload** the `.aab` from `app/build/outputs/bundle/release/` after signing it. Play App Signing then holds the final app key.

## What has not been verified

- The screens compile but have **not been seen on a device**. Glass, spacing and animations may need tuning.
- The release build uses R8 shrinking. Room and kotlinx.serialization normally work with their built-in rules, but a release build has not been run on a phone. Test the signed release build before uploading.
- The Quick Settings tile, shortcut and translucent sheet need a real phone.
- The Google Sheet export has not been tested against a real sheet.
