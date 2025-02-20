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

import com.teamscale.TestImpacted

if (!project.hasProperty("withoutServerConfig")) {
	teamscale {
		server {
			url = 'http://127.0.0.1:64000'
			userName = 'build'
			userAccessToken = '82l1jtkIx6xG7DDG34FLsKhejcHz1cMu' // Not a real access token
			project = 'test'
		}
	}
}

teamscale {
	commit {
		if (!project.hasProperty("withoutTimestamp")) {
			timestamp = 1544512967526L
			branchName = "master"
		}
		revision = "abcd1337"
	}
	repository="myRepoId"
	report {
		testwiseCoverage {
			partition = 'Unit Tests'
		}
	}
}

task unitTest(type: TestImpacted) {
	useJUnitPlatform {
		excludeTags 'integration'
	}
	jacoco.includes = [ project.findProperty('jacocoIncludePattern') ?: 'com.example.project.*' ]
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
	if (project.hasProperty('excludeFailingTests')) {
		exclude '**/FailingRepeatedTest*'
	}
}

task integrationTest(type: TestImpacted) {
	useJUnitPlatform {
		includeTags 'integration'
	}

	jacoco.includes = [ 'com.example.project.*' ]
	teamscale.report.partition = 'Integration Tests'
}

repositories {
	mavenCentral()
	mavenLocal()
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
}
