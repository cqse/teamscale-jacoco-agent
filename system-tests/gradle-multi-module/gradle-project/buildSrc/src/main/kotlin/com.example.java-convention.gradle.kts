plugins {
	java
	id("com.teamscale")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.9.1"))
	testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
	useJUnitPlatform()
}

teamscale {

	server {
		url = "http://localhost:${System.getenv("TEAMSCALE_PORT")}/"
		userName = "admin"
		userAccessToken = "mrllRGxXJrsyL0UID3gGcJnZuQT4EyWr"
		project = "submodules"
	}

	report {
		testwiseCoverage {
			partition.set("Unit Tests")
		}
	}
	commit {
		branchName = "branch1"
		timestamp = "123456"
	}

}

tasks.register("tiaTests", com.teamscale.TestImpacted::class.java) {
	useJUnitPlatform()
	configure<JacocoTaskExtension> {
		includes = listOf("org.example.*")
	}
}
