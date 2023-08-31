plugins {
	com.teamscale.`system-test-convention`
}

tasks.register<JavaExec>("runWithoutGradleWorker") {
	dependsOn(":agent:shadowJar")
	mainClass.set("jul.test.SystemUnderTest")
	classpath = sourceSets["main"].runtimeClasspath
	systemProperty("java.util.logging.manager", "jul.test.CustomLogManager")
	teamscaleAgent(mapOf("mode" to "testwise", "tia-mode" to "http", "http-server-port" to "63900"))
}

tasks.test {
	dependsOn("runWithoutGradleWorker")
	val logFilePath = "logTest"
	teamscaleAgent(mapOf("debug" to logFilePath))
}
