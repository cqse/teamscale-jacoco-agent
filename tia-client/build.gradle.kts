plugins {
    `java-library`
    com.teamscale.`kotlin-convention`
    com.teamscale.coverage
    com.teamscale.publish
}

publishAs {
    readableName.set("Teamscale TIA Client")
    description.set("Library and CLI to simplify integration of TIA into custom build frameworks")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.teamscale.tia.CommandLineInterface"
    }
}


dependencies {
    api(project(":teamscale-client"))
    api(project(":report-generator"))
    api(libs.retrofit.core)
    implementation(libs.retrofit.converter.jackson)

    testImplementation(libs.mockito.core)
    testImplementation(libs.okhttp.mockwebserver)
}
