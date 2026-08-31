/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/shared/compat/AppCompatibilities.kt
 *
 * Central Morphe `Compatibility` metadata so Morphe Manager shows human-readable app names.
 */
package app.morphe.patches.shared.compat

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

@Suppress("MemberVisibilityCanBePrivate")
internal object AppCompatibilities {
    // Google apps with GmsCore (microG) support patches.
    //
    // Launcher activities, signature hashes and fingerprint targets were verified against
    // the EXACT versions pinned below (see each app's Constants.kt for provenance).
    // The fingerprints rely on obfuscated class names (e.g. Maps' superclass `Lncu;`,
    // Chrome's `Lv81;`) that change on every Google release, so patching any other
    // version is expected to FAIL at fingerprint resolution.
    // To support a new version: download its APK, re-verify the values per
    // GMSCORE_GOOGLE_APPS_GUIDE.md, then add/replace the AppTarget here.
    //
    // Field status (2026-09-01, on-device with ReVanced GmsCore):
    //   - Gmail: WORKING (v1.0.3 legacy-identity fix, user-confirmed): launch,
    //     account detection, login.
    //   - Maps: launch + account + routing work (user-confirmed). The
    //     search-bar account avatar chip does not render (open item).
    //   - Drive/Docs/Sheets/Slides: launch + account detection WORKING
    //     (v1.0.4 self-package rewrite, user-confirmed 2026-09-01). v1.0.5
    //     adds a suite-wide cross-app identity rewrite so the four apps can
    //     keep resolving each other after all package names changed (file
    //     open via Drive's DocumentsProvider, editor hand-off, split view).
    //   - Chrome: REMOVED in v1.0.5 at the maintainer's request (not needed
    //     for the microG use case).

    val GMAIL = Compatibility(
        name = "Gmail",
        packageName = "com.google.android.gm",
        appIconColor = 0xEA4335,
        targets = listOf(AppTarget("2026.08.24.971409176.Release")),
    )

    val GOOGLE_DRIVE = Compatibility(
        name = "Google Drive",
        packageName = "com.google.android.apps.docs",
        appIconColor = 0x2684FC,
        targets = listOf(AppTarget("2.26.347.3.all.alldpi")),
    )

    val GOOGLE_MAPS = Compatibility(
        name = "Google Maps",
        packageName = "com.google.android.apps.maps",
        appIconColor = 0x34A853,
        targets = listOf(AppTarget("26.34.04.965633971")),
    )

    val GOOGLE_DOCS = Compatibility(
        name = "Google Docs",
        packageName = "com.google.android.apps.docs.editors.docs",
        appIconColor = 0x4285F4,
        targets = listOf(AppTarget("1.26.341.02.90")),
    )

    val GOOGLE_SHEETS = Compatibility(
        name = "Google Sheets",
        packageName = "com.google.android.apps.docs.editors.sheets",
        appIconColor = 0x0F9D58,
        targets = listOf(AppTarget("1.26.341.01.90")),
    )

    val GOOGLE_SLIDES = Compatibility(
        name = "Google Slides",
        packageName = "com.google.android.apps.docs.editors.slides",
        appIconColor = 0xF4B400,
        targets = listOf(AppTarget("1.26.341.01.90")),
    )
}
