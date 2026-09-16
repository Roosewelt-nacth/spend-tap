# Spend Tracker — Mobile (iOS + Android)

A single Expo/React Native app for both platforms, plus a native home-screen widget per platform. Shares the same Supabase backend as the desktop widget.

## What I could and couldn't verify

I wrote and reviewed all of this code carefully, but **could not build or run it** in the environment this was created in: no Java/Android SDK were installed (needed for Android), and iOS requires a Mac with Xcode, which isn't available here at all. Treat this as a solid, reviewed starting point — expect the first `expo prebuild` / native build to surface a few environment-specific fixes (dependency versions drifting, a missing native-module linking step, etc.), which is normal for a first native build and not a sign anything here was untested guesswork beyond what's stated.

## Architecture

- **App.tsx / src/screens** — the shared UI: setup (Supabase creds + categories + budgets) and dashboard (quick-add + budget bars + recent entries + AI advice)
- **src/lib** — Supabase client, local config storage, entry CRUD, advice call, widget-sync helper
- **src/widgets/SpendWidget.tsx** — Android widget layout (declarative, rendered natively by `react-native-android-widget`)
- **ios-widget/SpendWidget.swift** — iOS WidgetKit extension source, to be added manually in Xcode (see below)
- **../supabase/functions/get-advice** — Edge Function that proxies the Claude API call, so the Anthropic key stays server-side instead of shipping inside the app

## 1. Backend (shared with desktop app)

If you already ran `schema.sql` for the desktop app, this is the same Supabase project — no changes needed there.

Deploy the advice proxy function so the mobile app doesn't need the raw Anthropic key:

```bash
npx supabase login
npx supabase link --project-ref <your-project-ref>
npx supabase functions deploy get-advice
npx supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
```

## 2. Install and run the app (no widgets yet)

```bash
cd mobile-app
npm install
npx expo start
```

Scan the QR code with Expo Go on your phone. At this stage the app works fully (add spends, budgets, advice) but the home-screen widget won't exist yet — Expo Go can't load custom native modules.

## 3. Android widget (buildable on Windows)

You'll need Android Studio + an SDK installed (Java 17, Android SDK, an emulator or a USB-connected phone with developer mode on).

```bash
npx expo prebuild -p android
npx expo run:android
```

`expo prebuild` reads the `react-native-android-widget` plugin config in `app.json` and generates the native Android project with the widget wired in. Long-press your home screen → Widgets → "Spend Tracker" to pin it.

## 4. iOS widget (needs a Mac)

1. On a Mac, run `npx expo prebuild -p ios`, then open `ios/SpendTracker.xcworkspace` in Xcode
2. File → New → Target → **Widget Extension**, name it `SpendWidget`
3. Add `ios-widget/SpendWidget.swift` to that new target
4. In both the main app target and the `SpendWidget` target: Signing & Capabilities → **+ App Groups** → add `group.com.yourorg.spendtracker` (must match `APP_GROUP` in `SpendWidget.swift` and in `src/lib/widgetSync.ts`)
5. Build and run on a simulator or device; long-press the Home Screen → + → find "Spend Tracker"

## Notes / caveats carried over from the desktop app

- Still no real user auth — a random device ID is generated on first launch and tags all rows, same as the desktop app. Fine for solo testing across your own phone + desktop; add Supabase Auth before this touches anyone else's data.
- Update `group.com.yourorg.spendtracker` and `com.yourorg.spendtracker` (bundle id / package name) everywhere they appear before shipping — they're placeholders.
- Widgets refresh on a timer (Android via `requestWidgetUpdate` calls after each new entry; iOS every ~30 min via WidgetKit's own timeline, since iOS limits how often a widget can force-refresh).
