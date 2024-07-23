import com.teamscale.TestImpacted

plugins {
    id("java")
    id("com.teamscale") version "34.0.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

teamscale {

    server {
        url = "http://localhost:8080/"
        userName = "admin"
        userAccessToken = "q4tu9vfAAjQZ1peCpPvQHSrLi5CeIcGY"
        project = "cucumber-gradle"
    }

    report {
        testwiseCoverage {
            partition.set("Cucumber Tests")
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.cucumber:cucumber-java:7.13.0")
    testImplementation("io.cucumber:cucumber-junit:7.13.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.13.0")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
}

tasks.register<com.teamscale.TestImpacted>("tiaTests") {
    useJUnitPlatform()
    configure<JacocoTaskExtension> {
        includes = listOf("*org.example.*")
    }
}
