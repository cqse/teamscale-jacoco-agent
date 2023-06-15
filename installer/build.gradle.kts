plugins {
    application
    com.teamscale.`java-convention`
    com.teamscale.coverage
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.teamscale.jacoco.agent.installer.Installer"
    }
}


dependencies {
    implementation(libs.okhttp.core)
    implementation(libs.teamscaleLibCommons)
}
