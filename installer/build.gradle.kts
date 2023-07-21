plugins {
    application
    com.teamscale.`java-convention`
    com.teamscale.coverage
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.teamscale.profiler.installer.RootCommand"
    }
    // don't append a version number
    archiveFileName.set("installer.jar")
}

application {
    applicationName = "installer"
    mainClass.set("com.teamscale.profiler.installer.RootCommand")
}

dependencies {
    implementation(libs.okhttp.core)
    implementation(libs.teamscaleLibCommons)
    implementation(libs.picocli.core)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.spark)
}

