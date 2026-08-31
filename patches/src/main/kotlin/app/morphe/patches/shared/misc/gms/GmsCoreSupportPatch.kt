/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/shared/misc/gms/GmsCoreSupportPatch.kt
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.ResourcePatchBuilder
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.packagename.changePackageNamePatch
import app.morphe.patches.all.misc.packagename.setOrGetFallbackPackageName
import app.morphe.patches.shared.misc.gms.Constants.ACTIONS
import app.morphe.patches.shared.misc.gms.Constants.AUTHORITIES
import app.morphe.patches.shared.misc.gms.Constants.PERMISSIONS
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.IntentPreference
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element
import org.w3c.dom.Node

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/GmsCoreSupportPatch;"

/**
 * Old vendor id for maximum backwards compatibility.
 */
internal const val GMS_CORE_VENDOR_GROUP_ID = "app.revanced"

/**
 * v1.0.5: cross-app identity renames for the Google Drive suite.
 *
 * The Drive suite is one logical app split across four packages (Drive + the three
 * editors). Every app in the family resolves its siblings and its own role at
 * runtime through DEX string constants:
 *   - PackageInfoHelper (Lktz;) derives the app role from
 *     cachedPackageName.startsWith("com.google.android.apps.docs.editors");
 *   - Drive's family registry (Lqfg;) maps Gmail / Drive / each editor package
 *     to hand-off targets;
 *   - URI security checks (Lsam;, Lcom/google/android/libraries/security/content/c;,
 *     PromptBarFragment) compare Uri.getAuthority() against the Drive app's
 *     "com.google.android.apps.docs.storage*" authorities, which the manifest
 *     rename turns into "app.morphe.android.apps.docs.storage*";
 *   - Drive's provider-package check (Lpih;) accepts providers whose package
 *     starts with "com.google.android.apps.docs.".
 *
 * This map is passed UNCHANGED to all four suite patches and applied as EXACT
 * whole-string matches only (see [DRIVE_SUITE_CONTENT_URI_RENAMES] for the
 * content:// prefix rules). Keys are never dotted children, so
 * fully-qualified class names (which survive the package rename because the
 * patcher never renames classes) can not be mangled. Every key/value pair below
 * was cross-checked against the full DEX string census of the four pinned APKs
 * (2026-09-01, Docs 1.26.341.02.90 / Sheets 1.26.341.01.90 / Slides 1.26.341.01.90 /
 * Drive 2.26.347.3): 0 keys collide with defined classes.
 *
 * Requires: the suite apps keep their DEFAULT Morphe target package names
 * (app.morphe.*) -- a custom package-name patch option on any suite app
 * desynchronizes the family again.
 */
internal val DRIVE_SUITE_CROSS_APP_RENAMES: Map<String, String> = mapOf(
    // Drive itself (bare package) + provider-package prefix check + the
    // "am I an editor" family prefix checked by every suite app.
    "com.google.android.apps.docs" to "app.morphe.android.apps.docs",
    "com.google.android.apps.docs." to "app.morphe.android.apps.docs.",
    "com.google.android.apps.docs.editors" to "app.morphe.android.apps.docs.editors",
    // Sibling editor packages (Drive's hand-off registry, editor cross-launch).
    "com.google.android.apps.docs.editors.docs" to "app.morphe.android.apps.docs.editors.docs",
    "com.google.android.apps.docs.editors.sheets" to "app.morphe.android.apps.docs.editors.sheets",
    "com.google.android.apps.docs.editors.slides" to "app.morphe.android.apps.docs.editors.slides",
    // Drive's provider authorities as BARE authority strings (URI security
    // equals() checks). The Drive manifest renames these; the content:// URI
    // prefix form lives in DRIVE_SUITE_CONTENT_URI_RENAMES below.
    "com.google.android.apps.docs.storage" to "app.morphe.android.apps.docs.storage",
    "com.google.android.apps.docs.storage.documents" to "app.morphe.android.apps.docs.storage.documents",
    "com.google.android.apps.docs.storage.legacy" to "app.morphe.android.apps.docs.storage.legacy",
    // Gmail sibling (Drive's family registry maps it for hand-off).
    "com.google.android.gm" to "app.morphe.android.gm",
)

