# iOS Back-Tap Quick Spend: Plan

Goal: triple back-tap the iPhone, a native overlay drops down over whatever app is open, you type the amount, tap a category, and it's logged. No app launch.

## 1. What the reference video shows

A Dynamic Island morph: a small pill expands into a black rounded capsule under the island, and a white selection capsule slides between three options with a spring animation. For this app, the three icons become category chips, and a number readout sits above or inside the capsule.

Style tokens to copy: pure black capsule, white selected pill, monospaced digits, spring morph, about 30pt corner radius.

## 2. What iOS allows

iOS never lets a third-party app draw its own free-floating overlay over other apps. There are only three sanctioned surfaces. They set the whole design.

| Surface | Look | Text/number input | Works from Back Tap | Risk |
|---|---|---|---|---|
| **A. App Intent parameters** (system sheet) | Native Siri-style card | Native number pad, then native choice list | Yes, rock solid | Plain look |
| **B. Interactive Snippet** (`SnippetIntent`, iOS 26+) | Your own SwiftUI card over the current app | Your own keypad built from buttons | Yes | Newer API, verify on device |
| **C. Live Activity in the Dynamic Island** | Closest to the video | Buttons only, no keyboard | Starting one from the background is restricted | Highest risk, see below |

Honest caveats:
- Back Tap can only run a Shortcut. The Shortcut calls our App Intent. That is the entire trigger path.
- Surface C cannot be a persistent, always-expanded overlay. The island expands briefly on start or update, then collapses. Phones without a Dynamic Island only get a lock-screen banner.
- Starting a Live Activity from a background Shortcut is limited by ActivityKit. Treat C as a stretch goal and test it early.
- On a locked phone, the Shortcut may ask for Face ID first.

## 3. Recommended approach

Build a new native SwiftUI app. Don't extend the Expo app. App Intents, snippets and Live Activities are native-only, and React Native adds friction with no benefit here.

Layers, built in this order:
1. **Prototype 0, no code and no Mac:** a plain Shortcut (Ask for Input, then Choose from Menu, then POST to Supabase). It proves the Back Tap flow and your category list today.
2. **Prototype 1:** native app with `LogSpendIntent` (surface A). This is the first real app.
3. **Prototype 2:** interactive snippet UI styled like the video (surface B).
4. **Stretch:** Dynamic Island Live Activity (surface C).

## 4. What already exists and what we reuse

| Existing | Verdict |
|---|---|
| `supabase/` schema (`entries`, `settings`) and `get-advice` function | **Reuse as is.** The new app writes the same rows. |
| `mobile-app/` (Expo, RN 0.74) | **Park it.** It was never built. No Back Tap path, and its widget plumbing is a different design. |
| `mobile-app/ios-widget/SpendWidget.swift` | **Reuse** the colors and the App Group idea for a home-screen widget later. |
| `desktop-app/` (Electron overlay) | Untouched. It shares the same backend. |
| `types.ts` (`SpendEntry`) | Reuse the field names. The Swift model mirrors them. |
| Auth | There is none. Rows are keyed by a local `device_id`. Fine for solo testing. |

Gaps: nothing exists for App Intents, Back Tap, a native Swift target or Live Activities. The bundle id and App Group (`com.yourorg...`) are placeholders and need real values.

## 5. Categories

Your four: **Family, Friends, Personal, Petrol.**

Proposed additions, to be confirmed by you:
- Food & Dining
- Groceries
- Transport (non-petrol: cab, metro)
- Shopping
- Bills & Rent
- Health
- Entertainment
- Other

Rules for the quick overlay:
- Show **at most 6 chips** (the video has 3). Pin your top ones; the rest sit behind "More".
- Sort by recent use so the common ones are one tap away.
- Keep the existing `payment_method` (UPI or cash), `note` and `is_impulse` fields as **optional**, defaulted to UPI, empty and false. The overlay must stay two taps.

## 6. Architecture

```
Back Tap x3
  -> Shortcut: "Log Spend" (one action: our App Intent)
     -> LogSpendIntent (App Intents framework, runs without opening the app)
        -> amount (number prompt) + category (AppEnum / chips)
        -> SpendStore (SwiftData in the shared App Group container)
        -> background sync to Supabase REST (existing schema)
  -> Home-screen widget reads the same App Group store (later)
```

Targets: one app target (SwiftUI, App Intents included), one widget extension (later), and one Live Activity extension (stretch).

## 7. How to get started

You are on Windows, which matters.

**Step 0 (about 15 min, any iPhone, no Mac):**
1. Shortcuts app, new shortcut "Log Spend": Ask for Input (Number, "How much?"), Choose from Menu (Family, Friends, Personal, Petrol, Other), then Get Contents of URL (POST to your Supabase `entries` endpoint).
2. Settings, Accessibility, Touch, Back Tap, Triple Tap, pick "Log Spend".
3. Check that the flow feels right and settle the category list.

**Step 1 onward needs a Mac with Xcode.** iOS apps cannot be compiled on Windows. Options:
- A Mac you own or borrow (best).
- A cloud Mac (MacinCloud and similar) or a GitHub Actions macOS runner. Workable, but debugging on a real device is painful.
- An Apple Developer account ($99/yr) is needed for TestFlight and long-lived installs. A free account allows 7-day device installs. Whether it covers App Groups and Live Activities needs checking.

**Build order once on a Mac:**
1. Create the `ios-app/` SwiftUI project, bundle id, App Group.
2. `SpendEntry` model and `SpendStore` (SwiftData).
3. `LogSpendIntent` plus `AppShortcutsProvider` (surface A). Test Back Tap end to end.
4. Supabase sync (URLSession, same schema).
5. Snippet UI in the video's style (surface B).
6. Optional: widget, then the Live Activity experiment.

## 8. Definition of "working prototype"

- Triple tap on a real iPhone shows the overlay without opening the app.
- Amount plus category logs in two interactions or fewer.
- The entry appears in Supabase and in the app's list within seconds.
- The overlay uses the black capsule, white selected pill and spring motion from the video (surface B).

## 9. Open questions

1. Do you have access to a Mac with Xcode?
2. Which iPhone and iOS version? (iOS 26 is needed for surface B. Dynamic Island is needed for C.)
3. Is the category list in section 5 right?
4. Should it stay on Supabase, or be local-only for the first prototype?
