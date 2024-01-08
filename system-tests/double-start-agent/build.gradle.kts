plugins {
    com.teamscale.`system-test-convention`
}

tasks.test {
    val logFilePath = "logTest"

    doFirst {
        delete(logFilePath)
    }

    teamscaleAgent(
        mapOf(
            "debug" to logFilePath
        )
    )
    teamscaleAgent(
        mapOf(
            "debug" to logFilePath
        )
    )
}