/**
 * v1.0.5: content:// URI authority-prefix renames for the Drive suite.
 *
 * ONLY the Drive app's provider authorities belong here — the manifest rename
 * on the Drive side turned "com.google.android.apps.docs.storage*" into
 * "app.morphe.android.apps.docs.storage*", and the SAF "open file" flow is
 * rooted at "content://com.google.android.apps.docs.storage.documents/root"
 * (decompiled class Luem;), so every suite app's copy of that URI must follow.
 *
 * Deliberately does NOT include package-name keys: the editors' own providers
 * ("com.google.android.apps.docs.editors.kix|trix[.statesyncer]" and friends)
 * are NOT renamed in their manifests (the authority does not start with the
 * editor's own package), so their URIs must stay original. A package-name key
 * here could prefix-match those URIs and break the editors' cross-app state
 * providers. Verified: the only family content:// URI in any of the four
 * pinned APKs is the storage.documents root above.
 */
internal val DRIVE_SUITE_CONTENT_URI_RENAMES: Map<String, String> = mapOf(
    "com.google.android.apps.docs.storage" to "app.morphe.android.apps.docs.storage",
    "com.google.android.apps.docs.storage.documents" to "app.morphe.android.apps.docs.storage.documents",
    "com.google.android.apps.docs.storage.legacy" to "app.morphe.android.apps.docs.storage.legacy",
)

