plugins {
	com.teamscale.`java-convention`
	application
	com.teamscale.`agent-jar`
}

application {
	mainClass.set("com.example.Main")
}

version = "unspecified"

dependencies {
	testImplementation(libs.junit4)
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "com.example.Main"
	}
}

tasks.named<JavaExec>("run") {
	teamscaleAgent(
		mapOf(
			"config-file" to "jacocoagent.properties"
		)
	)
	dependsOn(":agent:shadowJar")
}
