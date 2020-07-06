import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
}

group = "com.teamscale"

repositories {
    mavenCentral()
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

dependencies {
    testImplementation("junit:junit:4.12")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("org.mockito:mockito-core:2.26.0")
    testImplementation("org.mockito:mockito-junit-jupiter:2.26.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.6.2")
}