/**
 * A patch that allows patched Google apps to run without root and under a different package name
 * by using GmsCore instead of Google Play Services.
 *
 * @param fromPackageName The package name of the original app.
 * @param toPackageName The package name to fall back to if no custom package name is specified in patch options.
 * @param primeMethodFingerprint The fingerprint of the "prime" method that needs to be patched.
 * @param earlyReturnFingerprints The fingerprints of methods that need to be returned early.
 * @param keepOriginalPackageScopedNames "Legacy identity" mode for account/sync-centric apps
 * whose own package name is woven through their code, resources AND manifest at once (Gmail):
 * keep everything that is scoped by the app's own original package name -- content provider
 * authorities ("com.google.android.gm.email.provider", "<pkg>.sapi", ...), the app's own
 * C2D_MESSAGE permission and any DEX string constants referencing them -- in their ORIGINAL
 * form, and instead only patch the pieces that the GmsCore ecosystem strictly requires
 * (package attribute, GMS vendor strings, c2dm permissions, sync adapter account types).
 *
 * Rationale (field-verified on Android 12 / EMUI, see v1.0.3): renaming authorities on ONE
 * side only creates a mixed naming state that crashes at runtime. Gmail references its own
 * authorities from DEX string constants, from res/xml (sync adapters) and possibly from
 * other non-code sources; rewriting just the DEX side (the v1.0.2 approach) made the
 * app's internal URI matchers expect the NEW authority while queries still arrived on the
 * OLD one ("Unknown uri: content://com.google.android.gm.email.provider/uiaccts"), and
 * lookups of the NEW authority failed wherever the manifest rename never applied
 * (multi-authority attributes are not prefix-renamed: "Failed to find provider info for
 * app.morphe.android.gm.email.provider"). Keeping the ORIGINAL names everywhere is the
 * only strategy that cannot miss a source: the renamed app simply owns the original
 * authority/permission names, exactly like the unpatched app does on a real device.
 *
 * The coexistence cost: the patched app can no longer be installed alongside the genuine
 * app (custom permission and authority name collisions). This is acceptable for the
 * microG use case, where the genuine app is precisely what is being replaced.
 * @param rewriteSelfPackageNameStrings Rewrite the app's own package name inside DEX string
 * constants -- the EXACT package match ("com.google.android.apps.docs"), self-targeted
 * "content://<pkg>..." URIs and the app's own "<pkg>.permission.C2D_MESSAGE" permission.
 * Required by the Drive suite apps (Drive, Docs, Sheets, Slides), which resolve their own
 * identity at runtime and compare it against the hardcoded original package name:
 *   - PackageInfoHelper caches Context.getPackageName() and gates features with
 *     pkg.equals("com.google.android.apps.docs") checks (field-verified in the
 *     2.26.347.3 DEX, class Llru;/Lbih;);
 *   - OpenUrlAliasManager.configureEntryPoints() self-matches PackageManager lookups
 *     against getPackageName() and logs "Invalid package configuration: %s" when the
 *     renamed package never matches (class Lljv;);
 *   - the editors apps validate their runtime package against a hardcoded allowlist and
 *     throw IllegalStateException "Invalid app package: <pkg>" (Slides, crash log
 *     2026-08-31 16:11:15.750).
 * After the manifest package rename none of these can ever match unless the DEX constants
 * follow, and the resulting null identity is what the startup coroutines crash on
 * (NullPointerException at a null-check idiom, 39 crashes across Drive/Docs/Slides in
 * bug report DBY-W09NM-2026-08-31-16-31-39).
 *
 * Deliberately NO dotted-children rewrite this time: many "<fromPkg>.xxx" strings are
 * fully-qualified CLASS names (e.g. "com.google.android.apps.docs.app.PaymentsActivity"),
 * which remain valid after the package rename (the patcher never renames classes) and must
 * not be mangled. The exact-match rules above cannot collide with class names.
 * @param crossAppPackageRenames v1.0.5: EXACT-match renames for OTHER family apps' package
 * names and bare authority strings, so a renamed app family can keep resolving its own
 * members after the manifest package rename. Added for the Drive suite (Drive, Docs,
 * Sheets, Slides), which is ONE logical app split across FOUR packages:
 *   - the editors validate incoming file URIs against the Drive app's provider
 *     authorities ("com.google.android.apps.docs.storage[.documents|.legacy]", decompiled
 *     classes Lsam; / Lcom/google/android/libraries/security/content/c; / PromptBarFragment)
 *     and hand files to each other by package name (Drive's Lqfg; family registry,
 *     AbstractEditorActivity open flow);
 *   - every suite app derives "am I an editor / which app am I" from
 *     PackageInfoHelper.cachedPackageName.startsWith("com.google.android.apps.docs.editors")
 *     (class Lktz;->a()) and from provider packageName prefix checks
 *     ("com.google.android.apps.docs." — Drive's Lpih;->b);
 *   - the "open file" flow launches the system SAF picker rooted at
 *     "content://com.google.android.apps.docs.storage.documents/root" (class Luem;).
 * After the rename, the Drive app's manifest serves the RENAMED authorities while these
 * DEX constants stay original, so every cross-app lookup misses — the editors fall back
 * to a degraded identity ("drive" instead of "docs"/"sheets"/"slides") and file opening
 * fails validation ("Unable to open file because it is not a valid document",
 * resource invalid_uri_error, shown by AbstractEditorActivity onCreate when
 * IntentHelper.getUri() returns null).
 *
 * Rules (all EXACT-match, never dotted children, so class-name strings are provably
 * untouched — verified against the full string census of the four pinned APKs,
 * scripts/analysis 2026-09-01): a DEX string exactly equal to a map key is replaced
 * with the map value.
 * @param crossAppContentUriRenames v1.0.5: "content://" authority-prefix renames for
 * provider authorities that the OTHER side of the family renamed in its manifest.
 * Deliberately SEPARATE from [crossAppPackageRenames] and restricted to whole
 * authorities (the Drive storage trio): a package-name key like
 * "com.google.android.apps.docs.editors" must never be used as a content:// prefix,
 * because the editors' own providers ("...editors.kix|trix..." authorities) are NOT
 * renamed in their manifests and their URIs must stay original.
 * The shared [DRIVE_SUITE_CROSS_APP_RENAMES] / [DRIVE_SUITE_CONTENT_URI_RENAMES] maps
 * cover all four apps; pass the SAME maps to every suite patch. Assumes default Morphe
 * target package names (do not override the package-name patch option for the suite
 * apps, or the cross references desync again).
 * @param mainActivityOnCreateFingerprint The fingerprint of the main activity onCreate method.
 * @param extensionPatch The patch responsible for the extension.
 * @param executeBlock The additional execution block of the patch.
 * @param block The additional block to build the patch.
 */
