# GmsCore support for Gmail, Drive, Maps, Docs, Sheets, Slides

> NOTE (v1.0.5): the Chrome patch was REMOVED at the maintainer's request — Chrome
> does not need it for the microG use case. The historical Chrome notes below are
> kept for reference only.

This repo is a copy of **De-Vanced** with new scaffolding added under
`patches/src/main/kotlin/app/morphe/patches/{gmail,gdrive,gmaps,gchrome,gdocs,gsheets,gslides}/`
so you can extend the existing "GmsCore support" pattern (currently only
wired up for Google Photos) to more Google apps.

## What already works (reused, not reinvented)

The reusable engine lives in `patches/.../shared/misc/gms/GmsCoreSupportPatch.kt`.
It rewrites `com.google.android.gms` strings/URIs/permissions to a chosen
vendor id, changes the app's own package name, and adds manifest metadata so
a GmsCore implementation (MicroG) can be spoofed as Google Play Services.
The companion Java class (`extensions/shared-youtube/.../GmsCoreSupportPatch.java`)
checks at runtime that GmsCore/MicroG is installed, not battery-restricted,
and running in the background — prompting the user if not.

Google Photos' working implementation
(`patches/.../googlephotos/misc/gms/`) was used as the template for the new
scaffolding.

## What was previously missing -- now VERIFIED (2026-08-30)

All launcher activity classes and signing certificate hashes below were
extracted from real APKs (obtained from APKCombo; see "Provenance" at the
end) using automated manifest + DEX + signature-block analysis. The
`Constants.kt` for every app is filled in with these values.

| App | Verified version (vc) | Launcher manifest entry | REAL fingerprint target class | Signature SHA-1 |
|-----|----------------------|-------------------------|-------------------------------|-----------------|
| Gmail | 2026.08.24.971409176.Release (65987503) | `ConversationListActivityGmail` **(alias)** | `Lcom/google/android/gm/ui/MailActivityGmail;` (direct `onCreate`) | `38918a453d07199354f8b19af05ec6562ced5788` |
| Drive | 2.26.347.3.all.alldpi (214624049) | `NewMainProxyActivity` **(alias)** | `Lcom/google/android/apps/docs/drive/startup/StartupActivity;` (direct `onCreate`) | `38918a453d07199354f8b19af05ec6562ced5788` |
| Maps | 26.34.04.965633971 (1068739428) | `MapsActivity` (direct activity) | `Lncu;` -- obfuscated superclass defines `onCreate` | `38918a453d07199354f8b19af05ec6562ced5788` |
| Chrome | 152.0.7977.64 (797706400) | `Main` **(alias)** -> `ChromeTabbedActivity` | `Lv81;` -- obfuscated ChromeActivity base defines `onCreate` | `38918a453d07199354f8b19af05ec6562ced5788` |
| Docs | 1.26.341.02.90 (220701916) | `NewMainProxyActivity` **(alias)** | `Lcom/google/android/apps/docs/editors/homescreen/ProxyLaunchActivity;` (direct `onCreate`) | `24bb24c05e47e0aefa68a58a766179d9b613a600` |
| Sheets | 1.26.341.01.90 (220702133) | `NewMainProxyActivity` **(alias)** | `Lcom/google/android/apps/docs/editors/homescreen/ProxyLaunchActivity;` (direct `onCreate`) | `24bb24c05e47e0aefa68a58a766179d9b613a600` |
| Slides | 1.26.341.01.90 (220702177) | `NewMainProxyActivity` **(alias)** | `Lcom/google/android/apps/docs/editors/homescreen/ProxyLaunchActivity;` (direct `onCreate`) | `24bb24c05e47e0aefa68a58a766179d9b613a600` |

### Key findings from the verification pass

1. **Five of seven launchers are `activity-alias` entries.** What the
   manifest advertises as the launchable activity usually does not exist as
   a DEX class. The patch fingerprint must target the alias's
   `android:targetActivity` (or wherever that class's `onCreate` actually
   lives) or it will not resolve.
2. **Two apps need obfuscated-superclass hooks.** Maps' `MapsActivity` is a
   1-method shell whose `onCreate(Bundle)V` is defined on `Lncu;`; Chrome's
   `ChromeTabbedActivity` gets `onCreate(Bundle)V` from `Lv81;`. These
   obfuscated names are R8 artifacts and WILL change between versions --
   re-verify them whenever you bump the target version.
3. **Two different signing keys are in play.** Gmail/Drive/Maps/Chrome use
   the classic Google-wide certificate (`38918a...`, CN=Android, O=Google Inc.,
   valid 2008-2036). Docs/Sheets/Slides use the newer Google LLC certificate
   (`24bb24...`, the same key that signs Google Photos). Never copy a
   signature hash between app families.
