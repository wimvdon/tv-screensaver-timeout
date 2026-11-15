# Architecture

## This app
An Android TV app (Philips Google TVs) with one screen. It keeps Android's screen-off timeout
(`Settings.System.SCREEN_OFF_TIMEOUT`) below the two minutes after which Philips starts its own
screensaver, so the Google TV screensaver (ambient mode) starts first.

- `logic/TimeoutRules.kt`: the choices (30 s, 1 min, 90 s) and what to do in each situation
  (write, nothing to do, no permission, restore). Plain Kotlin, unit tested.
- `logic/DurationParts.kt`: how a duration is shown (minutes or seconds). Unit tested.
- `system/ScreenTimeout.kt`: reads and writes the setting and carries out a `Step` from the rules.
  Writing needs `WRITE_SETTINGS`, which TVs only let you grant over ADB
  (`adb shell appops set <package> WRITE_SETTINGS allow`).
- `system/Watcher.kt`: keeps the value in place without a running service. `BootReceiver` sets it
  again after a restart and after an app update; a job 90 seconds after boot catches firmware that
  writes its own value late; a JobScheduler content-trigger job runs whenever the setting changes
  and puts the chosen value back (it schedules itself again after each run).
- `ui/home/`: the one screen, drawn with TV Material. Without the permission it shows a setup
  state with the ADB command instead of the settings. `HomeController` carries out the actions.
- **Stored data:** one DataStore file, `device_state` (never backed up): the chosen time, whether
  to keep it set, and the TV's own value from before the first change (for Restore). Everything is
  device-only because it only works after the permission step on this TV. The template's
  `backed_up_settings` rule stays in place for settings that could be backed up later.

## Structure
```
app/src/main/kotlin/<package>/
  ui/MainActivity.kt       Entry point. Only sets up the theme and the first screen.
  logic/                   Plain Kotlin rules (no Android types) → unit tested.
  data/                    DataStore.
  system/                  Android system access: the setting, boot receiver, jobs.
  ui/theme/Theme.kt        Material 3 theme (light/dark, dynamic colour).
  ui/<feature>/            One folder per screen/feature:
    <Feature>Screen.kt       Composables: display state, forward clicks. No logic.
    <Feature>….kt            Plain Kotlin logic (no Android types) → unit tested.
app/src/main/res/
  values/strings.xml       All user-visible text (English, the default).
  values-xx/strings.xml    Dutch, French, German, Spanish and Italian. Every string must exist in every language.
app/src/test/kotlin/       JVM unit tests (mirrors the main package structure).
scripts/                   Release helpers (Python/Java) with their own tests.
.github/workflows/         ci.yml (every PR), release.yml (signed releases).
```

## Conventions
- **Single `app` module.** Split into modules only if the app grows large; record it in `decisions.md`.
- **Logic in plain Kotlin classes** with no `Context`, `View` or Compose types, so it can be
  tested on the JVM. Example: `ui/home/TapCounter.kt` (immutable; methods return a new value).
- **UI state**: small screens use `rememberSaveable`. When a screen gets real logic or data,
  add a `ViewModel` (`androidx.lifecycle:lifecycle-viewmodel-compose`) that exposes a
  `StateFlow<UiState>` and calls into the plain-Kotlin logic. Record it in `decisions.md`.
- **Navigation**: one screen today. With several screens, use Navigation Compose.
- **Persistence**: Jetpack DataStore (version already in the catalog). Wrap it behind a small
  repository interface so logic tests can use a fake. Decide per value whether it is backed up
  (see "Backup" below).
- **No network, analytics, crash reporting or ads** unless the owner agreed (see `CLAUDE.md`),
  in which case update `PRIVACY.md`.
- **Strings** in resources only; plurals via `<plurals>`.
- **Versions** of libraries only in `gradle/libs.versions.toml`.
- **App identity** (`applicationId`/`namespace`) only in `app/build.gradle.kts` (`appId`).

## Backup
Android backs up app data to the user's Google account (if they turned backup on) and copies it
to a new phone during set-up. The app decides which files take part, in
`app/src/main/res/xml/data_extraction_rules.xml` (`cloud-backup` = Google account,
`device-transfer` = new phone). The rule is an **allow list**: only listed files are copied, so a
new file is never backed up by accident.

- **Backed up:** settings the user chose and would want back after a reset or on a new phone
  (theme, layout, toggles, names). Keep them in one DataStore named `backed_up_settings`
  (`preferencesDataStore(name = "backed_up_settings")`), which the rules already list.
- **Not backed up:** sign-in tokens, passwords and API keys (security, and keys encrypted with the
  Android Keystore can't be read on another phone anyway); state that only means something on this
  phone (a running job, notification IDs, "switched on right now"); settings that only work after
  a permission or set-up on this phone; copies of data an online service can download again; caches
  (Android never backs up `cacheDir` and `noBackupFilesDir`). Keep these in another DataStore file.
- Android rules work per **file**, not per key. Moving a value from one file to another in an app
  that is already released needs a `DataMigration` (see Snoozer's `MoveKeysMigration`).
- `BackupRulesTest` checks both sections list exactly the settings file. Change the test together
  with the rules, and update `PRIVACY.md` when what is backed up changes.
- **minSdk below 31:** Android 8-11 ignore `data_extraction_rules.xml` and read
  `android:fullBackupContent` instead. Add `res/xml/backup_rules.xml` with the same `include` lines,
  point `fullBackupContent` at it in the manifest, and let `BackupRulesTest` check it too.
- To test on a phone: change a setting, then open Settings, search for "Backup" and tap **Back up now**. Uninstall the app and install the same APK again: the
  setting comes back. Restore only works for an APK signed with the same key.
