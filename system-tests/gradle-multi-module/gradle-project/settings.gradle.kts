pluginManagement {
	repositories {
		mavenLocal()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenLocal()
		mavenCentral()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "gradle-submodule-test"
include("lib")
include("app")
