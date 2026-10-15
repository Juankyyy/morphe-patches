group = "app.juankyyy"

patches {
    // TODO: Update this section with your project details.
    about {
        name = "Juankyyy Patches"
        description = "Patches for apps I like"
        source = "https://github.com/Juankyyy/morphe-patches"
        author = "Juankyyy"
        contact = "https://github.com/Juankyyy/morphe-patches/issues"
        website = "https://github.com/Juankyyy"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)

    // Minimal smoke-test harness (see patches/src/test/kotlin/).
    // Applies patches to a real APK from the gitignored apks/ dir and asserts
    // on the emitted bytecode. Tests skip when the APK is absent, so CI stays green.
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

    test {
        useJUnitPlatform()
        // Smoke tests load the full APK and parse every emitted dex in memory.
        maxHeapSize = "2g"
    }
}
