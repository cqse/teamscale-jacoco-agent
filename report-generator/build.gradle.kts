plugins {
    `java-library`
    com.teamscale.`java-convention`
    com.teamscale.coverage
    com.teamscale.publish
}

publishAs {
    readableName.set("Test Coverage Report Generator")
    description.set("Utilities for generating JaCoCo and Testwise Coverage reports")
}

dependencies {
    implementation(project(":teamscale-client"))

    implementation(libs.jacoco.core)
    implementation(libs.jacoco.report)
    implementation(libs.jacoco.agent) {
        artifact {
            classifier = "runtime"
            // Needs to be explicitly specified to end up in the Gradle Module metadata
            // https://github.com/gradle/gradle/issues/21526
            extension = "jar"
        }
    }

    implementation(libs.jackson.databind)

    testImplementation(libs.jsonassert)
    testImplementation(libs.teamscaleLibCommons)
}

