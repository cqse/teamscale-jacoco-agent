plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
	implementation("com.teamscale:teamscale-gradle-plugin:${System.getenv("AGENT_VERSION") ?: "35.2.1-SNAPSHOT"}")
}
