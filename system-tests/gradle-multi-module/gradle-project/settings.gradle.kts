pluginManagement {
	repositories {
		mavenLocal()
		gradlePluginPortal()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "gradle-submodule-test"
include("module1")
include("module2")

dependencyResolutionManagement {
	repositories {
		mavenLocal()
		mavenCentral()
	}
}
