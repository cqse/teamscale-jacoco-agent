import org.beryx.jlink.util.JdkUtil

plugins {
	application
	com.teamscale.`java-convention`
	com.teamscale.coverage
	com.teamscale.`system-test-convention`
	id("org.beryx.jlink") version ("3.1.1")
}

tasks.jar {
	manifest {
		attributes(
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

val ADOPTIUM_BINARY_REPOSITORY = "https://api.adoptium.net/v3/binary"
val RUNTIME_JDK_VERSION = "17.0.5+8"
jlink {
	options = listOf(
		"--compress", "2",
		"--no-header-files",
		"--no-man-pages",
		"--dedup-legal-notices", "error-if-not-same-content"
	)
	launcher {
		name = "installer"
	}

	targetPlatform("linux-x86_64") {
		setJdkHome(
			jdkDownload(
				"$ADOPTIUM_BINARY_REPOSITORY/version/jdk-${RUNTIME_JDK_VERSION}/linux/x64/jdk/hotspot/normal/eclipse",
				closureOf<JdkUtil.JdkDownloadOptions> {
					archiveExtension = "tar.gz"
				})
		)
	}
	targetPlatform("windows-x86_64") {
		setJdkHome(
			jdkDownload(
				"$ADOPTIUM_BINARY_REPOSITORY/version/jdk-${RUNTIME_JDK_VERSION}/windows/x64/jdk/hotspot/normal/eclipse",
				closureOf<JdkUtil.JdkDownloadOptions> {
					archiveExtension = "zip"
				})
		)
	}
}

dependencies {
	implementation(libs.okhttp.core)
	implementation(libs.commonsLang)
	implementation(libs.commonsIo)
	implementation(libs.picocli.core)
	annotationProcessor(libs.picocli.codegen)
	implementation(libs.jna.platform)

	testImplementation(libs.spark)
	testImplementation(project(":common-system-test"))
}

tasks.processResources {
	filesMatching("**/app.properties") {
		filter {
			it.replace("%APP_VERSION_TOKEN_REPLACED_DURING_BUILD%", version.toString())
		}
	}
}
