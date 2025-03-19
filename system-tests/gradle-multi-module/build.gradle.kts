plugins {
    com.teamscale.`kotlin-convention`
    com.teamscale.`system-test-convention`
    com.teamscale.coverage
}

tasks.test {
    // install dependencies needed by the Gradle test project
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}
