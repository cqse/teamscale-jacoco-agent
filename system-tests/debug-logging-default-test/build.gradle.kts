plugins {
    id("com.teamscale.system-test-convention")
}

tasks.test {
    teamscaleAgent(mapOf("debug" to "true"))
}
