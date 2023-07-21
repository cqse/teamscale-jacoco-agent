plugins {
    application
    com.teamscale.`java-convention`
    com.teamscale.coverage
    id("org.graalvm.buildtools.native") version "0.9.20"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.teamscale.profiler.installer.RootCommand"
    }
}

application {
    applicationName = "installer"
    mainClass.set("com.teamscale.profiler.installer.RootCommand")
}

dependencies {
    // we need this alpha version since it comes with support for GraalVM
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")
    implementation(libs.teamscaleLibCommons)
    implementation(libs.picocli.core)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.spark)
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("installer")
            fallback.set(false)
            // build an executable instead of a shared library
            sharedLibrary.set(false)
            // Required for reading files from the filesystem. See https://github.com/oracle/graal/issues/1294
            buildArgs("-H:+AddAllCharsets")
        }
    }
}
