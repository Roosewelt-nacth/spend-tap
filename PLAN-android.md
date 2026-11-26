# Spend Tap for Android: Plan

Goal: a native Android app with the same features as the iOS one, a quick-log overlay (the Android version of Back Tap), and a release path to Google Play.

## 1. Decisions

| Question | Decision | Why |
|---|---|---|
| Language / UI | **Kotlin + Jetpack Compose** | Native, Google's current UI toolkit, closest to what Android users expect |
| Design language | **Material 3 with dynamic colour (Material You)** plus glass cards over an animated gradient | iOS "Liquid Glass" doesn't exist on Android. A blurred, translucent card look still reads as glass, and Material You makes it feel at home on the user's wallpaper theme |
| Glass blur | `haze` library (real backdrop blur) on Android 12+; plain translucent cards below that | Compose has no built-in backdrop blur, and blur needs API 31+ |
| Charts | Custom `Canvas` donut and bars, animated | No dependency, full control, same look as iOS |
| Storage | **Room (SQLite)** for entries and plans, **DataStore** for settings | More robust than JSON files for an app other people will use. Same columns as the Supabase `entries` table |
| Architecture | MVVM: `ViewModel` + `StateFlow` + repository | Standard, testable |
| Yearly Google Sheet | Same Apps Script web app as iOS (`google-sheets/Code.gs`), uploaded by **WorkManager** | Android can run a scheduled job in the background, so the export can run by itself in January, unlike iOS |
| Build machine | This Windows PC | Android builds on Windows. No Mac needed |

## 2. Feature map: iOS to Android

| iOS | Android |
|---|---|
| Back Tap, then Shortcut, then App Intent | **Quick Settings tile** and **app shortcut** open a translucent quick-log sheet over the current app. On Pixel phones, Settings, System, Gestures, Quick Tap can launch it by double-tapping the back. Samsung can do it with a Routine or Good Lock |
| Month tab (hero ring, filters, list) | Same, in Compose |
| Analytics (donut, daily and monthly bars, income split) | Same, drawn on Canvas |
| Savings tab (SIPs, RDs, goals, projections) | Same |
| Settings (income, budget, Sheets) | Same, plus a notification toggle |
| Home-screen widget (planned) | **Glance widget** showing today's spend with a "+" button |
| Yearly upload on first January launch | **WorkManager** daily check, uploads the previous year automatically |

I would not build a floating bubble that draws over other apps. It needs the "display over other apps" permission, Google Play scrutinises it, and users distrust it. The translucent quick-log screen gives the same effect without it.

## 3. Build order

1. **Toolchain:** JDK 17, Android command-line tools, SDK platform and build-tools, Gradle wrapper. Command-line only, no Android Studio, to save disk space.
2. **Project skeleton:** Gradle Kotlin DSL, Compose, Material 3, one empty screen that builds.
3. **Logic first:** port `Analytics`, `SavingsPlan` and `MonthKey` to Kotlin, plus the same 28 checks as JVM unit tests. They run on the PC with no emulator.
4. **Data layer:** Room entities, DAOs, repository, DataStore.
5. **Screens:** Month, Analytics, Savings, Settings, in the glass style.
6. **Quick log:** the tile, the app shortcut and the translucent sheet with a keypad and category chips.
7. **Sheets export and WorkManager job.**
8. **Widget.**
9. **Release build:** signed bundle (`.aab`), plus an `.apk` for direct installs.

## 4. Testing on a phone

- **Your Android phone over USB (best):** turn on Developer options and USB debugging, then `adb install`. This also tests the tile and Quick Tap for real.
- **No phone:** an emulator needs several more GB of disk and a system image. With 12.7 GB free that is tight, so I would skip it unless you want it.

## 5. Publishing to Google Play

Things I'm fairly sure of, to confirm in the Play Console:
- **Developer account:** one-time fee (US$25). Identity verification is required.
- **New personal accounts** must run a closed test with a minimum number of testers for about two weeks before production access. Check the current numbers.
- **Target API level:** new apps must target a recent Android version. Check the current deadline when we reach release.
- **Format:** an Android App Bundle (`.aab`), signed by an upload key you keep safe. Play App Signing holds the real key.
- **Store listing:** icon, feature graphic, at least two screenshots, short and full descriptions.
- **Privacy policy URL:** required. The app keeps data on the device and only sends a report to a sheet the user connects, which makes the policy short.
- **Data safety form:** declare local-only storage and the optional Google Sheets upload.
- **Financial apps:** a spend tracker that doesn't move money should not need the extra financial-services declarations, but check the Play policy page for your region.

## 6. Risks and unknowns

- **Disk:** 12.7 GB free on C:. The command-line toolchain is roughly 3 to 4 GB with caches. Another drive would be safer if one exists.
- **Versions:** I don't know the newest stable Android Gradle Plugin and Compose versions. I will look them up when scaffolding, not guess.
- **Haze blur** costs frame rate on older phones. I'll keep it to a few surfaces and fall back to plain translucent cards on Android 11 and older.
- **Quick Tap** is a Pixel feature, and other makers' equivalents vary, so the tile and shortcut are the dependable triggers.
- **Visual check:** without an emulator or phone connected, I can compile but not see the screens, as with iOS.

## 7. Open questions

1. Do you have an Android phone you can connect by USB, and which model and Android version?
2. Is there a drive with more free space for the SDK (it is about 3 to 4 GB)?
3. Do you plan to publish under your own Play account, or hand the project to someone else?
4. Should the Android app also keep data on the phone only, or sync to Supabase later?
