plugins {
	application
	com.teamscale.`java-convention`
	com.teamscale.coverage
	id("org.beryx.jlink") version ("3.0.1")
}

tasks.jar {
	manifest {
		attributes(
			//"Automatic-Module-Name" to "com.teamscale.profiler.installer",
			"Main-Class" to "com.teamscale.profiler.installer.RootCommand",
		)
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

application {
	applicationName = "installer"
	mainClass = "com.teamscale.profiler.installer.RootCommand"
	mainModule = "com.teamscale.profiler.installer"
	applicationDefaultJvmArgs = listOf(
		// Ensure that no stack traces are lost.
		// See <https://stackoverflow.com/questions/2411487/nullpointerexception-in-java-with-no-stacktrace>
		"-XX:-OmitStackTraceInFastThrow",
	)
}

jlink {
	options = listOf(
		"--strip-debug",
		"--compress", "2",
		"--no-header-files",
		"--no-man-pages",
		"--dedup-legal-notices", "error-if-not-same-content"
	)
	launcher {
		name = "installer"
	}
}

dependencies {
	implementation(libs.okhttp.core)
	implementation(libs.commonsLang)
	implementation(libs.commonsIo)
	implementation(libs.picocli.core)
	annotationProcessor(libs.picocli.codegen)
	implementation("net.java.dev.jna:jna-platform:5.14.0")

	testImplementation(libs.spark)
}

tasks.processResources {
	filesMatching("**/app.properties") {
		filter {
			it.replace("%APP_VERSION_TOKEN_REPLACED_DURING_BUILD%", rootProject.ext["appVersion"].toString())
		}
	}
}
