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

tasks.test {
    useJUnitPlatform {
        excludeEngines("teamscale-test-impacted")
    }
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

// Workaround until https://github.com/gradle/gradle/issues/15383 is fixed
val catalogs = extensions.getByType<VersionCatalogsExtension>()
val libs = catalogs.named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

dependencies {
    testImplementation(lib("junit-jupiter-api"))
    testImplementation(lib("assertj"))
    testImplementation(lib("mockito-core"))
    testImplementation(lib("mockito-junit"))

    testRuntimeOnly(lib("junit-platform-launcher"))
    testRuntimeOnly(lib("junit-jupiter-engine"))

    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1")
    }
}
