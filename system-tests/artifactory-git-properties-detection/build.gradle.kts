plugins {
    id("com.teamscale.system-test-convention")
}

sourceSets {
    main {
        output.setResourcesDir(java.classesDirectory.get().asFile)
    }
}

tasks.test {
    val artifactoryPort = 65432
    val agentPort = 65434
    teamscaleAgent(
        mapOf(
            "http-server-port" to "$agentPort",
            "artifactory-url" to "http://localhost:$artifactoryPort",
            "artifactory-api-key" to "fake",
            "artifactory-partition" to "some-test"
        )
    )
}
