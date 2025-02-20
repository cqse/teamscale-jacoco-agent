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

	fun withSampleCode() {
		File("test-project").copyRecursively(projectDir)
	}
}
