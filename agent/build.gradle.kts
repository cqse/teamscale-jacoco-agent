plugins {
	com.teamscale.`java-convention`
	alias(libs.plugins.markdownToPdf)
	application

	// we don't want to cause conflicts between our dependencies and the target application
	// since the agent will be loaded with the same class loader as the profiled application
	// so we use the shadow plugin to relocate our dependencies
	com.teamscale.`shadow-convention`
	com.teamscale.coverage
	com.teamscale.publish
	com.teamscale.`logger-patch`
}

evaluationDependsOn(":installer")

publishAs {
	artifactId.set("teamscale-jacoco-agent")
	readableName.set("Teamscale JaCoCo Agent")
	description.set("JVM profiler that simplifies various aspects around recording and uploading test coverage")
}

val appVersion = rootProject.extra["appVersion"].toString()
val jacocoVersion = libs.versions.jacoco.get()
val outputVersion = "$appVersion-jacoco-$jacocoVersion"

dependencies {
	implementation(libs.jetty.server)
	implementation(libs.jetty.servlet)

	implementation(libs.jersey.server)
	implementation(libs.jersey.containerServletCore)
	implementation(libs.jersey.containerJettyHttp)
	implementation(libs.jersey.mediaJsonJackson)
	implementation(libs.jersey.hk2)
	runtimeOnly(libs.jakarta.activation.api)

	implementation(project(":teamscale-client"))
	implementation(project(":report-generator"))

	implementation(libs.jacoco.core)
	implementation(libs.jacoco.report)
	implementation(libs.jacoco.agent) {
		artifact {
			classifier = "runtime"
		}
	}

	implementation(libs.logback.core)
	implementation(libs.logback.classic)

	implementation(libs.jcommander)
	implementation(libs.teamscaleLibCommons)

	implementation(libs.retrofit.core)

	implementation(libs.jackson.databind)

	testImplementation(project(":tia-client"))
	testImplementation(libs.retrofit.converter.jackson)
	testImplementation(libs.okhttp.mockwebserver)
}

application {
	mainClass.set("com.teamscale.jacoco.agent.Main")
}

tasks.shadowJar {
	// since this is used as an agent, we want it to always have the same name
	// otherwise people have to adjust their -javaagent parameters after every
	// update
	archiveFileName.set("teamscale-jacoco-agent.jar")

	manifest {
		attributes["Premain-Class"] = "com.teamscale.jacoco.agent.PreMain"
	}
}

tasks.startShadowScripts {
	applicationName = "convert"
}

distributions {
	named("shadow") {
		distributionBaseName.set("teamscale-jacoco-agent")
		contents {
			from(project(":installer").tasks["nativeCompile"]) {
				include("installer")
			}

			from(tasks.readmeToPdf) {
				into("documentation")
				rename("README.pdf", "userguide.pdf")
			}

			filesMatching("**/VERSION.txt") {
				filter {
					it.replace("%APP_VERSION_TOKEN_REPLACED_DURING_BUILD%", outputVersion)
				}
			}
		}
	}
}

tasks.shadowDistZip {
	archiveFileName.set("teamscale-jacoco-agent.zip")
}

tasks.processResources {
	filesMatching("**/app.properties") {
		filter {
			it.replace("%APP_VERSION_TOKEN_REPLACED_DURING_BUILD%", appVersion)
		}
	}
}
