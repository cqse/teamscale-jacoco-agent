plugins {
    com.teamscale.`system-test-convention`
}

tasks.test {
    /** These ports must match what is configured in the SystemTest class. */
    val fakeTeamscalePort = 63200
    val agentPort = 63201
    teamscaleAgent(
        mapOf(
            "http-server-port" to "$agentPort",
            "teamscale-server-url" to "http://localhost:$fakeTeamscalePort",
            "teamscale-user" to "fake",
            "teamscale-access-token" to "fake",
            "teamscale-project" to "p",
            "teamscale-partition" to "part",
            "teamscale-commit" to "master:12345",
            "includes" to "**",
            "excludes" to "*foo.*"
        )
    )
}
