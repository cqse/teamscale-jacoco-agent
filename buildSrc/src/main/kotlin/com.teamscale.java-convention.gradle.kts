import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
}

group = "com.teamscale"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.assertj:assertj-core:3.24.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.3")
}