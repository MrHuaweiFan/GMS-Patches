plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.morphe.extension.sharedyoutube.library"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(libs.annotation)

    // v1.0.5 DO NOT re-add runtime gson here (or in ANY extension module).
    // `implementation` deps of an extension library are dexed into the .mpe
    // and merged into EVERY patched app. No extension source file uses gson --
    // this line injected the entire un-obfuscated gson 2.14.0 (including
    // com.google.gson.internal.bind.JavaTimeTypeAdapters) into every patched
    // app. Google apps ship their own R8-minified gson whose static
    // initializer reflectively probes for exactly that class name
    // (Class.forName("com.google.gson.internal.bind.JavaTimeTypeAdapters") +
    // newInstance + check-cast to the app's renamed factory interface). In a
    // stock app the probe fails with ClassNotFoundException and is caught ->
    // java.time adapters are skipped. With the injected copy present the probe
    // SUCCEEDS, then the cast to the app's renamed interface throws
    // ClassCastException -- which is NOT a ReflectiveOperationException, so it
    // escapes the catch block, kills the Gson class initializer
    // (ExceptionInInitializerError) and crashes the app at startup.
    // Proven on Sheets 1.26.341.01.90 (bl.a -> com.google.gson.e.<clinit> ->
    // SavedViewportSerializer.<init> FATAL 4x in the 21:48 bug report) and on
    // Drive 2.26.347.3 (Laeyx;->a); Docs/Slides/Maps carry the same probe
    // string, so their Gson was equally poisoned (server-driven UI parsing,
    // account/profile JSON) even where it did not crash outright.
    // Patches-side gson (patches/build.gradle.kts) is compileOnly +
    // patchListGeneratorClasspath only and was already correct.
    implementation(project(":extensions:shared:library"))
}
