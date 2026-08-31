package app.morphe.patches.gdocs.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patches.gdocs.misc.gms.Constants.GDOCS_MAIN_ACTIVITY_CLASS_TYPE

/**
 * VERIFIED against Docs 1.26.341.02.90: targets ProxyLaunchActivity (the
 * real class behind the NewMainProxyActivity launcher alias), which directly
 * defines public final onCreate(Landroid/os/Bundle;)V in classes.dex.
 * Matches the onCreate(Bundle) method of Google Docs's main/launcher Activity.
 * This is the standard hook point used to trigger the GmsCore availability
 * check as early as possible in the app's lifecycle.
 *
 * NOTE: Some apps also gate GmsCore-only calls behind other classes (a
 * "prime" method executed even earlier, or background services/receivers
 * that need an early-return patch). Google Photos did not need these extra
 * fingerprints, but Google Docs may. Only add them if you observe the app
 * still crashing or hanging on a genuine com.google.android.gms call
 * *after* this fingerprint-based patch is applied and MicroG is installed.
 */
internal object HomeActivityOnCreateFingerprint : Fingerprint(
    definingClass = GDOCS_MAIN_ACTIVITY_CLASS_TYPE,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
