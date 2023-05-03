plugins {
    com.teamscale.`system-test-convention`
}

tasks.test {
    teamscaleAgent(mapOf("debug" to "true"))
}
