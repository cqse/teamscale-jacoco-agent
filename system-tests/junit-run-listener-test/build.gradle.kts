plugins {
    com.teamscale.`system-test-convention`
    com.teamscale.coverage
}

tasks.test {
    environment("AGENT_PATH", agentJar)
    environment("AGENT_VERSION", version)
    environment("AGENT_PORT", agentPort)
    environment("TEAMSCALE_PORT", teamscalePort)
    // install dependencies needed by the Maven test projects
    dependsOn(rootProject.tasks["publishToMavenLocal"])
}