fun gmsCoreSupportPatch(
    fromPackageName: String,
    toPackageName: String,
    primeMethodFingerprint: Fingerprint? = null,
    earlyReturnFingerprints: Set<Fingerprint> = setOf(),
    keepOriginalPackageScopedNames: Boolean = false,
    rewriteSelfPackageNameStrings: Boolean = false,
    crossAppPackageRenames: Map<String, String> = emptyMap(),
    crossAppContentUriRenames: Map<String, String> = emptyMap(),
    mainActivityOnCreateFingerprint: Fingerprint,
    extensionPatch: Patch<*>,
    gmsCoreSupportResourcePatchFactory: () -> Patch<*>,
    executeBlock: BytecodePatchContext.() -> Unit = {},
    block: BytecodePatchBuilder.() -> Unit = {},
) = bytecodePatch(
    name = "GmsCore support",
    description = "Allows the app to work without root by using a different package name when patched " +
        "using a GmsCore instead of Google Play Services.",
) {

    dependsOn(
        changePackageNamePatch,
        gmsCoreSupportResourcePatchFactory(),
        extensionPatch,
    )

    execute {
        fun transformStringReferences(transform: (str: String) -> String?) = getAllClassesWithStrings().forEach {
            val mutableClass by lazy {
                mutableClassDefBy(it)
            }

            it.methods.forEach classLoop@{ method ->
                val implementation = method.implementation ?: return@classLoop

                val mutableMethod by lazy {
                    mutableClass.findMutableMethodOf(method)
                }

                implementation.instructions.forEachIndexed { index, instruction ->
                    val string = ((instruction as? Instruction21c)?.reference as? StringReference)?.string
                        ?: return@forEachIndexed

                    // Apply transformation.
                    val transformedString = transform(string) ?: return@forEachIndexed

                    mutableMethod.replaceInstruction(
                        index,
                        BuilderInstruction21c(
                            Opcode.CONST_STRING,
                            instruction.registerA,
                            ImmutableStringReference(transformedString),
                        ),
                    )
                }
            }
        }

        // region Collection of transformations that are applied to all strings.

        fun commonTransform(referencedString: String): String? = when (referencedString) {
            "com.google",
            "com.google.android.gms",
            in PERMISSIONS,
            in ACTIONS,
            in AUTHORITIES,
            -> referencedString.replace("com.google", GMS_CORE_VENDOR_GROUP_ID)

            // No vendor prefix for whatever reason...
            "subscribedfeeds" -> "$GMS_CORE_VENDOR_GROUP_ID.subscribedfeeds"
            else -> null
        }

        fun contentUrisTransform(str: String): String? {
            // only when content:// uri
            if (str.startsWith("content://")) {
                // check if matches any authority
                for (authority in AUTHORITIES) {
                    val uriPrefix = "content://$authority"
                    if (str.startsWith(uriPrefix)) {
                        return str.replace(
                            uriPrefix,
                            "content://${authority.replace("com.google", GMS_CORE_VENDOR_GROUP_ID)}",
                        )
                    }
                }

                // gms also has a 'subscribedfeeds' authority, check for that one too
                val subFeedsUriPrefix = "content://subscribedfeeds"
                if (str.startsWith(subFeedsUriPrefix)) {
                    return str.replace(subFeedsUriPrefix, "content://$GMS_CORE_VENDOR_GROUP_ID.subscribedfeeds")
                }
            }

            return null
        }

        fun packageNameTransform(
            fromPackageName: String,
            toPackageName: String,
            keepOriginalPackageScopedNames: Boolean,
            rewriteSelfPackageNameStrings: Boolean,
            crossAppPackageRenames: Map<String, String>,
            crossAppContentUriRenames: Map<String, String>,
        ): (String) -> String? = { string ->
            // v1.0.5: family cross-app identity. Exact-match rename of OTHER
            // suite members' package names / authority strings, plus content://
            // URI authority-prefix renames for authorities the other side's
            // manifest renamed (the Drive storage trio ONLY — never package
            // names, so the editors' own unrenamed ...editors.kix|trix provider
            // URIs can never be touched). All exact keys are whole strings,
            // never dotted children, so fully-qualified class names can never
            // collide (verified against the DEX string census of all four
            // pinned suite APKs).
            crossAppPackageRenames[string]
                ?: crossAppContentUriRenames.entries.firstOrNull { entry ->
                    string.startsWith("content://${entry.key}")
                }?.let { hit ->
                    "content://${hit.value}" + string.removePrefix("content://${hit.key}")
                }
                ?: when {
                // Upstream ReVanced behavior for apps whose only self-package DEX
                // references are these two provider authorities (Photos, YouTube, ...):
                // the manifest renames the authorities, so the DEX references follow.
                // Skipped entirely in legacy identity mode -- there the manifest KEEPS
                // the original authorities, so DEX references must stay untouched too.
                !keepOriginalPackageScopedNames &&
                        (string == "$fromPackageName.SuggestionProvider" ||
                                string == "$fromPackageName.fileprovider")
                        -> string.replace(fromPackageName, toPackageName)

                // v1.0.4: suite apps (Drive/Docs/Sheets/Slides) resolve their own
                // identity by package name at runtime (PackageInfoHelper feature
                // gating, OpenUrlAliasManager self-matching, "Invalid app package"
                // allowlist validation). The manifest rename changes what
                // Context.getPackageName() returns, so these exact-match DEX constants
                // must follow or the startup coroutines crash on the null identity.
                //
                // EXACT package match + self content:// URIs + the app's own C2D
                // permission ONLY. Never rewrite dotted children
                // "<fromPkg>.something": a large share of them are fully-qualified
                // class names ("...docs.app.PaymentsActivity"), which stay valid after
                // the package rename because the patcher never renames classes.
                rewriteSelfPackageNameStrings && string == fromPackageName ->
                    toPackageName

                rewriteSelfPackageNameStrings &&
                        string == "$fromPackageName.permission.C2D_MESSAGE" ->
                    "$toPackageName.permission.C2D_MESSAGE"

                rewriteSelfPackageNameStrings &&
                        string.startsWith("content://$fromPackageName") ->
                    "content://$toPackageName" +
                            string.removePrefix("content://$fromPackageName")

                else -> null
            }
        }

        fun transformPrimeMethod(packageName: String) {
            primeMethodFingerprint!!.method.apply {
                var register = 2

                val index = instructions.indexOfFirst {
                    if (it.getReference<StringReference>()?.string != fromPackageName) return@indexOfFirst false

                    register = (it as OneRegisterInstruction).registerA
                    return@indexOfFirst true
                }

                replaceInstruction(index, "const-string v$register, \"$packageName\"")
            }
        }

        // endregion

        val packageName = setOrGetFallbackPackageName(toPackageName)

        // Transform all strings using all provided transforms, first match wins.
        val transformations = arrayOf(
            ::commonTransform,
            ::contentUrisTransform,
            packageNameTransform(
                fromPackageName,
                packageName,
                keepOriginalPackageScopedNames,
                rewriteSelfPackageNameStrings,
                crossAppPackageRenames,
                crossAppContentUriRenames,
            ),
        )
        transformStringReferences transform@{ string ->
            transformations.forEach { transform ->
                transform(string)?.let { transformedString -> return@transform transformedString }
            }

            return@transform null
        }

        // Specific method that needs to be patched.
        primeMethodFingerprint?.let { transformPrimeMethod(packageName) }

        // Return these methods early to prevent the app from crashing.
        earlyReturnFingerprints.forEach {
            it.method.apply {
                if (returnType == "Z") {
                    returnEarly(false)
                } else {
                    returnEarly()
                }
            }
        }
        ServiceCheckFingerprint.method.returnEarly()

        // Google Play Utility is not present in all apps, so we need to check if it's present.
        if (GooglePlayUtilityFingerprint.methodOrNull != null) {
            GooglePlayUtilityFingerprint.method.returnEarly(0)
        }

        // Set original and patched package names for extension to use.
        OriginalPackageNameExtensionFingerprint.method.returnEarly(fromPackageName)

        // Verify GmsCore is installed and whitelisted for power optimizations and background usage.
        mainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "checkGmsCore(Landroid/app/Activity;)V"
        )

        // Change the vendor of GmsCore in the extension.
        GmsCoreSupportFingerprint.method.returnEarly(
            GMS_CORE_VENDOR_GROUP_ID
        )

        executeBlock()
    }

    block()
}

