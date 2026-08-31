package app.morphe.patches.gslides.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.gslides.misc.gms.Constants.GSLIDES_PACKAGE_NAME
import app.morphe.patches.gslides.misc.gms.Constants.MORPHE_GSLIDES_PACKAGE_NAME
import app.morphe.patches.gslides.misc.gms.Constants.GSLIDES_SPOOFED_PACKAGE_SIGNATURE
import app.morphe.patches.gslides.misc.gms.HomeActivityOnCreateFingerprint
import app.morphe.patches.gslides.misc.extension.sharedExtensionPatch
import app.morphe.patches.shared.misc.gms.DRIVE_SUITE_CONTENT_URI_RENAMES
import app.morphe.patches.shared.misc.gms.DRIVE_SUITE_CROSS_APP_RENAMES
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference

/**
 * Google Slides GmsCore support.
 *
 * Modeled directly on the working Google Photos implementation in
 * app.morphe.patches.googlephotos.misc.gms.GmsCoreSupportPatch.
 *
 * v1.0.4: rewriteSelfPackageNameStrings is enabled because the editors apps
 * validate their runtime package against a hardcoded package allowlist and
 * crash when the renamed package never matches. Slides is the only suite app
 * with an EXPLICIT crash message for this: IllegalStateException
 * "Invalid app package: app.morphe.android.apps.docs.editors.slides" at
 * ayjx.h (bug report DBY-W09NM-2026-08-31-16-31-39, 16:11:15.750), plus 11
 * identical startup NPEs (akzc.a/ydl.b/bdmj.lN).
 *
 * v1.0.5: crossAppPackageRenames is enabled with the shared suite family map.
 * The Drive suite (Drive/Docs/Sheets/Slides) is one app split across four
 * packages: after the rename, cross-app lookups (editor hand-off by package
 * name, URI security checks against Drive's renamed .storage authorities, the
 * PackageInfoHelper "am I an editor" prefix check, the SAF open-file root URI)
 * all pointed at names that no longer exist. The family map rewrites them
 * consistently in every suite app, restoring file open and editor hand-off.
 *
 * Status: every constant below is verified against the real APK (see
 * Constants.kt for provenance and re-verification instructions). Static
 * verification is complete; runtime behavior under microG still requires
 * on-device testing.
 */
@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = GSLIDES_PACKAGE_NAME,
    toPackageName = MORPHE_GSLIDES_PACKAGE_NAME,
    rewriteSelfPackageNameStrings = true,
    crossAppPackageRenames = DRIVE_SUITE_CROSS_APP_RENAMES,
    crossAppContentUriRenames = DRIVE_SUITE_CONTENT_URI_RENAMES,
    mainActivityOnCreateFingerprint = HomeActivityOnCreateFingerprint,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
) {
    compatibleWith(AppCompatibilities.GOOGLE_SLIDES)
}

/**
 * Minimal preference screen used only to satisfy the shared GmsCore support
 * resource patch API. Replace with a real settings screen if/when this repo
 * gets a dedicated Morphe settings UI for Google Slides.
 */
private object DummyPreferenceScreen : BasePreferenceScreen() {
    val SCREEN = Screen(
        key = "morphe_settings_gslides_screen_1_misc",
        summaryKey = null,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        // No-op: no dedicated settings screen for this app yet.
    }
}

private fun gmsCoreSupportResourcePatch() =
    app.morphe.patches.shared.misc.gms.gmsCoreSupportResourcePatch(
        fromPackageName = GSLIDES_PACKAGE_NAME,
        toPackageName = MORPHE_GSLIDES_PACKAGE_NAME,
        spoofedPackageSignature = GSLIDES_SPOOFED_PACKAGE_SIGNATURE,
        screen = DummyPreferenceScreen.SCREEN,
    )
