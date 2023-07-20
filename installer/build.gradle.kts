plugins {
    application
    com.teamscale.`java-convention`
    com.teamscale.coverage
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.teamscale.profiler.installer.Installer"
    }
    // don't append a version number
    archiveFileName.set("installer.jar")
}

dependencies {
    implementation(libs.okhttp.core)
    implementation(libs.teamscaleLibCommons)

    testImplementation(libs.spark)
}