4. **The shared engine's mandatory fingerprints resolve in all seven APKs.**
   "Google Play Services not available" (ServiceCheckFingerprint) was found
   in every APK; the GooglePlayUtility string triple was found in
   Gmail/Chrome/Docs/Sheets/Slides (it is optional and absent in Maps, which
   is handled gracefully by the shared patch code).
5. **Gmail's launcher history**: `ConversationListActivityGmail` has been
   Gmail's public launcher name since the 2016 Material rewrite, and
   `MailActivityGmail` has been its real target throughout -- but the
   `onCreate` may move (see Maps/Chrome) so the per-version verification
   step below still applies when you bump versions.

### How to re-find each value when bumping versions

1. **Launcher activity class**
   ```
   aapt dump badging yourapp.apk | grep launchable-activity
   ```
   Then check the manifest: if the launchable name is an
   `<activity-alias>`, follow `android:targetActivity`. Finally confirm the
   target class actually defines `onCreate(Landroid/os/Bundle;)V` in the DEX
   (jadx-gui: open the class, look for a directly-declared `onCreate`); if
   it does not, walk `extends` up the chain until you find the class that
   does, and use that class in the fingerprint (see Maps/Chrome notes).
   Convert `com.example.Foo` -> smali form `Lcom/example/Foo;`.

2. **Signing certificate SHA-1**
   ```
   apksigner verify --print-certs yourapp.apk
   ```
   (or `keytool -printcert -jarfile` for v1-only APKs; for v2/v3-only
   APKs use apksigner or androguard). Use the SHA-1 of the first/only
   signer's certificate. Expect `38918a...` for Gmail/Drive/Maps/Chrome and
   `24bb24...` for Docs/Sheets/Slides per the table above.

3. **Whether you need more than the activity `onCreate` fingerprint**

   Google Photos only needed `mainActivityOnCreateFingerprint` -- no
   `primeMethodFingerprint`, no `earlyReturnFingerprints`. Some apps check
   for Play Services earlier (e.g. in `Application.onCreate`, a boot
   receiver, or a background service) before the main Activity ever
   launches. You'll only discover this by patching with just the Activity
   hook first, testing against a real MicroG install, and watching logcat
   (`adb logcat | grep -i gms`) for crashes or ANRs that happen before your
   hook fires. If you find one, add a fingerprint for that method the same
   way `shared/misc/gms/Fingerprints.kt` and `GmsCoreSupportPatch.kt`
   already support via `primeMethodFingerprint` / `earlyReturnFingerprints`.
   Known candidates to watch for:
   - Gmail: `com.google.android.apps.gmail.application.tiktok.Hub_Application`
     (custom Application class, runs before any activity)
   - Drive/Docs/Sheets/Slides: `com.google.android.apps.docs.drive.DriveApplication`
     or the editors' equivalent Application class
   - Maps: fused-location provider initialization in services/receivers --
     Maps is the most likely to need early-return patches.

