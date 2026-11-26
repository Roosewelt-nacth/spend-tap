# How this app was built, and how to do it again

This is the method behind Spend Tap, written so you can repeat it for the next feature.

## The method in one line

**Decide, then calculate, then store, then draw, then ship.** Each layer is written and checked before the next one starts, so a bug is always in the layer you just touched.

| # | Layer | Files | How it was checked |
|---|---|---|---|
| 1 | Decide what it should do | `PLAN-ios-backtap.md` | You read it |
| 2 | Models and calculation (no UI) | `Models/*.swift` | `ios-app/checks/main.swift`, 28 checks, run on the Mac |
| 3 | Storage | `Store/*.swift` | Compiles, loaded by the UI |
| 4 | Screens | `Views/*.swift` | `xcodebuild` compiles them against the real iOS 26 SDK |
| 5 | Outside world (Google Sheets) | `Export/`, `google-sheets/Code.gs` | Compiles; needs your sheet to test for real |
| 6 | Ship | `xcodebuild`, then `.ipa`, then Sideloadly | On your phone |

## Step by step, using the monthly analytics feature as the example

### 1. Turn the wish into concrete rules (before any code)

Your request was "monthly calculation, pie charts, glass UI, filters, yearly Google Sheet, SIPs". I turned each part into a rule I could check:

- A month's total only counts entries dated in that month.
- Filters change the charts and the list, but **not** the big "spent this month" number, so the headline is never misleading.
- Money left = budget minus spent. If there is no budget: income minus SIPs minus spent.
- A SIP is "due" from its start month until its end month, and counts as paid once its debit day has passed.
- The yearly export is a 12-month by category table plus a savings column.

Writing rules like these first is the most useful habit. They become the test cases.

### 2. Write the calculation as plain functions

`Models/Analytics.swift` has no SwiftUI in it. It takes lists in and returns numbers out: `Analytics.summary(...)`, `yearSummary(...)`, `report(...)`. Because it only imports Foundation, it can run as a command-line program.

### 3. Test it before drawing anything

`ios-app/checks/main.swift` states each rule as a check:

```swift
check(s.spentAll == 400, "November total ignores other months")
check(sf.spent == 100 && sf.spentAll == 400, "filter changes charts, not the hero total")
```

Run it on the Mac (no simulator, no signing):

```bash
cd ~/Documents/Austin/spend-tracker/ios-app
swiftc -module-cache-path ../../checkbuild/mc -o ../../checkbuild/check \
  SpendTap/Models/SpendEntry.swift SpendTap/Models/SavingsPlan.swift \
  SpendTap/Models/AppSettings.swift SpendTap/Models/Analytics.swift checks/main.swift
../../checkbuild/check
```

All 28 passed. When you change a rule, change the check first, watch it fail, then fix the code.

### 4. Store the data in small typed files

`JSONFile<T>` reads and writes one JSON file. `AppStore` holds the app's data, and screens only read from it.
Lesson from `AppSettings`: it decodes every key with `decodeIfPresent`. When a later version adds a setting, an older saved file still loads instead of silently resetting.

### 5. Build the screens from small pieces

- `Theme.swift` holds what every screen shares: the moving background, `glassCard()`, the month switcher and the filter bar.
- `MonthView` shows the hero ring, filter chips, stat tiles and the entry list.
- `AnalyticsView` draws the donut with Swift Charts `SectorMark`, plus the bar charts and the income split.
- `SavingsView` lists SIPs and other plans and has the editor sheet.
- `SettingsView` has income, budget and the Google Sheet link.

The "glass" feel comes from three things:
1. **`.glassEffect(...)`** on cards, chips and buttons (iOS 26 Liquid Glass).
2. **`AuroraBackground`**, a slowly moving `MeshGradient`. Glass needs colour behind it to show.
3. **Animation modifiers.** `.contentTransition(.numericText())` makes numbers roll, `.animation(.smooth, value:)` animates ring and bars, and `GlassEffectContainer` lets nearby glass pieces blend.

### 6. The yearly Google Sheet, and why it is built this way

Signing in with Google from an app needs a Google Cloud project, a verified OAuth screen and token handling. That is a lot of machinery for one upload a year.

Instead, you paste `google-sheets/Code.gs` into **your own** sheet (Extensions, then Apps Script), deploy it as a web app, and give the app the URL plus a secret phrase. The app POSTs the yearly report there, and the script writes a tab named after the year. "A sheet of the user's choice" simply means whichever sheet's script you deployed.

iOS cannot wake an app on 1 January. So the first time you open the app in a new year, it uploads the previous year (`AppStore.autoExportIfNeeded`). There are also manual export buttons in Settings.

### 7. Build and ship

```bash
git push mac master                    # from Windows
ssh <your-mac>                         # then, on the Mac:
cd ~/Documents/Austin/spend-tracker && git pull
cd ios-app && ../../tools/xcodegen-dl/xcodegen/bin/xcodegen generate
xcodebuild -project SpendTap.xcodeproj -scheme SpendTap \
  -destination "generic/platform=iOS" -configuration Release \
  -derivedDataPath ~/Documents/Austin/DerivedData \
  CODE_SIGNING_ALLOWED=NO ENABLE_DEBUG_DYLIB=NO build
```

Then zip `SpendTap.app` into `Payload/` as `SpendTap.ipa`, copy it back, and install it with Sideloadly.

`project.yml` is the only project definition. XcodeGen turns it into the Xcode project, which is why the `.xcodeproj` is never committed.

## Reusing this method for the next feature

1. Write the rules in plain sentences.
2. Add the data fields to a model (use `decodeIfPresent` for anything saved to disk).
3. Put the calculation in `Models/`, add checks to `checks/main.swift`, run them.
4. Expose it through `AppStore`.
5. Add the view, reusing `glassCard()` and `PeriodSwitcher`.
6. Build with `xcodebuild`, fix the errors it names, and re-run the checks.
7. Commit, push, rebuild the `.ipa`.

A prompt that works well for new features: *"Rules: [sentences]. Add the logic in Models/ with checks first, then the store, then the view in the existing glass style. Run the checks and a Release build on the Mac."*

## Known limits

- The screens have been compiled but **not looked at**. The simulator could not boot over SSH, so the glass and animation have not been seen on a device. Expect to adjust spacing and colours after the first look.
- Data lives on the phone only. Deleting the app deletes it. Use the yearly sheet export as your backup.
- The Sheets secret is stored in a plain settings file on the phone. Move it to the Keychain before sharing the app with others.
- SIP projections are rough estimates, not financial advice.
- Back Tap still needs the Shortcut setup described earlier.
