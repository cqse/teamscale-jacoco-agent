pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "gradle-cucumber"

dependencyResolutionManagement {
	repositories {
		mavenLocal()
		mavenCentral()
	}
}

