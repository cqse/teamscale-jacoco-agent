plugins {
    com.teamscale.`system-test-convention`
    kotlin("jvm") version "1.9.0"
}

tasks.test {
    /** These ports must match what is configured in the SystemTest class. */
    teamscaleAgent(
        mapOf(
            "http-server-port" to "$agentPort",
            "teamscale-server-url" to "http://localhost:$teamscalePort",
            "teamscale-user" to "fake",
            "teamscale-access-token" to "fake",
            "teamscale-project" to "p",
            "teamscale-partition" to "part",
            "teamscale-commit" to "master:12345",
            "includes" to "*foo*",
        )
    )
}
