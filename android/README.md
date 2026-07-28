# Shared Budget — Android app

Native Kotlin + Jetpack Compose app for logging transactions into the
shared Google Sheet that powers the static site at the repo root.

Three tabs (floating bottom navigation):

- **Home** — total + per-person balances on a health-tinted gradient
  card, category donut with budget bars, and the Add Transaction form
  (top-up toggle, amount, who, category chips, note). Posts to the
  Apps Script web endpoint, which appends a row to the sheet.
- **Months** — spending by month: an animated trend bar chart (tap a
  bar to inspect a month) plus expandable per-month cards with top-ups,
  per-person split, and category breakdown.
- **History** — all transactions grouped by day with daily totals;
  the five most recent rows can be edited or deleted.

## Prerequisites (one time, on Windows)

1. Install **Android Studio** (Hedgehog or newer) — this bundles JDK 17,
   the SDK, and Gradle. Let it download SDK platform 34 and build-tools
   on first launch.
2. Deploy the Apps Script endpoint by following
   [`../apps-script/README.md`](../apps-script/README.md). Copy the
   `/exec` URL — you'll need it below.
3. Note the public URL of `sheet.csv` (the static site's domain
   plus `/sheet.csv` — e.g. `https://<user>.github.io/<repo>/sheet.csv`
   or your Vercel URL).

## Configure the URLs

The two URLs are wired in via Gradle properties so you can rebuild for
new environments without editing source. Either:

- Edit the defaults at the top of [`app/build.gradle.kts`](app/build.gradle.kts), **or**
- Pass them on the command line: `gradlew assembleDebug -PWEB_APP_URL=... -PSHEET_CSV_URL=...`, **or**
- Add to a `gradle.properties` (in this `android/` folder) you keep locally:
  ```
  WEB_APP_URL=https://script.google.com/macros/s/.../exec
  SHEET_CSV_URL=https://<your>.github.io/<repo>/sheet.csv
  ```

## Build a debug APK

Open this `android/` folder as a project in Android Studio and choose
**Build → Build Bundle(s) / APK(s) → Build APK(s)**. Output:

```
app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is auto-signed with the Android Studio debug keystore —
installable on stock Android in unknown-sources mode.

## Install on a phone

1. Send `app-debug.apk` to the phone (email, Telegram, USB).
2. Open the file. Android prompts to allow "Install unknown apps" for the
   source app (Files / Gmail / Chrome, etc.).
3. Tap Install. The app appears in the launcher as "Shared Budget".

For updates, rebuild and resend — same package name + same debug key
means it installs in place, no uninstall needed.

## Known limitations

- **No offline queue (v1).** If the phone has no connectivity, Submit
  fails with a snackbar; the form state is preserved so you can retry.
- **Apps Script URL must be stable.** Re-deploying via "New deployment"
  rotates the URL and breaks installed apps. Always use **Manage
  deployments → edit version → Deploy** instead.
- **Debug keystore expires after 365 days.** Either rebuild within the
  year, or migrate to a release keystore via `keytool` and a
  `signingConfigs.release` block.

## Layout

```
android/
  settings.gradle.kts
  build.gradle.kts                      — root, plugin versions
  gradle.properties                     — JVM args, AndroidX flag
  app/
    build.gradle.kts                    — Compose, OkHttp, serialization, BuildConfig URLs
    proguard-rules.pro
    src/main/
      AndroidManifest.xml               — single Activity, INTERNET permission
      res/values/{strings,themes}.xml
      java/com/subhanismayil/budget/
        MainActivity.kt                 — entry point, swaps Entry/Balances
        ui/theme/{Color,Type,Theme}.kt  — Material3 dark glass palette
        ui/EntryScreen.kt               — transaction entry form
        ui/EntryViewModel.kt            — form state + submit
        ui/BalancesScreen.kt            — read-only stats from sheet.csv
        ui/BalancesViewModel.kt
        data/Categories.kt              — single source of truth (mirrors app.js)
        data/Models.kt                  — TransactionRequest / TransactionResponse
        data/BudgetApi.kt               — OkHttp + kotlinx.serialization
        data/CsvParser.kt               — port of app.js parsing + StatsComputer
        data/Prefs.kt                   — DataStore (remembers last "who")
```
