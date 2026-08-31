package app.morphe.patches.gmail.misc.gms

/**
 * Verified against Gmail 2026.08.24.971409176.Release (versionCode 65987503),
 * minSdk 24, targetSdk 37, downloaded from APKCombo on 2026-08-30.
 *
 * How these values were obtained (see scripts/extract_apk_info.py in the
 * working environment):
 *   - Launchable activity: the manifest declares
 *     android.intent.action.MAIN / LAUNCHER on the *activity-alias*
 *     "com.google.android.gm.ConversationListActivityGmail", whose
 *     android:targetActivity is the real class
 *     com.google.android.gm.ui.MailActivityGmail. The alias itself does NOT
 *     exist in the DEX; patching must target the real class below.
 *   - The real class directly defines a public final
 *     onCreate(Landroid/os/Bundle;)V (classes2.dex), so the
 *     HomeActivityOnCreateFingerprint resolves.
 *   - Signature: APK Signature Scheme v2, single signer
 *     CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US
 *     (the long-lived Google-wide release certificate, valid 2008-2036).
 *
 * NOTE: Google ships Gmail as an app bundle; these values were verified on the
 * base APK. Version-specific activity names have been stable for years
 * (MailActivityGmail dates back to the 2016 rewrite) but re-verify when
 * targeting a materially different version.
 */
internal object Constants {
    const val GMAIL_PACKAGE_NAME = "com.google.android.gm"
    const val MORPHE_GMAIL_PACKAGE_NAME = "app.morphe.android.gm"

    // Real launcher activity class (target of the ConversationListActivityGmail alias).
    // Exists in classes2.dex with a direct public final onCreate(Bundle)V.
    const val GMAIL_MAIN_ACTIVITY_CLASS_TYPE =
        "Lcom/google/android/gm/ui/MailActivityGmail;"

    // SHA-1 of the original APK's v2 signer certificate (Google Inc. release key).
    // SHA-256 for cross-check: f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83
    const val GMAIL_SPOOFED_PACKAGE_SIGNATURE =
        "38918a453d07199354f8b19af05ec6562ced5788"
}
