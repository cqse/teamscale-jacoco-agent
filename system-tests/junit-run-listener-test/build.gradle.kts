plugins {
    id("com.teamscale.system-test-convention")
    id("com.teamscale.coverage")
}

tasks.test {
    environment("AGENT_PATH", agentJar)
    environment("AGENT_VERSION", version)
    environment("AGENT_PORT", 63400)
    environment("TEAMSCALE_PORT", 63401)
    // install dependencies needed by the Maven test projects
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}
