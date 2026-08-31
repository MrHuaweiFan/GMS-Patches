package app.morphe.patches.gmaps.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.gmaps.misc.gms.Constants.GMAPS_PACKAGE_NAME
import app.morphe.patches.gmaps.misc.gms.Constants.MORPHE_GMAPS_PACKAGE_NAME
import app.morphe.patches.gmaps.misc.gms.Constants.GMAPS_SPOOFED_PACKAGE_SIGNATURE
import app.morphe.patches.gmaps.misc.gms.HomeActivityOnCreateFingerprint
import app.morphe.patches.gmaps.misc.extension.sharedExtensionPatch
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference

/**
 * Google Maps GmsCore support.
 *
 * Modeled directly on the working Google Photos implementation in
 * app.morphe.patches.googlephotos.misc.gms.GmsCoreSupportPatch.
 *
 * Status: every constant below is verified against the real APK (see
 * Constants.kt for provenance and re-verification instructions). Static
 * verification is complete; runtime behavior under microG still requires
 * on-device testing.
 */
@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = GMAPS_PACKAGE_NAME,
    toPackageName = MORPHE_GMAPS_PACKAGE_NAME,
    mainActivityOnCreateFingerprint = HomeActivityOnCreateFingerprint,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
) {
    compatibleWith(AppCompatibilities.GOOGLE_MAPS)
}

/**
 * Minimal preference screen used only to satisfy the shared GmsCore support
 * resource patch API. Replace with a real settings screen if/when this repo
 * gets a dedicated Morphe settings UI for Google Maps.
 */
private object DummyPreferenceScreen : BasePreferenceScreen() {
    val SCREEN = Screen(
        key = "morphe_settings_gmaps_screen_1_misc",
        summaryKey = null,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        // No-op: no dedicated settings screen for this app yet.
    }
}

private fun gmsCoreSupportResourcePatch() =
    app.morphe.patches.shared.misc.gms.gmsCoreSupportResourcePatch(
        fromPackageName = GMAPS_PACKAGE_NAME,
        toPackageName = MORPHE_GMAPS_PACKAGE_NAME,
        spoofedPackageSignature = GMAPS_SPOOFED_PACKAGE_SIGNATURE,
        screen = DummyPreferenceScreen.SCREEN,
    )
