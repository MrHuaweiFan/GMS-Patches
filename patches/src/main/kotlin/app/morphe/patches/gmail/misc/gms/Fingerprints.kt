package app.morphe.patches.gmail.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patches.gmail.misc.gms.Constants.GMAIL_MAIN_ACTIVITY_CLASS_TYPE

/**
 * Matches the onCreate(Bundle) method of Gmail's real launcher Activity.
 *
 * VERIFIED against Gmail 2026.08.24.971409176.Release: the manifest's
 * launchable entry "com.google.android.gm.ConversationListActivityGmail" is an
 * activity-alias; its targetActivity is com.google.android.gm.ui.MailActivityGmail,
 * which directly defines a public final onCreate(Landroid/os/Bundle;)V in
 * classes2.dex. This fingerprint therefore resolves.
 *
 * Additional shared-fingerprint verification against the same APK:
 *   - ServiceCheckFingerprint       -> Labkz;->d(Landroid/content/Context;I)V
 *     (bundled obfuscated GooglePlayServicesUtil error path) -- resolves.
 *   - GooglePlayUtilityFingerprint  -> Labkz;->b(Landroid/content/Context;I)I
 *     -- resolves (it is optional in the shared patch, but present).
 *
 * NOTE: Some apps also gate GmsCore-only calls behind other classes (a
 * "prime" method executed even earlier, or background services/receivers
 * that need an early-return patch). Google Photos did not need these extra
 * fingerprints, but Gmail may -- in particular its Application class
 * (com.google.android.apps.gmail.application.tiktok.Hub_Application, extends
 * an obfuscated Lvqr;) runs before any activity. Only add extra fingerprints
 * if you observe the app still crashing or hanging on a genuine
 * com.google.android.gms call *after* this patch is applied and MicroG is
 * installed.
 */
internal object HomeActivityOnCreateFingerprint : Fingerprint(
    definingClass = GMAIL_MAIN_ACTIVITY_CLASS_TYPE,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
