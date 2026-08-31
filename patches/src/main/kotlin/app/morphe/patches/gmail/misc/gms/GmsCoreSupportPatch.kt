package app.morphe.patches.gmail.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.gmail.misc.gms.Constants.GMAIL_PACKAGE_NAME
import app.morphe.patches.gmail.misc.gms.Constants.MORPHE_GMAIL_PACKAGE_NAME
import app.morphe.patches.gmail.misc.gms.Constants.GMAIL_SPOOFED_PACKAGE_SIGNATURE
import app.morphe.patches.gmail.misc.gms.HomeActivityOnCreateFingerprint
import app.morphe.patches.gmail.misc.extension.sharedExtensionPatch
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference

/**
 * Gmail GmsCore support.
 *
 * Modeled directly on the working Google Photos implementation in
 * app.morphe.patches.googlephotos.misc.gms.GmsCoreSupportPatch.
 *
 * Status: every constant below is verified against the real APK (see
 * Constants.kt for provenance and re-verification instructions). Static
 * verification is complete; runtime behavior under microG still requires
 * on-device testing.
 *
 * v1.0.3: Gmail runs in "legacy identity" mode (keepOriginalPackageScopedNames).
 * Gmail is account/sync-centric and references its own package-scoped names from
 * THREE different places at once: DEX string constants (internal URI matchers and
 * queries), res/xml (sync adapters, notification URIs) and the manifest (provider
 * authorities, often in multi-authority attributes that the prefix rename never
 * touches). Renaming any one side desynchronizes the others and crashes the app
 * (v1.0.1: "Failed to find provider com.google.android.gm.sapi"; v1.0.2:
 * "Unknown uri: content://com.google.android.gm.email.provider/uiaccts" and
 * "Failed to find provider info for app.morphe.android.gm.email.provider").
 * Keeping the original authorities/permissions everywhere -- while patching only
 * package attribute, GMS vendor strings, c2dm permissions, sync adapter account
 * types and the receiver permission guard -- is the only strategy that cannot
 * miss a source.
 */
@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = GMAIL_PACKAGE_NAME,
    toPackageName = MORPHE_GMAIL_PACKAGE_NAME,
    keepOriginalPackageScopedNames = true,
    mainActivityOnCreateFingerprint = HomeActivityOnCreateFingerprint,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
) {
    compatibleWith(AppCompatibilities.GMAIL)
}

/**
 * Minimal preference screen used only to satisfy the shared GmsCore support
 * resource patch API. Replace with a real settings screen if/when this repo
 * gets a dedicated Morphe settings UI for Gmail.
 */
private object DummyPreferenceScreen : BasePreferenceScreen() {
    val SCREEN = Screen(
        key = "morphe_settings_gmail_screen_1_misc",
        summaryKey = null,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        // No-op: no dedicated settings screen for this app yet.
    }
}

private fun gmsCoreSupportResourcePatch() =
    app.morphe.patches.shared.misc.gms.gmsCoreSupportResourcePatch(
        fromPackageName = GMAIL_PACKAGE_NAME,
        toPackageName = MORPHE_GMAIL_PACKAGE_NAME,
        spoofedPackageSignature = GMAIL_SPOOFED_PACKAGE_SIGNATURE,
        screen = DummyPreferenceScreen.SCREEN,
        keepOriginalPackageScopedNames = true,
    )