/**
 * A collection of permissions, intents and content provider authorities
 * that are present in GmsCore which need to be transformed.
 */
private object Constants {
    /**
     * All permissions.
     */
    val PERMISSIONS = setOf(
        "com.google.android.c2dm.permission.RECEIVE",
        "com.google.android.c2dm.permission.SEND",
        "com.google.android.gms.auth.api.phone.permission.SEND",
        "com.google.android.gms.permission.AD_ID",
        "com.google.android.gms.permission.AD_ID_NOTIFICATION",
        "com.google.android.gms.permission.CAR_FUEL",
        "com.google.android.gms.permission.CAR_INFORMATION",
        "com.google.android.gms.permission.CAR_MILEAGE",
        "com.google.android.gms.permission.CAR_SPEED",
        "com.google.android.gms.permission.CAR_VENDOR_EXTENSION",
        "com.google.android.googleapps.permission.GOOGLE_AUTH",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.cp",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.local",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.mail",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.writely",
        "com.google.android.gtalkservice.permission.GTALK_SERVICE",
        "com.google.android.providers.gsf.permission.READ_GSERVICES",
    )

    /**
     * All intent actions. INTENTIONALLY EMPTY since 2026-08-31: bind actions
     * are kept LITERAL ("com.google.android.gms.*") and are NOT renamed.
     *
     * Ground truth (decoded from the released ReVanced GmsCore APK
     * v0.3.13.3.250932-user, package app.revanced.android.gms - the microG
     * build the target audience actually runs):
     *
     *  - It serves 251 intent actions. ALL of them LITERAL
     *    ("com.google.android.gms.*", "com.google.android.c2dm.*", ...).
     *    It declares ZERO "app.revanced.*" action intent-filters.
     *  - The services Gmail's startup depends on are all served literally
     *    with REAL implementations:
     *      auth.service.START              -> org.microg.gms.auth.proxy.AuthProxyService
     *      auth.api.signin.service.START   -> org.microg.gms.auth.signin.AuthSignInService
     *      signin.service.START            -> org.microg.gms.signin.SignInService
     *      auth.api.credentials.service.START -> org.microg.gms.auth.credentials.CredentialsService
     *      auth.be.appcert.AppCertService  -> org.microg.gms.auth.appcert.AppCertService
     *      auth.login.LOGIN (activity)     -> org.microg.gms.auth.login.LoginActivity
     *      auth.GOOGLE_SIGN_IN (activity)  -> org.microg.gms.auth.signin.AuthSignInActivity
     *      checkin.BIND_TO_SERVICE         -> org.microg.gms.checkin.CheckinService
     *      phenotype.service.START, clearcut.service.START, people.service.START, ...
     *  - A bind is Intent(action).setPackage("app.revanced.android.gms"): the
     *    PACKAGE must be renamed (handled by the "com.google.android.gms"
     *    string transform), but the ACTION string must stay literal. Renaming
     *    the action made the bind resolve to nothing, which is what broke
     *    patched Gmail sign-in ("Gmail is having trouble with Google Play
     *    services") on ReVanced GmsCore while account-less apps (YouTube)
     *    appeared fine.
     *  - Actions served only by microG's DummyService (e.g.
     *    common.service.START, accounts.ACCOUNT_SERVICE) also resolve
     *    literally and fail gracefully (API_DISABLED) - no retry storm.
     *
     * Compatibility note: the Morphe MicroG-RE build (6.1.4, morphe.software)
     * serves the auth family RENAMED (via ${basePackageName} intent-filters)
     * plus a literal DummyService fallback. On THAT build a literal bind hits
     * DummyService and auth cannot work. This bundle therefore targets
     * ReVanced GmsCore; for MicroG-RE use the v1.0.0 bundle which renames
     * actions (see git history for the previous 223-entry list).
     */
    val ACTIONS = emptySet<String>()

