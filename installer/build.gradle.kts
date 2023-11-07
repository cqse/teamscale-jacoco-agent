plugins {
	application
	com.teamscale.`java-convention`
	com.teamscale.coverage
	id("org.graalvm.buildtools.native") version "0.9.5"
}

repositories {
	maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "com.teamscale.profiler.installer.RootCommand"
	}
}

application {
	applicationName = "installer"
	mainClass.set("com.teamscale.profiler.installer.RootCommand")
}

dependencies {
	// we need this older version since newer versions are Kotlin-implemented and don't play nice with GraalVM
	implementation("com.squareup.okhttp3:okhttp:3.14.2")
	implementation(libs.teamscaleLibCommons)
	implementation(libs.picocli.core)
	annotationProcessor(libs.picocli.codegen)
	implementation("org.jetbrains.nativecerts:jvm-native-trusted-roots:1.0.19")

	testImplementation(libs.spark)
}

tasks.processResources {
	filesMatching("**/app.properties") {
		filter {
			it.replace("%APP_VERSION_TOKEN_REPLACED_DURING_BUILD%", rootProject.ext["appVersion"].toString())
		}
	}
}

graalvmNative {
	binaries {
		named("main") {
			imageName.set("installer")
			fallback.set(false)
			// build an executable instead of a shared library
			sharedLibrary.set(false)
			buildArgs(
				// Required for reading files from the filesystem. See https://github.com/oracle/graal/issues/1294
				"-H:+AddAllCharsets",
				// Required for HTTP requests
				"--enable-http", "--enable-https",
				// Required to include the app.properties file
				"-H:IncludeResourceBundles=com.teamscale.profiler.installer.app"
			)
		}
	}
}
