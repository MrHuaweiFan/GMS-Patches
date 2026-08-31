package app.morphe.patches.gmail.misc.extension

import app.morphe.patches.gmail.misc.gms.HomeActivityOnCreateFingerprint
import app.morphe.patches.shared.misc.extension.ExtensionHook
import app.morphe.patches.shared.misc.extension.sharedExtensionPatch

/**
 * Passes the Activity context into the extension's GmsCoreSupportPatch as
 * soon as Gmail's main Activity is created, mirroring Google Photos'
 * app.morphe.patches.googlephotos.misc.extension.SharedExtensionPatch.
 */
private class HomeActivityInitHook : ExtensionHook(
    fingerprint = HomeActivityOnCreateFingerprint,
    insertIndexResolver = { 0 },
    contextRegisterResolver = { "p0" },
)

internal val homeActivityInitHook: ExtensionHook = HomeActivityInitHook()

// isYouTubeOrYouTubeMusic = true is intentional and matches Google Photos:
// GmsCoreSupportPatch.java currently only ships inside the "shared-youtube"
// extension bundle in this fork, despite the name. If this repo ever moves
// GmsCoreSupportPatch into the plain "shared" bundle, switch this to false.
val sharedExtensionPatch = sharedExtensionPatch(
    isYouTubeOrYouTubeMusic = true,
    homeActivityInitHook,
)
