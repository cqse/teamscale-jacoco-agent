import org.gradle.jvm.tasks.Jar

plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

dependencies {
	implementation("org.slf4j:slf4j-api:2.0.13")
	implementation("ch.qos.logback:logback-core:1.5.6")
	implementation("ch.qos.logback:logback-classic:1.5.6")
}

tasks.test {
	dependsOn(tasks.jar)
	systemProperty("agentJar", agentJar)
	doFirst {
		file("logTest/app.log").delete()
	}
}

tasks.withType<Jar> {
	archiveFileName = "app.jar"
	manifest {
		attributes["Main-Class"] = "jul.test.SystemUnderTest"
	}
	// create a fat jar
	from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
