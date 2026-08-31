group = "app.morphe"

patches {
    about {
        name = "GMS Patches"
        description = "GmsCore (microG) support patches for Google apps, for use with Morphe."
        source = "https://github.com/MrHuaweiFan/GMS-Patches"
        author = "MrHuaweiFan"
        contact = "na"
        website = "https://github.com/MrHuaweiFan/GMS-Patches"
        license = "GNU General Public License v3.0"
    }
}

//dependencies {
//    compileOnly(libs.morphe.patcher)
//
//    // Used by JsonGenerator.
//    implementation(libs.gson)
//
//    // Required due to smali, or build fails. Can be removed once smali is bumped.
//    implementation(libs.guava)
//
//    // Android API stubs defined here.
//    compileOnly(project(":patches:stub"))
//}


// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
