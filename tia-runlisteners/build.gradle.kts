plugins {
    `java-library`
    com.teamscale.`java-convention`
    com.teamscale.coverage
    com.teamscale.publish
}

publishAs {
    readableName.set("Teamscale TIA JUnit Run Listeners")
    description.set("JUnit 4 RunListener and JUnit 5 TestExecutionListener to record testwise coverage via the tia-client")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.teamscale.tia.CommandLineInterface"
    }
}

dependencies {
    implementation(project(":tia-client"))
    api(libs.junit4)
    api(libs.junit.platform.launcher)
}
