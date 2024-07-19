import com.teamscale.TestImpacted

plugins {
    id("java")
    id("com.teamscale") version "31.0.0-cucumber-SNAPSHOT"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
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

tasks.register("tiaTests", TestImpacted::class) {
    // Tried to fix with the following, but it makes no difference:
    // systemProperty("cucumber.junit-platform.naming-strategy", "long")
    // testClassesDirs = files("/Users/pete/Projects/cucumber-gradle/build/classes/java/test")

    // Usual setup from here
    useJUnitPlatform()
    jacoco {
        include("*org.example.*")
    }
}
