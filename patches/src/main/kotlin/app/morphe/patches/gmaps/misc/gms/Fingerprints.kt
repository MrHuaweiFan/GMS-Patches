package app.morphe.patches.gmaps.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patches.gmaps.misc.gms.Constants.GMAPS_MAIN_ACTIVITY_CLASS_TYPE

/**
 * Matches the onCreate(Bundle) method executed when the Maps launcher
 * activity starts, triggering the GmsCore availability check as early as
 * possible.
 *
 * VERIFIED against Maps 26.34.04.965633971: in this build the launcher
 * class Lcom/google/android/maps/MapsActivity; is a 1-method shell and does
 * NOT define onCreate. The method lives on its obfuscated direct superclass
 * Lncu; (48 methods, classes.dex) as a public final
 * onCreate(Landroid/os/Bundle;)V, which is what this fingerprint targets.
 * Lncu; also covers MapsPreviewActivity; see Constants.kt for details.
 * Lncu; is an R8 name and will change between versions.
 *
 * NOTE: Some apps also gate GmsCore-only calls behind other classes (a
 * "prime" method executed even earlier, or background services/receivers
 * that need an early-return patch). Maps, with its fused-location and
 * native-rendering GMS surface, is the most likely of the seven apps to
 * need these -- add them only if you observe the app still crashing or
 * hanging on a genuine com.google.android.gms call *after* this patch is
 * applied and MicroG is installed.
 */
internal object HomeActivityOnCreateFingerprint : Fingerprint(
    definingClass = GMAPS_MAIN_ACTIVITY_CLASS_TYPE,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
