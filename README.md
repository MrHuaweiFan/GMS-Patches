# Google GMS Patches

**microG / GmsCore support patches for first-party Google apps — for use with [Morphe](https://morphe.software).**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://android.com)
[![Apps](https://img.shields.io/badge/Google%20Apps-6%20patched-success?style=flat-square)](#google-apps-patches)

This is a fork of [De-Vanced](https://github.com/RookieEnough/De-Vanced) focused on a single goal:
making Google's own apps run against [microG](https://microg.org) instead of Google Play Services.
It contains exactly six user-facing patches — GmsCore support for Gmail, Drive, Maps, Docs,
Sheets and Slides — modeled directly on the battle-tested Google Photos GmsCore patch from
De-Vanced. Everything else from upstream was removed; the Google Photos patch itself lives on in
the upstream repo if you need it. (A Chrome patch existed until v1.0.4 and was removed in v1.0.5 —
Chrome does not need it for the microG use case.)

---

## Status — read this first

| Milestone | State |
| :--- | :--- |
| Launcher activity classes, package names, signing certificate hashes | ✅ Extracted from real APKs (all 6 apps, current releases) |
| Patch-engine fingerprints resolve in the target DEX files | ✅ Verified statically for all 6 apps |
| Gradle build (`:patches:buildAndroid`) | ✅ CI builds and publishes the bundle |
| Version pinning (`targets` in `AppCompatibilities`) | ✅ All 6 pinned to the verified versions — other versions fail by design |
| On-device runtime with microG | 🔄 Gmail: launch + login + account CONFIRMED WORKING (v1.0.3 legacy identity). Drive/Docs/Sheets/Slides: launch + account detection CONFIRMED WORKING (v1.0.4 self-package rewrite); v1.0.5 adds a suite-wide cross-app identity rewrite so the four apps keep resolving each other (file open, editor hand-off, split view) after the rename. Maps: launch + account + routing work; the search-bar avatar chip is an open item. For Morphe MicroG-RE use v1.0.0. |

Every constant in the six `Constants.kt` files was extracted from the actual APK releases listed
below and carries a provenance comment — nothing is a placeholder. The pins are enforced because
the fingerprints depend on obfuscated class names that change with every Google release; patching
an unlisted version will fail at fingerprint resolution. **[GMSCORE_GOOGLE_APPS_GUIDE.md](GMSCORE_GOOGLE_APPS_GUIDE.md)**
covers re-verification commands, the microG setup checklist, field-test results, and logcat triage.

## Google apps patches

Each patch applies the same three-part transformation that the Google Photos patch uses:
the app package is renamed (e.g. `com.google.android.gm` → `app.morphe.android.gm`) so it can be
installed next to the Play Store original and binds to microG's GmsCore instead of Google's,
the original Google signing certificate hash is spoofed so signature checks inside the app pass,
and GmsCore vendor lookups are rewired to the microG package.

| App | Package | Pinned version (versionCode) | Signing key |
| :--- | :--- | :--- | :--- |
| Gmail | `com.google.android.gm` | 2026.08.24.971409176.Release (65987503) | Google Inc. classic |
| Google Drive | `com.google.android.apps.docs` | 2.26.347.3.all.alldpi (214624049) | Google Inc. classic |
| Google Maps | `com.google.android.apps.maps` | 26.34.04.965633971 (1068739428) | Google Inc. classic |
| Google Docs | `com.google.android.apps.docs.editors.docs` | 1.26.341.02.90 (220701916) | Google LLC (newer) |
| Google Sheets | `com.google.android.apps.docs.editors.sheets` | 1.26.341.01.90 (220702133) | Google LLC (newer) |
| Google Slides | `com.google.android.apps.docs.editors.slides` | 1.26.341.01.90 (220702177) | Google LLC (newer) |

If you intend to patch a different version than the one listed, the guide's re-verification
section shows the two commands (`aapt`, `apksigner`) that confirm these values still hold.

Each app's patch set in Morphe Manager consists of **GmsCore support** (the main patch), its
**Extension** dependency (injects the microG compatibility runtime), and the global
**Change package name** helper the engine relies on — the same three-part pattern Google Photos
uses upstream. Toggling *GmsCore support* pulls in the other two automatically.

Two implementation details worth knowing if you plan to extend or debug these patches:

- Five of the six apps declare their launcher through an `activity-alias` whose name does not
  exist in the DEX. The patches target the real `targetActivity` classes instead — e.g. Gmail's
  manifest alias `ConversationListActivityGmail` resolves to `com.google.android.gm.ui.MailActivityGmail`.
- Google signs with two different release certificates: the long-lived "Google Inc." key on
  Gmail/Drive/Maps, and a newer "Google LLC" key (the same one used by Google Photos) on
  Docs/Sheets/Slides. The per-app constants already account for this.
- The Drive suite is one logical app split across four packages (Drive, Docs, Sheets, Slides).
  Since v1.0.5 the four patches share a cross-app identity map so the renamed apps keep
  resolving each other (file open, editor hand-off, split view). Keep the default `app.morphe.*`
  target package names for all four — a custom package name on any suite app desynchronizes
  the family again.

## Importing into Morphe

`patches-list.json` and `patches-bundle.json` at the repo root are **generated files** — they are
rewritten automatically whenever CI cuts a release, and `patches-bundle.json` is what tells Morphe
Manager where to download the patch bundle from. A freshly pushed fork has **no releases yet**, so
those files still point at the upstream repo and Morphe will import the wrong patches. To get your
own bundle:

1. Push your changes to `main` using a [conventional commit](https://www.conventionalcommits.org)
   message (`feat:`, `fix:`, …) — the release workflow only publishes when it sees one.
2. Wait for the *Release* GitHub Action to finish and create a tag.
3. Re-import in Morphe Manager; it will now fetch `patches-<version>.mpp` from **this** repo.

## Building

Prerequisites:

- JDK 21 (Temurin works)
- Android SDK — set `sdk.dir` in `local.properties` at the repo root, or export `ANDROID_HOME`
- A GitHub personal access token with `read:packages` scope (the Morphe Gradle plugin is hosted on
  GitHub Packages, which requires authentication even for public packages)

Put the token in `~/.gradle/gradle.properties` (not the repo one — keep it out of git):

```properties
gpr.user=<your GitHub username>
gpr.key=<your token>
```

Then build the Android patch bundle:

```bash
./gradlew :patches:buildAndroid
```

The bundle lands in `patches/build/libs/patches-<version>.mpp`. Load it in Morphe Manager (or the
Morphe CLI) as your patch source. `patches-list.json` and `patches-bundle.json` in the repo root
are generated files and refresh on the next build/release.

## Testing with microG

1. Use a device or ROM that supports signature spoofing (a microG requirement) and install
   [microG GmsCore](https://microg.org/download.html).
2. In microG settings, enable *Google device registration* and add a Google account if the app
   needs account features (Gmail does).
3. Patch the stock APK with Morphe using the bundle built above, then install the result.
4. Launch the app and watch for early crashes:

```bash
adb logcat | grep -iE "gms|microg|AndroidRuntime"
```

If an app crashes before its main UI appears, capture the stack trace — the guide's
"Re-verification" section explains how to locate the crashing class and, when needed, add an
early-return fingerprint so the GMS check is bypassed instead of answered.

## Credits

- **[De-Vanced](https://github.com/RookieEnough/De-Vanced)** — the fork this project builds on,
  and the Google Photos GmsCore patch these seven patches are modeled on.
- **[ReVanced](https://github.com/ReVanced/revanced-patches)** — original patches (GPL v3);
  preserved source notices live in [`archive/`](archive/archive_contents.txt).
- **[Morphe](https://morphe.software)** — the patcher framework and ecosystem.

## License

GPL v3 — see [LICENSE](LICENSE). The [NOTICE](NOTICE) file adds conditions from GPLv3 §7: this
project is not affiliated with or endorsed by Morphe or Google, and derivatives must use a
distinct project name. Descriptive references such as "patches for use with Morphe" are fine.
