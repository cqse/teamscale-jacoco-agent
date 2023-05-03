plugins {
	com.teamscale.`java-convention`
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