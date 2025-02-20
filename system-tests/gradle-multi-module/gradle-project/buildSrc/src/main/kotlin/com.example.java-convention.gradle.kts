import com.teamscale.TestImpacted

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

testing {
	suites {
		val test by getting(JvmTestSuite::class) {
			useJUnitJupiter()
		}

		register<JvmTestSuite>("integrationTest") {
			dependencies {
				implementation(project())
			}
		}
	}
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.12.0"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

teamscale {
	server {
		url = "http://localhost:${System.getenv("TEAMSCALE_PORT")}/"
		userName = "admin"
		userAccessToken = "mrllRGxXJrsyL0UID3gGcJnZuQT4EyWr"
		project = "submodules"
	}

	commit {
		branchName = "branch1"
		timestamp = "123456"
	}
}

val test by testing.suites.existing(JvmTestSuite::class)

val tiaTests by tasks.registering(TestImpacted::class) {
	useJUnitPlatform()
	partition = "Unit Tests"
	configure<JacocoTaskExtension> {
		includes = listOf("org.example.*")
	}
	testClassesDirs = files(test.map { it.sources.output.classesDirs })
	classpath = files(test.map { it.sources.runtimeClasspath })
}

val testwiseCoverageReportElements by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
	extendsFrom(configurations.getByName("implementation"))
	attributes {
		attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.VERIFICATION))
		attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("testwise-coverage"))
	}
	outgoing.artifact(tiaTests.map { it.reports.testwiseCoverage.outputLocation })
}