4. **App-specific quirks**

   Google Photos needed one extra patch beyond GmsCore support itself:
   `googlephotos/misc/login/SelectedAccountPatch.kt`, which stops the app
   from clearing the signed-in account after a cold start under MicroG.
   That patch matches obfuscated (ProGuard'd) method names specific to one
   Photos build, found by decompiling that exact APK — there's no way to
   write the equivalent for Gmail/Drive/etc. without decompiling those
   apps' current builds and watching what breaks under MicroG. Expect to
   need something similar for at least some of these apps.

## A note on Chrome specifically

Chrome doesn't gate its core browsing functionality behind a Play Services
presence check the way Gmail/Drive/Photos do — it will simply run without
GmsCore installed at all. The only Play-Services-dependent features are
Google Account sign-in/sync and some Safe Browsing/autofill calls. A
GmsCoreSupportPatch in the Photos style may not accomplish much for Chrome
in practice; `gchrome/` is scaffolded for symmetry, but you may get more
value from treating Chrome sync separately (it's closer to the account/auth
plumbing than to a hard GmsCore dependency), or skipping it. Worth
confirming, on your own patched build, that there's an actual problem this
patch needs to solve before investing time here.

## Version pinning (done)

All seven patches pin their verified version in `shared/compat/AppCompatibilities.kt`
via `targets = listOf(AppTarget("<version>"))`. This is mandatory, not cosmetic:
the fingerprints depend on obfuscated class names (`Lncu;`, `Lv81;`, `Labkz;`) that
change with essentially every Google release, so patching an unpinned version
fails at fingerprint resolution — this was confirmed on device (patches for
other versions failed; the pinned versions apply cleanly).

To support a new release: obtain its base APK, re-verify launcher activity and
certificate per the "How to re-find each value" section above, update the
per-app `Constants.kt` if anything changed, and add/replace the `AppTarget`
entry with the new exact `versionName` string.

## Field test results and troubleshooting (2026-08-30)

First on-device results with the pinned versions, real microG install:

| App | Patching | Launch | Behavior under microG |
| :-- | :-- | :-- | :-- |
| Gmail | ✅ applies | ✅ launches | ❌ Stuck on loading, then crashes |
| Maps | ✅ applies | ✅ launches | ⚠️ UI loads; map tiles never render; no account button; in-app login errors |
| Drive, Chrome, Docs, Sheets, Slides | ✅ expected | untested | — |

### microG setup checklist (do this before blaming the patches)

1. **Correct microG build**: the patched apps look for GmsCore under the
   package `app.revanced.android.gms` (the engine rewrites every
   `com.google.android.gms` reference to the `app.revanced` vendor group and
   the manifest adds a `<queries>` entry for it). A stock microG
   (`com.google.android.gms`) is NOT what these apps talk to — use the build
   linked from Morphe (`https://morphe.software/microg`). If it were missing
   you'd see a "GmsCore is not installed" toast and be thrown to the download
   page, so reaching a loading spinner means this part is fine.
2. **Add the Google account inside microG**, not inside the patched app:
   microG Settings → Google account → add account. The in-app sign-in flows of
   Google's own apps use proprietary dialogs that validate package name +
   signature server-side and will error out on a renamed package (exactly the
   Maps login error observed). The account must come from the system
   AccountManager, which microG provides. The account type also matches:
   microG registers accounts with type `app.revanced`, and the patch rewrites
   the app's account-type string `com.google` to `app.revanced` for exactly
   this reason.
3. **Enable Google device registration** in microG settings (Checkin). Without
   it, auth token requests for the account can fail.
4. **Watch for the first-run consent dialog**: when a patched app first
   requests an auth token, microG shows an "app wants access to account data"
   consent dialog (the patched app presents as its original Google-signed
   identity, but microG still asks once per app). Approve it — until you do,
   the app cannot get a token and will wait or error out.
5. **Battery**: whitelist microG from battery optimization (the patched app
   nags about this on every launch until done).
6. **Permissions**: grant everything (especially Contacts/Phone state) to both
   microG and the patched app, then reboot once.

### Known limitation: Maps can never fully work

This is an ecosystem limit, not a patch bug. The Google Maps app renders its
map through proprietary GMS modules and fetches tiles from Google's servers
with device-bound credentials. microG does not implement those modules, so the
map view stays blank regardless of any bytecode patching. What the patch does
give Maps is a non-crashing install that coexists with the Play Store original.
If map rendering is the goal, Maps is the wrong app to patch — treat it as
permanently partial or drop it from the bundle.

### Root cause of "Gmail stuck on loading, then crashes" — FOUND and FIXED (2026-08-31)

The shared GmsCore engine renamed *every* intent action in its (YouTube-era)
`ACTIONS` list from `com.google.…` to `app.revanced.…`. That is only correct
for services the microG build actually *serves under the renamed name*. The
Morphe microG build 6.1.4 (package `app.revanced.android.gms`) serves several
of those actions **only under their literal `com.google.…` names**, mostly via
its `DummyService` catch-all:

| Action | microG 6.1.4 serves it as | Old patch behavior | Result |
|---|---|---|---|
| `gms.common.service.START` | literal only (DummyService) | renamed | bind resolved to nothing |
| `gms.accounts.ACCOUNT_SERVICE` | literal only (DummyService) | renamed | bind resolved to nothing |
| `gms.checkin.BIND_TO_SERVICE` | literal only (CheckinService) | renamed | bind resolved to nothing |
| `gms.identity.service.BIND` | literal only (DummyService) | renamed | bind resolved to nothing |
| `contextmanager…START` | literal only (DummyService) | renamed | bind resolved to nothing |
| `cast.firstparty/.remote_display/_mirroring` | literal only (DummyService) | renamed | bind resolved to nothing |
| `gms.auth.service.START` | **both** forms | renamed | worked |
| `gms.signin/auth.api.signin/credentials` | **both** forms | renamed | worked |

Gmail connects the CommonService telemetry client and the accounts service
**during startup** (verified in the pinned APK's DEX: the `IClientTelemetry…`
binder interfaces and the accounts-service action are referenced from the
startup path). With the renamed action the bind *failed outright* every time,
so Gmail's startup chain waited on a GMS connection that could never be
established — the loading screen hung until a watchdog killed the process.
That is why Maps/Photos-style apps launched fine while Gmail did not: they do
not block startup on those particular services.

**The fix** (in `shared/misc/gms/GmsCoreSupportPatch.kt`):
1. The eight literal-only actions above were **removed from the rename list**,
   so the apps now bind them against microG's literal `com.google.*` service
   declarations. They resolve to `DummyService`, which accepts the connection
   and fails gracefully per-call instead of hanging the connection retry loop.
2. Ten Gmail-specific bind actions observed in the pinned APK but absent from
   the upstream list (cloudmessaging/FCM, clearcut sampler, telemetry
   throttling/notification, dtdi, mdisync, notifications capping, ad-ID,
   auth-aang, account-transfer) were **added**. microG 6.1.4 does not serve
   these under either name yet, so the bind fails identically either way — the
   entries exist so any future microG build that implements them (under the
   renamed convention) starts working with no patch change.

Also verified against the microG source (no patch change needed): the
spoofed-identity meta-data works in our favor — microG reads
`app.revanced.android.gms.SPOOFED_PACKAGE_NAME/SIGNATURE` from the patched
app and treats it as a genuine Google package
(`38918a45…` is in microG's `GOOGLE_PRIMARY_KEYS`), and microG's account
authenticator uses account type `app.revanced`, which the engine's
`"com.google" → "app.revanced"` rewrite matches. FCM via
`cloudmessaging.service.START` is **not implemented** in microG 6.1.4; push
falls back to the legacy `c2dm.intent.REGISTER` path, which the patch already
renames and microG serves.

**To retest after updating the bundle:** patch Gmail again (same pinned
version), install, and go through the checklist above — in particular watch
for the first-run consent dialog from microG. If it still fails, the next
most likely causes are microG-side (account consent not granted, device
registration incomplete), not fingerprint issues.

### Root cause of "Continue -> problem with Google services" (onboarding) — PROVEN in microG source (2026-08-31)

After the startup fix above, patched Gmail loads, the welcome screen appears,
and tapping **Continue** produces a dialog the user reported as "there was a
problem with Google services". The dialog was identified from the APK: it is
`common_google_play_services_unknown_issue`, rendered as
**"Gmail is having trouble with Google Play services. Please try again."**

Why that exact message appears (all three facts verified in the pinned APK):

1. Gmail's GMS error mapper (`Labqd;->b(Context, I)` in classes5.dex) maps
   error codes 3, 4, 5, 9 and 20 to *named* string resources
   (`…sign_in_failed_text`, `…api_unavailable_text`,
   `…restricted_profile_text`, `…network_error_text`,
   `…invalid_account_text`). **None of those five resources exist in Gmail's
   resources.arsc** (verified: only 21 `common_google_play_services_*`
   names are compiled in, none of the five among them).
2. The fallback (`Labqd;->d`) formats `common_google_play_services_unknown_issue`
   with the app name -> "Gmail is having trouble with Google Play services."
   So **any of codes {3, 4, 5, 9, 20} shows this one dialog**.
3. The code in question is almost certainly **SIGN_IN_REQUIRED (4)**. microG's
   own sign-in services return exactly that when the Google account is not
   available to them:

   - `play-services-core/…/signin/SignInService.kt` (line 65):
     `signIn()` returns `ConnectionResult.SIGN_IN_REQUIRED` when the requested
     account is null **or not present in AccountManager** for the account type.
   - `play-services-core/…/auth/signin/AuthSignInService.kt` (line 89):
     `silentSignIn()` returns `Status(SIGN_IN_REQUIRED)` when no account is
     resolved **or when the app's OAuth use is not permitted**
     (`isPermitted == false` and `AuthPrefs.isTrustGooglePermitted == false`).

So the failing layer is **the microG account/consent state, not the patch**:
the patched app correctly reaches microG's sign-in service, and microG replies
"no usable account / consent not granted". The follow-up symptom (subsequent
launches hang on loading again) is the same SIGN_IN_REQUIRED result being
retried on the startup path after onboarding was left half-finished.

**The fix is on-device, no patch change required:**

1. **Add the Google account inside the microG app** (microG Services ->
   Google account -> add account). Android Settings -> Accounts must then
   show the account. This is the single most common cause of the error.
2. **microG Settings -> enable "Trust Google for app permissions"**
   (`pref_auth_trust_google`). With it off, apps whose consent was never
   asked/answered get SIGN_IN_REQUIRED from microG even with an account
   present; the setting's own summary warns exactly that.
3. Keep granting the legacy **"Google mail" permission**
   (`…googleapps.permission.GOOGLE_AUTH…` family) in the patched app's
   App Info -> Permissions before first launch — the field test showed the
   loading screen passes with it granted.
4. Then re-patch/reinstall is NOT needed if already on bundle >= 1.0.0 with
   the startup fix; just relaunch Gmail and tap Continue — microG should now
   resolve the account and answer SUCCESS.

If the account cannot be added inside microG at all (Google rejecting the
login), that is a device-registration/Checkin problem: enable *Google device
registration* in microG settings, reboot, retry the login. If it still
fails, capture the exact on-screen error from microG — that string, not the
Gmail dialog, is then the lead.

Note for bundle hygiene: releases 1.0.0 existed twice (a pre-fix build was
replaced by re-running CI with the same tag). Morphe re-downloads a bundle
when the release timestamp changes even if the version string does not, so
pull-to-refresh in Morphe (or delete + re-add the source) guarantees the
fixed build. Future releases get normal 1.0.x+ numbering.

### Field test 2 (2026-08-31): account present + "Trust Google" ON — still crashing. Why.

The second field test had the microG checklist items done — a Google account
existed and **Settings -> Trust Google for app permissions** was enabled
(verified in microG source: `AuthManager.java` lines 248/282 — with
`isTrustGooglePermitted` on, tokens issue for any app without a per-app
consent dialog, so microG-side config was correct). Gmail still crashed.
The explanation is not the patch and not microG — it is **which bundle the
device was patching with**:

1. **The test ran against the OLD, pre-fix bundle.** The fixed `.mpp` was
   only ever delivered via tmpfiles.org links that expire after ~60 minutes;
   the chat file transfer does not work for this user. The device therefore
   never received the build containing the startup bind fix.
2. **Local bundles in Morphe never update themselves.** This is documented
   in Morphe's own `docs/patch-sources.md`: *"Local sources never update on
   their own, you replace the file yourself when a new one comes out."* A
   locally imported `.mpp` stays byte-identical forever.
3. **Both builds are numbered 1.0.0**, so the version string on the source
   card cannot tell them apart (see bundle-hygiene note above).

#### How to tell which build is on the device (no PC needed)

Open Morphe -> Sources -> your GMS Patches card -> **Details** on the Gmail
patch (or start patching Gmail). The fixed build **pins the app version**:
it recommends exactly `2026.08.24.971409176` for Gmail. The old build has no
pin (`version: null` upstream) and recommends nothing specific. Pin visible
= fixed build; no pin = old build, replace it.

#### Recovery steps (do once, then forget)

1. **Best: add the source as Remote.** Sources -> **+** -> Remote tab ->
   `github.com/MrHuaweiFan/GMS-Patches`. Morphe fetches the latest release
   from GitHub directly and **auto-updates it forever** — no more manual
   zips. Delete the old local source afterwards to avoid the same app
   patched from two sources.
2. Or: download `patches-1.0.0.mpp` once from the release page
   (`https://github.com/MrHuaweiFan/GMS-Patches/releases` — permanent link,
   never expires), then Sources -> delete the old source -> **+** -> Local ->
   pick the fresh file.
3. Re-patch Gmail with the pinned version, grant the **Google mail**
   permission in App Info before the first launch, and only then open it.
4. microG side stays as-is: account added inside microG + Trust Google ON.

#### If it STILL crashes on the fixed bundle

Then and only then is the patch layer implicated again. The next diagnostic
step is a runtime capture: enable Developer options -> USB debugging is not
available without a PC, so instead note (a) the exact screen the crash
happens on, (b) whether microG's own account screen shows the account, and
(c) whether other pinned apps (Drive, Docs) sign in. Those three answers
triage the remaining candidates (uncovered early-return fingerprint vs.
microG auth config) without a logcat.

### Field test 3 (2026-08-31): full analysis — no patch bug found; recovery procedure v2

Third field report: without the "Google mail" permission Gmail **crashes**;
with it granted, it **never passes the loading screen and lags the device**.
Everything relevant has now been verified in source, and the conclusions are:

1. **The crash without the permission is expected, not a bug.** The legacy
   `…googleapps.permission.GOOGLE_AUTH…` family guards the AccountManager
   token call on Gmail's startup path; missing it raises a SecurityException
   the app does not catch. Granting it before first launch is correct and
   required — keep doing that.
2. **The auth chain was verified end-to-end in microG source — it is
   correct.** `PackageUtils.getAndCheckPackage` ends with
   `PackageSpoofUtils.spoofPackageName(...)` and `firstSignatureDigest` with
   `spoofStringSignature(...)`, both reading exactly the
   `app.revanced.android.gms.SPOOFED_PACKAGE_NAME` /
   `…SPOOFED_PACKAGE_SIGNATURE` meta-data this patch injects (values:
   `com.google.android.gm` + `38918a45…`, the real Google certificate).
   Google's servers receive a byte-perfect impersonation of genuine Gmail.
   With an account added in microG and "Trust Google for app permissions"
   enabled, tokens should issue without any consent dialog
   (`AuthManager.java` 248/282).
3. **Infinite loading + device lag is the fingerprint of the pre-fix
   bundle.** Its renamed-but-unserved service binds fail, and the GMS client
   retries in a loop — that loop is the lag. On the fixed bundle these binds
   resolve instantly (DummyService / real services). A *second* cause with
   the same symptom: **Gmail's saved data still holds the half-finished
   onboarding** from the earlier failed Continue. That state is poisoned;
   only a full uninstall (or clear-data) resets it.

#### Recovery procedure v2 (definitive order — do all steps)

1. **microG self-check first.** Open microG -> Self-Check: make sure device
   registration is enabled and reports OK, and the account is listed under
   microG's Google account screen. Fix anything red before touching Gmail.
2. **Add the patches as a Remote source** (Sources -> + -> Remote ->
   `github.com/MrHuaweiFan/GMS-Patches`), then **delete the old local
   source**. Remote sources update themselves; local files never do.
3. **Verify the pin.** Start patching Gmail: the fixed bundle recommends
   exactly `2026.08.24.971409176`. No recommended version = old bundle.
4. **Uninstall the previously patched Gmail completely** (or App Info ->
   Storage -> Clear data). Do not skip this — the old half-finished
   onboarding state causes the infinite loading by itself.
5. **Patch, install, and BEFORE the first launch grant the "Google mail"
   permission** (App Info -> Permissions).
6. **Launch the patched Gmail — note the patched app has its own package
   name and its own icon.** If two Gmail icons exist, the older/original one
   can never work under microG; open the new one.
7. First load can legitimately take one to two minutes (flag sync, account
   setup). If the spinner is still going after ~5 minutes with the device
   warm, stop and report — that means a bind is still failing and that is
   actionable information.

If after all seven steps Gmail still hangs, report: (a) which of the two
Gmail icons, (b) how long the loading lasts, (c) microG Self-Check state,
(d) whether Drive (pinned `2.26.347.3.all.alldpi`) gets past its own
loading screen. That quartet isolates the last remaining variables.

### Field test 4 (2026-08-31): THE root cause — renamed bind actions vs an all-literal microG. FIXED.

Field report: patched Gmail (with all permissions granted) reaches
**"Gmail is having trouble with Google Play services. Please try again."**,
on a device running **ReVanced GmsCore** (package `app.revanced.android.gms`),
where patched YouTube and Google Photos appear to work.

#### The evidence (decoded from the released ReVanced GmsCore APK)

The released ReVanced GmsCore v0.3.13.3.250932 APK was downloaded and its
manifest decoded:

| What | Value |
| :--- | :--- |
| Intent actions served | **251, ALL literal** (`com.google.android.gms.*`) |
| Intent actions served renamed (`app.revanced.*`) | **0** |
| `auth.service.START` | literal -> **real** `AuthProxyService` |
| `auth.api.signin.service.START` | literal -> **real** `AuthSignInService` |
| `signin.service.START` | literal -> **real** `SignInService` |
| `auth.be.appcert.AppCertService` | literal -> real `AppCertService` |
| `auth.login.LOGIN` (activity) | literal -> real `LoginActivity` |
| Providers' authorities | renamed (`app.revanced.android.gms.*`) |
| Declared permissions | renamed (`app.revanced.android.googleapps.permission.GOOGLE_AUTH.mail` etc.) |
| Authenticator account type | `app.revanced` |

#### Why patched Gmail broke

A GMS bind is `Intent(action).setPackage("app.revanced.android.gms")`. The
package rename is mandatory (that part was always right), but this engine
ALSO renamed the action strings themselves (223 of them:
`com.google.android.gms.auth.service.START` ->
`app.revanced.android.gms.auth.service.START`). Since the user's microG
serves zero renamed actions, **every renamed bind resolved to nothing** —
including all the auth/sign-in services Gmail requires at startup. The GMS
client surfaces the failure as the generic unknown-issue dialog.

Why YouTube/Photos seemed fine: they tolerate a missing GMS connection
(YouTube plays anonymously; Photos opens locally). Gmail is the first app
that hard-requires account sign-in during startup, so it is the first to
expose the broken binds. *(Refined in Field test 5 below: YouTube does more
than tolerate it — its login never touches the broken binds at all.)*

Why the earlier "v1.0.0 startup fix" looked right: it was tuned to the
Morphe **MicroG-RE** build, whose manifest serves the auth family renamed
(via `${basePackageName}` intent-filters) — the mirror image of ReVanced
GmsCore. The two builds need opposite action strategies.

#### The fix (this release, v1.0.1+)

`Constants.ACTIONS` is now **empty**: bind actions stay literal, so binds
hit the real literal services. Everything else is verified to stay renamed
and correct: vendor package (`app.revanced.android.gms`), account type
(`app.revanced`), the `GOOGLE_AUTH.*` permissions (declared renamed by this
microG build — the "Google mail" grant mechanism), content provider
authorities (served renamed), and the package/signature spoofing meta-data
(ReVanced's `AuthManager.requestAuth` applies `spoofPackageName` +
`spoofStringSignature` from the calling app's meta-data, so Google receives
a genuine Gmail identity: `com.google.android.gm` + `38918a45…`).

Known limitation: push (GCM/c2dm) registration is not functional on this
build (the app's c2dm registration intents have no matching receiver in
ReVanced GmsCore); Gmail falls back to its own periodic poll sync. Not a
startup blocker.

#### Compatibility matrix

| microG build | Use bundle | Why |
| :--- | :--- | :--- |
| ReVanced GmsCore (any current release) | **v1.0.1+** | serves literal actions only |
| Morphe MicroG-RE 6.1.4 (morphe.software) | v1.0.0 | serves auth family renamed only |

#### On-device steps (unchanged from recovery v2)

microG account added + permissions granted -> re-patch Gmail with the
pinned version (`2026.08.24.971409176`) -> grant "Google mail" before first
launch -> open the patched Gmail. First sign-in may take a minute.

### Field test 5 (2026-08-31): the confirming experiment — two roads to a token

Field report (all of it WITHOUT the v1.0.1 fix, i.e. on released v1.0.0 +
ReVanced microG):

- Upstream Morphe-patched **YouTube logs in and works on BOTH microGs**
  (MicroG-RE and ReVanced).
- **Google Maps patched with our v1.0.0 bundle logs in** on ReVanced microG
  and mostly works — only route-finding fails.
- Gmail, on the same device / same bundle / same microG: still
  "Gmail is having trouble with Google Play services".

That looks contradictory (v1.0.0 renames the same actions in every app — if
renamed binds were fatal to login, Maps would fail too), and resolving it
produced the definitive mechanism, now proven from both the app DEX and the
microG manifests:

**Two roads lead from an app to a Google token.**

| Road | Path | Sensitive to action renames? | Works on |
| :--- | :--- | :--- | :--- |
| **A. Authenticator** | app -> *system* `AccountManager` (`getAccountsByType` / `getAuthToken` / `addAccount`) -> the **system** binds microG's `GoogleLoginService` via the `android.accounts.AccountAuthenticator` action | **No** — `android.accounts.*` is a system action; no patch engine renames it | **any microG** |
| **B. GMS service binds** | app binds microG services directly: `auth.service.START` (AuthProxyService), `signin.service.START`, `auth.api.signin.service.START`, the `GOOGLE_SIGN_IN` activity, credentials, appcert… | **Yes** — these exact strings are what v1.0.0 renamed to `app.revanced.*` | only a microG that serves the renamed form (MicroG-RE) or the literal form (ReVanced) |

**Proof from the disassembled Gmail DEX** (all 9 dex files scanned):

- Road A is present: 33 call sites (`getAccountsByType`, `getAuthToken`,
  `blockingGetAuthToken`, `addAccount`) — this is why the account is always
  *visible* in Gmail's picker on every microG.
- Road B is present and mandatory for sign-in:
  `Label;->d()` returns `auth.service.START` (AuthProxyService client),
  `Labeu;->d()` returns `auth.api.signin.service.START` and
  `Labeu;->j()` / `SignInHubActivity` launch the `GOOGLE_SIGN_IN` activity,
  `Lacma;->d()` returns `signin.service.START`,
  `Labdl;->d()` returns `auth.account.data.service.START`.
  These are the play-services client factories — Gmail's sign-in
  (`SignInHubActivity` is Google's own sign-in hub) cannot proceed without
  them resolving.

**Reading the field data with the two roads:**

- **YouTube logs in everywhere** because its login walks Road A only. The
  upstream Morphe bundle renames the same actions — YouTube simply never
  binds them in order to sign in. (This is also why ReVanced's own YouTube
  patches never had to think about action names.)
- **Maps logs in** the same way (Road A identity). "Can't find any route"
  is Road-B / server-API territory degrading — a successful login is not
  the same as full function, and routing is a known microG ecosystem
  limitation regardless.
- **Gmail is the only app whose startup hard-requires Road B.** On
  ReVanced microG + v1.0.0 (renamed actions, microG serves literal only)
  every Road-B bind is dead -> the generic unknown-issue dialog.
- Corroborating: on MicroG-RE — which serves the **renamed** auth family
  with real services — the same v1.0.0 bundle got Gmail **past the loading
  screen** (field test 2). Road B was alive there; the remaining error was
  account/consent state, since fixed.

**The v1.0.1 fix (`ACTIONS = empty`) is confirmed correct, not just
plausible.** All bind action strings stay literal -> Road B binds resolve
against ReVanced GmsCore's real literal services (re-verified 2026-08-31
from the released APK: every served action is literal, zero renamed;
auth / signin / auth.api.signin / credentials / appcert / GOOGLE_SIGN_IN /
LOGIN are all real services). Road A is untouched by the change. No app can
regress: literal actions are exactly what the app would bind with
ReVanced-native patching, which is the proven-working style on this microG.

Maps on v1.0.1: can only improve — nothing that works today goes through a
renamed bind on ReVanced microG (there is nothing to hit), while Road-B
features (potentially including routing) gain live services.

### Field test 6 (2026-09-01): v1.0.4 still broken — the patch bundle itself was poisoning every app. FIXED in v1.0.5.

Field report (bug report 21:48, on patches v1.0.4):

- **Sheets crashes on every single launch** — the report contains the same
  FATAL stack four times:
  `ExceptionInInitializerError at SavedViewportSerializer.<init>`
  -> `Caused by ClassCastException: com.google.gson.internal.bind.JavaTimeTypeAdapters cannot be cast to com.google.gson.internal.bind.bi`
  -> `at com.google.gson.e.<clinit>`.
- Drive still shows neither the trash action nor the "Remove file?" dialog
  on shared files, and logs `OpenUrlAliasManager: Invalid package
  configuration` / `CrossAppStateChangedEve: Caller package not authorized`.
- Maps: the round account avatar button (account switcher / settings entry)
  is entirely absent — not an empty circle, the button does not exist.

**The bug was in the patch bundle, not in the apps or microG.**
`extensions/shared-youtube/library/build.gradle.kts` declared
`implementation(libs.gson)`, and the Morphe patcher dexes the `implementation`
dependencies of an extension library into the `.mpe` that gets merged into
EVERY patched app. No extension source file imports gson — it was dead weight
that shipped the entire un-obfuscated gson 2.14.0 into every patched APK.

Why that crashes Google apps specifically: their own R8-minified gson runs a
static-initializer probe for exactly that class name
(`Class.forName("com.google.gson.internal.bind.JavaTimeTypeAdapters")` +
`newInstance()` + cast to the app's *renamed* factory interface — verified in
the Sheets DEX at `bl.a()` and in Drive at `Laeyx;->a()`, and the same probe
string exists in Docs, Slides and Maps). In a stock app the `Class.forName`
fails with `ClassNotFoundException`, which the probe catches, and java.time
adapters are simply skipped. With the injected copy present the probe
SUCCEEDS, the cast to the renamed interface then throws
`ClassCastException` — which is not a `ReflectiveOperationException`, so it
escapes the catch block, kills the gson class initializer
(`ExceptionInInitializerError`) and everything that touches gson afterwards
crashes. For Sheets that happens inside `RitzActivity.onCreate` dependency
initialization, on every launch.

The same poison explains the other two reports even without a hard crash:
Drive's shared-file action bar is server-driven and client-parsed (the
"Remove file? … will be removed from view. Collaborators will still have
access." dialog string exists in neither the app's resources nor its DEX),
and Maps' account/avatar pipeline is JSON-driven — both run through the
poisoned gson. The Drive/Docs package-validation errors, on the other hand,
are the stale self-package strings that `rewriteSelfPackageNameStrings`
(v1.0.4) already addresses.

**The v1.0.5 fix is one line**: remove `implementation(libs.gson)` from the
extension module. `patches/build.gradle.kts` keeps gson `compileOnly` +
`patchListGeneratorClasspath` (patcher-side only — that part was already
correct and is untouched). Nothing else in the repo puts gson on a runtime
classpath that gets dexed into apps: `extensions/shared/library` has
`compileOnly(libs.annotation)` only.

Retest procedure for v1.0.5 (all patched apps must be re-patched — existing
installs still carry the injected classes):

1. Morphe: refresh the remote patch source, confirm the loaded bundle
   reports v1.0.5, re-patch Sheets, Drive, Docs, Slides, Maps (and Gmail).
2. Sheets first — it had the deterministic crash. Expected: opens normally,
   repeatedly (the crash was 100% reproducible, so this is a clean signal).
3. Drive: open a SHARED file (one you don't own), tap the three-dot menu —
   the trash/"Remove file" entry with the collaborators note should now
   appear, since the server-driven action bar can be parsed again.
4. Maps: open the main screen, wait ~30 seconds (account profile fetch),
   check the round avatar button. If Sheets+Drive are fixed but the avatar
   is STILL missing, the remaining suspect is the account pipeline itself
   (the AANG auth client microG does not implement) — capture a bug report
   immediately after those 30 seconds so the logcat window still contains
   the account code, and attach it for analysis.
## Provenance

The APKs used for verification were the current release-channel base APKs
from APKCombo (apkcombo.com), fetched and analyzed automatically on
2026-08-30: Gmail 2026.08.24.971409176.Release, Drive 2.26.347.3.all.alldpi,
Maps 26.34.04.965633971, Chrome 152.0.7977.64, Docs 1.26.341.02.90,
Sheets 1.26.341.01.90, Slides 1.26.341.01.90. All seven are signed with APK
Signature Scheme v2 by a single Google certificate (two distinct keys as
noted above). Google distributes these apps as split bundles; the analysis
used the universal/base APK, which is the part a patcher consumes and the
only part that contains the manifest and signing block. When you patch a
device-pulled copy, verify it is the same release channel and a version at
or near the ones listed -- Google apps change their obfuscated class names
(`Lncu;`, `Lv81;`, etc.) with essentially every release.

## License

De-Vanced (and therefore this fork) is GPL-3.0. See `NOTICE` for the
attribution/naming rules if you publish this — the README's "Getting
development started" section (still present in this copy) has the
rebranding checklist (project name, `build.gradle.kts` `about {}` block,
README) if you intend to distribute this rather than just build locally.
