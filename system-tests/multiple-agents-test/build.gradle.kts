plugins {
    com.teamscale.`kotlin-convention`
    com.teamscale.`system-test-convention`
}

val jacocoAgent: Configuration by configurations.creating

dependencies {
    // This version should differ from the version we currently use for the Teamscale JaCoCo agent itself
    jacocoAgent("org.jacoco:org.jacoco.agent:0.8.10:runtime")
}

tasks.test {
    val otherJacocoAgent = jacocoAgent.singleFile
    jvmArgs("-javaagent:$otherJacocoAgent")

    val logFilePath = "logTest"

    doFirst {
        delete(logFilePath)
    }

    teamscaleAgent(mapOf("debug" to logFilePath))
}

