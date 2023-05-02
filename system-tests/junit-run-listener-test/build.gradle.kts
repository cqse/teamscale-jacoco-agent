plugins {
    id("com.teamscale.system-test-convention")
    id("com.teamscale.coverage")
}

tasks.test {
    environment("AGENT_PATH", agentJar)
    environment("AGENT_VERSION", version)
    // install dependencies needed by the Maven test projects
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}