    /**
     * All content provider authorities.
     */
    val AUTHORITIES = setOf(
        "com.google.android.gms.auth.accounts",
        "com.google.android.gms.chimera",
        "com.google.android.gms.fonts",
        "com.google.android.gms.phenotype",
        "com.google.android.gsf.gservices",
        "com.google.settings",
    )
}

/**
 * Abstract resource patch that allows Google apps to run without root and under a different package name
 * by using GmsCore instead of Google Play Services.
 *
 * @param fromPackageName The package name of the original app.
 * @param toPackageName The package name to fall back to if no custom package name is specified in patch options.
 * @param spoofedPackageSignature The signature of the package to spoof to.
 * @param keepOriginalPackageScopedNames Legacy identity mode: keep provider authorities and the app's
 * own C2D_MESSAGE permission under their original names, keep c2dm intent actions literal, and
 * retarget sync adapter account types to the GmsCore vendor account type. See the matching parameter
 * on [gmsCoreSupportPatch] for the full rationale.
 * @param executeBlock The additional execution block of the patch.
 * @param block The additional block to build the patch.
 */
fun gmsCoreSupportResourcePatch(
    fromPackageName: String,
    toPackageName: String,
    spoofedPackageSignature: String,
    screen: BasePreferenceScreen.Screen,
    keepOriginalPackageScopedNames: Boolean = false,
    executeBlock: ResourcePatchContext.() -> Unit = {},
    block: ResourcePatchBuilder.() -> Unit = {},
) = resourcePatch {
    dependsOn(
        changePackageNamePatch
    )

    execute {
        /**
         * Add metadata to manifest to support spoofing the package name and signature of GmsCore.
         */
        fun addSpoofingMetadata() {
            fun Node.adoptChild(
                tagName: String,
                block: Element.() -> Unit,
            ) {
                val child = ownerDocument.createElement(tagName)
                child.block()
                appendChild(child)
            }

            document("AndroidManifest.xml").use { document ->
                val applicationNode =
                    document
                        .getElementsByTagName("application")
                        .item(0)

                // Spoof package name and signature.
                applicationNode.adoptChild("meta-data") {
                    setAttribute("android:name", "$GMS_CORE_VENDOR_GROUP_ID.android.gms.SPOOFED_PACKAGE_NAME")
                    setAttribute("android:value", fromPackageName)
                }

                applicationNode.adoptChild("meta-data") {
                    setAttribute("android:name", "$GMS_CORE_VENDOR_GROUP_ID.android.gms.SPOOFED_PACKAGE_SIGNATURE")
                    setAttribute("android:value", spoofedPackageSignature)
                }

                // GmsCore presence detection in extension.
                applicationNode.adoptChild("meta-data") {
                    // TODO: The name of this metadata should be dynamic.
                    setAttribute("android:name", "app.revanced.MICROG_PACKAGE_NAME")
                    setAttribute("android:value", "$GMS_CORE_VENDOR_GROUP_ID.android.gms")
                }
            }
        }

        /**
         * Patch the manifest to support GmsCore.
         *
         * Default mode (Photos, Maps, ...): rename the package, all provider authorities,
         * the app's own C2D permissions and every c2dm string to the GmsCore vendor.
         *
         * Legacy identity mode (Gmail): rename ONLY what the GmsCore ecosystem strictly
         * requires, and keep every self-package-scoped name (provider authorities, the
         * app's own C2D_MESSAGE permission) in its original form so that DEX constants,
         * res/xml sync adapters and manifest declarations stay consistent:
         *  - provider authorities are NOT renamed (all code references stay original);
         *  - c2dm PERMISSIONS are renamed (GmsCore defines them under its vendor package)
         *    but c2dm INTENT ACTIONS stay literal, because GmsCore serves and broadcasts
         *    them literally (verified in McsService/PushRegisterService: every receiver
         *    intent filter must keep the literal "com.google.android.c2dm.intent.*");
         *  - the app's own C2D_MESSAGE permission stays original AND the attribute
         *    `android:permission="<pkg>.permission.C2D_MESSAGE"` is stripped from
         *    receivers: GmsCore cannot hold a signature permission declared by the
         *    patched app, so a protected receiver would silently drop every push.
         *    Without the attribute GmsCore delivers via its documented fallback
         *    (package-scoped ordered broadcast without permission);
         *  - DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION is still renamed, because
         *    androidx registers runtime receivers with a permission derived from the
         *    REAL (renamed) package name at runtime.
         */
        fun patchManifest() {
            val packageName = setOrGetFallbackPackageName(toPackageName)

            val transformations = if (keepOriginalPackageScopedNames) {
                mapOf(
                    "package=\"$fromPackageName" to "package=\"$packageName",
                    "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to
                        "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    // Only the permission family; "com.google.android.c2dm.intent.*"
                    // actions MUST stay literal for GmsCore delivery.
                    "com.google.android.c2dm.permission." to
                        "$GMS_CORE_VENDOR_GROUP_ID.android.c2dm.permission.",
                    "com.google.android.libraries.photos.api.mars" to
                        "$GMS_CORE_VENDOR_GROUP_ID.android.apps.photos.api.mars",
                    "</queries>" to "<package android:name=\"$GMS_CORE_VENDOR_GROUP_ID.android.gms\"/></queries>",
                )
            } else {
                mapOf(
                    "package=\"$fromPackageName" to "package=\"$packageName",
                    "android:authorities=\"$fromPackageName" to "android:authorities=\"$packageName",
                    "$fromPackageName.permission.C2D_MESSAGE" to "$packageName.permission.C2D_MESSAGE",
                    "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to
                        "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    "com.google.android.c2dm" to "$GMS_CORE_VENDOR_GROUP_ID.android.c2dm",
                    "com.google.android.libraries.photos.api.mars" to
                        "$GMS_CORE_VENDOR_GROUP_ID.android.apps.photos.api.mars",
                    "</queries>" to "<package android:name=\"$GMS_CORE_VENDOR_GROUP_ID.android.gms\"/></queries>",
                )
            }

            val manifest = get("AndroidManifest.xml")
            var manifestText = transformations.entries.fold(manifest.readText()) { acc, (from, to) ->
                acc.replace(
                    from,
                    to,
                )
            }

            if (keepOriginalPackageScopedNames) {
                // GmsCore cannot hold a signature permission declared by this app, so a
                // receiver guarded by the app's own C2D_MESSAGE permission would never
                // receive a push. Strip the guard; delivery is still restricted to this
                // app's receivers by the package-scoped broadcast fallback in GmsCore.
                manifestText = manifestText.replace(
                    " android:permission=\"$fromPackageName.permission.C2D_MESSAGE\"",
                    "",
                )
            }

            manifest.writeText(manifestText)
        }

        /**
         * Retarget sync adapter account types to the GmsCore vendor account type.
         *
         * Sync adapters live in res/xml and are NOT covered by any DEX or manifest
         * transform. Stock they declare `android:accountType="com.google"`; the DEX
         * account type constant is rewritten to the GmsCore vendor type ("app.revanced")
         * and accounts created by GmsCore carry that type, so without this rewrite
         * `ContentResolver.requestSync` never finds a matching SyncAdapterType and mail
         * sync silently never runs.
         */
        fun fixSyncAdapterAccountTypes() {
            if (!keepOriginalPackageScopedNames) return

            val resDirectory = this["res", false]
            val xmlDirectory = resDirectory.resolve("xml")
            if (!xmlDirectory.isDirectory) return

            xmlDirectory.listFiles()
                ?.filter { it.isFile && it.extension == "xml" }
                ?.forEach { file ->
                    val text = file.readText()
                    if (!text.contains("<sync-adapter")) return@forEach

                    file.writeText(
                        text.replace(
                            "android:accountType=\"com.google\"",
                            "android:accountType=\"$GMS_CORE_VENDOR_GROUP_ID\"",
                        )
                    )
                }
        }

        patchManifest()
        fixSyncAdapterAccountTypes()
        addSpoofingMetadata()

        screen.addPreferences(
            IntentPreference(
                "microg_settings",
                intent = IntentPreference.Intent("", "org.microg.gms.ui.SettingsActivity") {
                    "$GMS_CORE_VENDOR_GROUP_ID.android.gms"
                }
            )
        )

        executeBlock()
    }

    block()
}
