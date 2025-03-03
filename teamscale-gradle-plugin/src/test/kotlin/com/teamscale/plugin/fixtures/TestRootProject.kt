package com.teamscale.plugin.fixtures

import java.io.File

class TestRootProject(projectDir: File) : TestProject(projectDir) {

	fun withSingleProject() {
		File("test-project").copyRecursively(projectDir)
	}

	fun subproject(projectName: String, configureProject: TestProject.() -> Unit): TestProject {
		if (!settingsFile.readText().contains(projectName)) {
			settingsFile.appendText(
				"""
                include '$projectName'
            """.trimIndent()
			)
		}
		val sub = subproject(projectName)
		configureProject(sub)
		return sub
	}

	fun subproject(projectName: String): TestProject {
		return TestProject(dir(projectName))
	}

	fun defaultProjectSetup() {
		withDependencyResolutionManagement()
		withTeamscalePlugin()
		withJunitDependencies()
	}

	fun withDependencyResolutionManagement() {
		settingsFile.writeText(
			"""
	dependencyResolutionManagement {
		repositories {
			mavenLocal()
			mavenCentral()
		}
	}
			""".trimIndent()
		)
	}

	fun withServerConfig(
		user: String = TeamscaleConstants.USER,
		accessToken: String = TeamscaleConstants.ACCESS_TOKEN,
		port: Int = TeamscaleConstants.PORT
	) {
		buildFile.appendText(
			"""

teamscale {
	server {
		url = 'http://127.0.0.1:${port}'
		userName = '${user}'
		userAccessToken = '${accessToken}'
		project = 'test'
	}
}
		""".trimIndent()
		)
	}

	fun withBranchAndTimestamp() {
		buildFile.appendText(
			"""
			
teamscale {
	commit {
		timestamp = 1544512967526L
		branchName = "master"
	}
}
		""".trimIndent()
		)
	}

	fun excludeFailingTests() {
		buildFile.appendText(
			"""

tasks.withType(Test).configureEach {
	exclude '**/FailingRepeatedTest*'
}
		""".trimIndent()
		)
	}

	fun defineLegacyTestTasks(jacocoIncludes: String = "com.example.project.*") {
		buildFile.appendText(
			"""
			
task unitTest(type: com.teamscale.TestImpacted) {
	useJUnitPlatform {
		excludeTags 'integration'
	}
	jacoco.includes = [ '${jacocoIncludes}' ]
	testLogging {
		// events "started", "skipped", "failed"
		exceptionFormat = "short"
		afterSuite { desc, result ->
			if (!desc.parent) { // will match the outermost suite
				def output = "Results: ${'$'}{result.resultType} (${'$'}{result.testCount} tests, ${'$'}{result.successfulTestCount} successes, ${'$'}{result.failedTestCount} failures, ${'$'}{result.skippedTestCount} skipped)"
				def startItem = '|  ', endItem = '  |'
				def repeatLength = startItem.length() + output.length() + endItem.length()
				println('\n' + ('-' * repeatLength) + '\n' + startItem + output + endItem + '\n' + ('-' * repeatLength))
			}
		}
	}
	testClassesDirs = testing.suites.test.sources.output.classesDirs
	classpath = testing.suites.test.sources.runtimeClasspath
	partition = 'Unit Tests'
	finalizedBy('unitTestReport')
}

tasks.register('unitTestReport', com.teamscale.reporting.testwise.TestwiseCoverageReport) {
	from(tasks.unitTest)
}

task integrationTest(type: com.teamscale.TestImpacted) {
	useJUnitPlatform {
		includeTags 'integration'
	}

	jacoco.includes = [ 'com.example.project.*' ]
	testClassesDirs = testing.suites.test.sources.output.classesDirs
	classpath = testing.suites.test.sources.runtimeClasspath
	partition = 'Integration Tests'
}
		""".trimIndent()
		)
	}

	fun defineUploadTask() {
		buildFile.appendText(
			"""

tasks.register('unitTestReportUpload', com.teamscale.TeamscaleUpload) {
	partition = 'Unit Tests'
	from(tasks.unitTestReport)
}
		""".trimIndent()
		)
	}
}
