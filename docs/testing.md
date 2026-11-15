# Testing

## Automatic (CI, on every pull request and push to `main`)

| Check | Command | Catches |
|---|---|---|
| Release script tests | `python3 -m unittest discover -s scripts/tests` | Version maths, keystore-secret decoding |
| Unit tests | `./gradlew testDebugUnitTest` | Logic bugs in plain-Kotlin classes (`app/src/test/`) |
| Lint | `./gradlew lintDebug` | Android API misuse, missing translations, accessibility, hard-coded text, … (errors fail the build) |
| Debug build | `./gradlew assembleDebug` | Compile errors; produces the APK to test on the phone |
| Release build | `./gradlew assembleRelease` | Problems caused by R8 minification / resource shrinking |

Reports (test results, lint HTML) are attached to every CI run as the `reports` artifact, also
when the run fails.

Run all of it locally before pushing:
```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
python3 -m unittest discover -s scripts/tests
```

### Writing unit tests
- Every logic change gets a test. Put logic in plain Kotlin classes (no Android types) and test
  them with JUnit in `app/src/test/kotlin/`, mirroring the package. See `TapCounterTest`.
- Test names describe behaviour in words: `` fun `tap stops at the maximum`() ``.
- No device tests (instrumented/UI tests) are set up: they need an emulator, which makes CI slow and
  flaky. Add them only if a decision in `decisions.md` says so.

## Manual (on the TV)

What automation can't check: how it looks and works on a real TV. For each pull request (before or
after it is merged: merging only waits for green CI), install the debug APK (see README, "Testing a
pull request") and follow the steps in the pull request description. In addition, check every now
and then (always before a release):

- [ ] The app opens from the TV's app list (its banner shows), and every item can be reached and
      chosen with the remote; the focused item is clearly highlighted.
- [ ] **Without the permission** (fresh install): the setup screen shows the ADB command with this
      build's package name. After running it, **Check again** shows the settings.
- [ ] **Choosing a time** updates "starts after" at the top. Leave the remote alone: the Google TV
      screensaver starts after that time, not the Philips one.
- [ ] **Restart** the TV (unplug it, or Settings › System › Restart). After about two minutes,
      open the app: the chosen time is still set.
- [ ] **Restore the TV's setting** puts back the original time and turns off Keep this setting.
- [ ] **Language**: Settings › System › Language, or the app's own language in Settings › Apps.
      All text is translated.
- [ ] **Back** leaves the app.
- [ ] After a release: install the release APK over the previous release (`adb install -r`). It
      updates and keeps the permission and settings.

## Reading a crash from a release build
Release builds are minified, so stack traces show short names like `a.b.c`. Download
`<app>-<version>-mapping.txt` from the matching GitHub Release and decode the trace with R8's
`retrace` (Android SDK `cmdline-tools/latest/bin/retrace`):
```sh
retrace <app>-<version>-mapping.txt stacktrace.txt
```
Or ask Claude Code to do it: give it the stack trace and the version.
