package app.morphe.patches.gmaps.misc.gms

/**
 * Verified against Google Maps 26.34.04.965633971 (versionCode 1068739428),
 * minSdk 28, targetSdk 37, base APK (nodpi/universal) obtained from APKCombo
 * on 2026-08-30.
 *
 * IMPORTANT -- fingerprint target differs from the other apps:
 *   - The manifest launcher is the *direct* activity
 *     com.google.android.maps.MapsActivity (NOT an alias), but in this build
 *     MapsActivity is a near-empty shell (1 method) that does NOT define
 *     onCreate itself. R8 has moved it to its obfuscated superclass:
 *       Lncu; -> onCreate(Landroid/os/Bundle;)V  [public final, classes.dex]
 *     The fingerprint below therefore targets Lncu; so it resolves.
 *   - Lncu; has exactly two subclasses: MapsActivity (the launcher) and
 *     MapsPreviewActivity. The GmsCore check will also run for
 *     MapsPreviewActivity; in the happy path checkGmsCore() is a silent
 *     no-op, so this is acceptable for a first working patch.
 *   - If a future Maps build again defines onCreate directly on
 *     MapsActivity, switch the constant back to
 *     "Lcom/google/android/maps/MapsActivity;".
 *   - Signature: APK Signature Scheme v2, single signer, the classic
 *     Google-wide release certificate (same key as Gmail/Drive/Chrome).
 *   - "Google Play Services not available" (ServiceCheckFingerprint) is
 *     present in classes2.dex and classes8.dex.
 *
 * Maps is the most complex of the set (native rendering, fused location
 * providers, heavy GMS surface area). Expect to need additional
 * primeMethodFingerprint / earlyReturnFingerprints work after on-device
 * testing with MicroG, beyond what the activity hook provides.
 */
internal object Constants {
    const val GMAPS_PACKAGE_NAME = "com.google.android.apps.maps"
    const val MORPHE_GMAPS_PACKAGE_NAME = "app.morphe.android.apps.maps"

    // Superclass that defines the launcher onCreate in this build
    // (MapsActivity itself no longer defines it; see header comment).
    const val GMAPS_MAIN_ACTIVITY_CLASS_TYPE =
        "Lncu;"

    // SHA-1 of the original APK's v2 signer certificate (Google Inc. release key).
    // SHA-256 for cross-check: f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83
    const val GMAPS_SPOOFED_PACKAGE_SIGNATURE =
        "38918a453d07199354f8b19af05ec6562ced5788"
}
