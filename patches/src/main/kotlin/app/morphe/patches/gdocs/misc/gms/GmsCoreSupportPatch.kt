package app.morphe.patches.gdocs.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.gdocs.misc.gms.Constants.GDOCS_PACKAGE_NAME
import app.morphe.patches.gdocs.misc.gms.Constants.MORPHE_GDOCS_PACKAGE_NAME
import app.morphe.patches.gdocs.misc.gms.Constants.GDOCS_SPOOFED_PACKAGE_SIGNATURE
import app.morphe.patches.gdocs.misc.gms.HomeActivityOnCreateFingerprint
import app.morphe.patches.gdocs.misc.extension.sharedExtensionPatch
import app.morphe.patches.shared.misc.gms.DRIVE_SUITE_CONTENT_URI_RENAMES
import app.morphe.patches.shared.misc.gms.DRIVE_SUITE_CROSS_APP_RENAMES
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference

/**
 * Google Docs GmsCore support.
 *
 * Modeled directly on the working Google Photos implementation in
 * app.morphe.patches.googlephotos.misc.gms.GmsCoreSupportPatch.
 *
 * v1.0.4: rewriteSelfPackageNameStrings is enabled because the editors apps
 * validate their runtime package against a hardcoded package allowlist and
 * crash when the renamed package never matches ("Invalid app package" family;
 * Docs NPE stack amvt.a/abv.b/bfqy.ja in bug report
 * DBY-W09NM-2026-08-31-16-31-39, 16:19-16:31 era).
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
    fromPackageName = GDOCS_PACKAGE_NAME,
    toPackageName = MORPHE_GDOCS_PACKAGE_NAME,
    rewriteSelfPackageNameStrings = true,
    crossAppPackageRenames = DRIVE_SUITE_CROSS_APP_RENAMES,
    crossAppContentUriRenames = DRIVE_SUITE_CONTENT_URI_RENAMES,
    mainActivityOnCreateFingerprint = HomeActivityOnCreateFingerprint,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
) {
    compatibleWith(AppCompatibilities.GOOGLE_DOCS)
}

/**
 * Minimal preference screen used only to satisfy the shared GmsCore support
 * resource patch API. Replace with a real settings screen if/when this repo
 * gets a dedicated Morphe settings UI for Google Docs.
 */
private object DummyPreferenceScreen : BasePreferenceScreen() {
    val SCREEN = Screen(
        key = "morphe_settings_gdocs_screen_1_misc",
        summaryKey = null,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        // No-op: no dedicated settings screen for this app yet.
    }
}

private fun gmsCoreSupportResourcePatch() =
    app.morphe.patches.shared.misc.gms.gmsCoreSupportResourcePatch(
        fromPackageName = GDOCS_PACKAGE_NAME,
        toPackageName = MORPHE_GDOCS_PACKAGE_NAME,
        spoofedPackageSignature = GDOCS_SPOOFED_PACKAGE_SIGNATURE,
        screen = DummyPreferenceScreen.SCREEN,
    )
