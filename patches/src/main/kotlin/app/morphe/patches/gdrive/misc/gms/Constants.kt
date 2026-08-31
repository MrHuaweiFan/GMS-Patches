package app.morphe.patches.gdrive.misc.gms

/**
 * Verified against Google Drive 2.26.347.3.all.alldpi (versionCode 214624049),
 * minSdk 26, targetSdk 37, base APK obtained from APKCombo on 2026-08-30.
 *
 * How these values were obtained:
 *   - The manifest launchable entry is the *activity-alias*
 *     "com.google.android.apps.docs.app.NewMainProxyActivity", whose
 *     android:targetActivity is the real class
 *     com.google.android.apps.docs.drive.startup.StartupActivity.
 *   - StartupActivity exists in classes2.dex and directly defines a
 *     public final onCreate(Landroid/os/Bundle;)V, so the
 *     HomeActivityOnCreateFingerprint resolves.
 *   - Signature: APK Signature Scheme v2, single signer
 *     CN=Android, OU=Android, O=Google Inc. (the classic Google-wide release
 *     certificate, valid 2008-2036 -- the same key that signs Gmail/Maps/Chrome).
 *   - "Google Play Services not available" (ServiceCheckFingerprint) is present
 *     in classes2.dex, so the shared ServiceCheck hook resolves as well.
 */
internal object Constants {
    const val GDRIVE_PACKAGE_NAME = "com.google.android.apps.docs"
    const val MORPHE_GDRIVE_PACKAGE_NAME = "app.morphe.android.apps.docs"

    // Real launcher activity class (target of the NewMainProxyActivity alias).
    // Exists in classes2.dex with a direct public final onCreate(Bundle)V.
    const val GDRIVE_MAIN_ACTIVITY_CLASS_TYPE =
        "Lcom/google/android/apps/docs/drive/startup/StartupActivity;"

    // SHA-1 of the original APK's v2 signer certificate (Google Inc. release key).
    // SHA-256 for cross-check: f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83
    const val GDRIVE_SPOOFED_PACKAGE_SIGNATURE =
        "38918a453d07199354f8b19af05ec6562ced5788"
}
