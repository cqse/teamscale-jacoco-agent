plugins {
    id("com.teamscale.system-test-convention")
    id("com.teamscale.coverage")
}

tasks.test {
    environment("AGENT_VERSION", version)
    environment("TEAMSCALE_PORT", 63700)
    // install dependencies needed by the Maven test project
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}

