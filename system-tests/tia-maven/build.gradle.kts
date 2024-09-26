plugins {
    com.teamscale.`system-test-convention`
    com.teamscale.coverage
}

tasks.test {
    // install dependencies needed by the Maven test project
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}

