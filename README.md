# Spend Tap

A personal spend tracker built around one idea: logging a spend should take two taps, from anywhere on your phone.

Native apps for **Android** and **iOS** share the same rules: monthly spend with a budget ring, filters, pie and bar analytics, SIP and savings plans that are set aside before spending, and a yearly export to a Google Sheet you own. Data stays on the phone.

## Download the Android app

1. Open the [latest release](../../releases/latest) and download **SpendTap.apk**.
   Direct link: `https://github.com/Roosewelt-nacth/spend-tap/releases/latest/download/SpendTap.apk`
2. Open the file on your phone. Android will ask you to allow installs from your browser or Files app. Allow it for this one install.
3. If Play Protect warns about an unknown app, choose **Install anyway**. The app is not on the Play Store yet.
4. Open **Spend Tap**, then Settings, then **Add the tile**. The **Log spend** tile opens a keypad over any app.

Updates: install the newer APK over the old one. Your data is kept, because every release is signed with the same key.

## Quick log

| Platform | Trigger |
|---|---|
| Android | Quick Settings tile, app shortcut, or Pixel Quick Tap |
| iOS | Back Tap, then a Shortcut that runs the Log Spend action |

## What's here

| Folder | What |
|---|---|
| [android-app/](android-app) | Kotlin, Jetpack Compose, Room. Built by GitHub Actions |
| [ios-app/](ios-app) | SwiftUI, App Intents. Needs a Mac with Xcode to build |
| [google-sheets/](google-sheets) | Apps Script that receives the yearly report |
| [docs/](docs) | Build guides, how the apps were built |
| [desktop-app/](desktop-app), [mobile-app/](mobile-app), [supabase/](supabase) | Earlier prototypes (Electron, Expo, Supabase backend) |

## Build it yourself

See [docs/ANDROID.md](docs/ANDROID.md) for Android and [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md) for the method and iOS steps. Plans are in [PLAN-android.md](PLAN-android.md) and [PLAN-ios-backtap.md](PLAN-ios-backtap.md).

## Releasing

Pushing a tag such as `v0.1.0` makes GitHub Actions run the tests, build and sign the APK and bundle, and publish them as a release. Repository secrets used: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

## Status

Early. The screens compile and the logic is tested, but expect rough edges. Not financial advice: projections are estimates.
