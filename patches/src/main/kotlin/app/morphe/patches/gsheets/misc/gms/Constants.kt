package app.morphe.patches.gsheets.misc.gms

/**
 * Verified against Google Sheets 1.26.341.01.90 (versionCode 220702133),
 * minSdk 29, targetSdk 37, base APK obtained from APKCombo on 2026-08-30.
 *
 * How these values were obtained:
 *   - The manifest launchable entry is the *activity-alias*
 *     "com.google.android.apps.docs.app.NewMainProxyActivity", whose
 *     android:targetActivity is the real class
 *     com.google.android.apps.docs.editors.homescreen.ProxyLaunchActivity
 *     (same shared launcher architecture as Docs and Slides).
 *   - ProxyLaunchActivity exists in classes.dex and directly defines a
 *     public final onCreate(Landroid/os/Bundle;)V, so the
 *     HomeActivityOnCreateFingerprint resolves.
 *   - Signature: APK Signature Scheme v2, single signer CN=Google LLC --
 *     the newer Google certificate (same key as Google Photos and the other
 *     Docs editors), NOT the classic key used by Gmail/Drive/Maps/Chrome.
 *   - "Google Play Services not available" (ServiceCheckFingerprint) and
 *     the GooglePlayUtility strings are present in classes.dex/classes2.dex.
 */
internal object Constants {
    const val GSHEETS_PACKAGE_NAME = "com.google.android.apps.docs.editors.sheets"
    const val MORPHE_GSHEETS_PACKAGE_NAME = "app.morphe.android.apps.docs.editors.sheets"

    // Real launcher activity class (target of the NewMainProxyActivity alias).
    // Exists in classes.dex with a direct public final onCreate(Bundle)V.
    const val GSHEETS_MAIN_ACTIVITY_CLASS_TYPE =
        "Lcom/google/android/apps/docs/editors/homescreen/ProxyLaunchActivity;"

    // SHA-1 of the original APK's v2 signer certificate (Google LLC newer release key,
    // same as Google Photos).
    // SHA-256 for cross-check: 3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a
    const val GSHEETS_SPOOFED_PACKAGE_SIGNATURE =
        "24bb24c05e47e0aefa68a58a766179d9b613a600"
}
