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
        }
    }

    // Need to stay at 1.12.0 because 1.13.0 upwards uses multi release jars which breaks the gradle plugin tests
    implementation("com.squareup.moshi:moshi:1.12.0")

    testImplementation(libs.jsonassert)
    testImplementation(libs.teamscaleLibCommons)
}

