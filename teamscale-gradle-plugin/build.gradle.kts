plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    com.teamscale.`java-convention`
    com.teamscale.coverage
    com.teamscale.publish
    alias(libs.plugins.pluginPublish)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<JavaCompile> {
    options.release = 11
}

publishAs {
    readableName.set("Teamscale Gradle Plugin")
    description.set("A Gradle plugin that supports collecting Testwise Coverage and uploading reports to Teamscale.")
}

gradlePlugin {
    website.set("https://www.teamscale.com/")
    vcsUrl.set("https://github.com/cqse/teamscale-jacoco-agent")
    plugins {
        create("teamscalePlugin") {
            id = "com.teamscale"
            displayName = "Teamscale Gradle plugin"
            implementationClass = "com.teamscale.TeamscalePlugin"
            description = "Supports collecting Testwise Coverage and uploading reports to Teamscale."
            tags.set(listOf("teamscale", "coverage", "tga", "test", "gap", "junit", "upload"))
        }
        create("teamscaleAggregationPlugin") {
            id = "com.teamscale.aggregation"
            displayName = "Teamscale Gradle aggregation plugin"
            implementationClass = "com.teamscale.aggregation.TeamscaleAggregationPlugin"
            description = "Supports aggregating test execution and coverage data."
            tags.set(listOf("teamscale", "coverage", "tga", "test", "gap", "junit", "aggregation"))
        }
    }
}

dependencies {
    implementation(project(":teamscale-client"))
    implementation(project(":report-generator"))
    implementation(gradleApi())
    implementation(libs.jgit)
    implementation(libs.jackson.databind)
    testImplementation(libs.okio)
    testImplementation(project(":common-system-test"))
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("**/plugin.properties") {
        filter {
            it.replace("%PLUGIN_VERSION_TOKEN_REPLACED_DURING_BUILD%", version.toString())
        }
    }
}

tasks.test {
    dependsOn(":agent:publishToMavenLocal")
    dependsOn(":impacted-test-engine:publishToMavenLocal")
    dependsOn(":teamscale-client:publishToMavenLocal")
    dependsOn(":tia-client:publishToMavenLocal")
    dependsOn(":report-generator:publishToMavenLocal")
}
