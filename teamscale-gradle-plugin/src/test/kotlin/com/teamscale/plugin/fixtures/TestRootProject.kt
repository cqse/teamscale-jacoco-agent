package com.teamscale.plugin.fixtures

import java.io.File

class TestRootProject(projectDir: File) : TestProject(projectDir) {

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
		settingsFile.writeText("""
dependencyResolutionManagement {
    repositories {
        mavenCentral()
		mavenLocal()
    }
}
		""".trimIndent())
		buildFile.appendText(
			"""
buildscript {
	repositories {
		mavenCentral()
		mavenLocal()
	}
}

plugins {
	id 'java'
	id 'jacoco'
	id 'com.teamscale'
}

teamscale {
	commit {
		revision = "abcd1337"
	}
	repository="myRepoId"
	report {
		testwiseCoverage {
			partition = 'Unit Tests'
		}
	}
}

task integrationTest(type: com.teamscale.TestImpacted) {
	useJUnitPlatform {
		includeTags 'integration'
	}

	jacoco.includes = [ 'com.example.project.*' ]
	teamscale.report.partition = 'Integration Tests'
}

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

	fun withServerConfig(user: String = TeamscaleConstants.USER, accessToken: String = TeamscaleConstants.ACCESS_TOKEN, port: Int = TeamscaleConstants.PORT) {
		buildFile.appendText("""

teamscale {
	server {
		url = 'http://127.0.0.1:${port}'
		userName = '${user}'
		userAccessToken = '${accessToken}'
		project = 'test'
	}
}
		""".trimIndent())
	}

	fun withBranchAndTimestamp() {
		buildFile.appendText("""
			
teamscale {
	commit {
		timestamp = 1544512967526L
		branchName = "master"
	}
}
		""".trimIndent())
	}

	fun excludeFailingTests() {
		buildFile.appendText("""

tasks.withType(Test).configureEach {
	exclude '**/FailingRepeatedTest*'
}
		""".trimIndent())
	}

	fun defineLegacyUnitTestTask(jacocoIncludes: String = "com.example.project.*") {
		buildFile.appendText("""
			
task unitTest(type: com.teamscale.TestImpacted) {
	useJUnitPlatform {
		excludeTags 'integration'
	}
	jacoco.includes = [ '${jacocoIncludes}' ]
	testLogging {
		// events "started", "skipped", "failed"
		exceptionFormat "short"
		afterSuite { desc, result ->
			if (!desc.parent) { // will match the outermost suite
				def output = "Results: ${'$'}{result.resultType} (${'$'}{result.testCount} tests, ${'$'}{result.successfulTestCount} successes, ${'$'}{result.failedTestCount} failures, ${'$'}{result.skippedTestCount} skipped)"
				def startItem = '|  ', endItem = '  |'
				def repeatLength = startItem.length() + output.length() + endItem.length()
				println('\n' + ('-' * repeatLength) + '\n' + startItem + output + endItem + '\n' + ('-' * repeatLength))
			}
		}
	}
}
		""".trimIndent())
	}
}
