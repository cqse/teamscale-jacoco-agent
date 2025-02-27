package com.teamscale.plugin.fixtures

import java.io.File

open class TestProject(val projectDir: File) {

	val settingsFile = file("settings.gradle")
	val buildDir = file("build")
	val buildFile = file("build.gradle")

	fun dir(path: String): File {
		val dir = if (path.startsWith("/")) {
			File(path)
		} else {
			File(projectDir, path)
		}
		dir.mkdirs()
		return dir
	}

	fun file(path: String): File {
		return if (path.contains("/")) {
			val parentPath = path.substringBeforeLast("/")
			val fileName = path.substringAfterLast("/")
			val parentDir = dir(parentPath)

			File(parentDir, fileName)
		} else {
			File(projectDir, path)
		}
	}


	fun withTeamscalePlugin(basePlugin: String = "java") {
		buildFile.appendText(
			"""
plugins {
	id '${basePlugin}'
	id 'com.teamscale'
}

teamscale {
	commit {
		revision = "abcd1337"
	}
	repository = "myRepoId"
}
	""".trimIndent()
		)
	}

	fun withJunitDependencies() {
		buildFile.appendText(
			"""
				
dependencies {
	// JUnit Jupiter
	testImplementation(platform("org.junit:junit-bom:5.12.0"))
	testImplementation("org.junit.jupiter:junit-jupiter")

	// If you also want to support JUnit 3 and JUnit 4 tests
	testImplementation("junit:junit:4.13.2")
	testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
				""".trimIndent()
		)
	}
}
