plugins {
    com.teamscale.`system-test-convention`
    com.teamscale.coverage
}

tasks.test {
    environment("AGENT_VERSION", version)
    environment("TEAMSCALE_PORT", 63800)
    // install dependencies needed by the Maven test project
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}
