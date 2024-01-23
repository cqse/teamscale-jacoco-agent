plugins {
    com.teamscale.`system-test-convention`
}

val jacocoAgent by configurations.creating

dependencies {
    // This version should differ from the version we currently use for the Teamscale JaCoCo agent itself
    jacocoAgent("org.jacoco:org.jacoco.agent:0.7.8:runtime")
}

tasks.test {
    val otherJacocoAgent = jacocoAgent.singleFile
    jvmArgs("-javaagent:$otherJacocoAgent")

    val logFilePath = "logTest"

    doFirst {
        delete(logFilePath)
    }

    teamscaleAgent(
        mapOf(
            "debug" to logFilePath
        )
    )
}

