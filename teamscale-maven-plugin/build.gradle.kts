import org.codehaus.plexus.util.Os

abstract class MavenExec : Exec() {
	@TaskAction
	override fun exec() {
		executable = "./mvnw"
		if (Os.isFamily(Os.FAMILY_WINDOWS)) {
			executable = "cmd"
			args(listOf("/c", "mvnw.cmd") + (args ?: emptyList()))
		}
		workingDir(".")
		project.file("pom.xml").writeText(
			project.file("pom.xml").readText().replace(
				Regex("<teamscale.agent.version>[^<]+</teamscale.agent.version>"),
				"<teamscale.agent.version>${project.version}</teamscale.agent.version>"
			).replace(
				Regex("<revision>[^<]+</revision>"),
				"<revision>${project.version}</revision>"
			)
		)
		super.exec()
		project.file("pom.xml").writeText(
			project.file("pom.xml").readText().replace(
				Regex("<teamscale.agent.version>[^<]+</teamscale.agent.version>"),
				"<teamscale.agent.version>34.0.0</teamscale.agent.version>"
			).replace(
				Regex("<revision>[^<]+</revision>"),
				"<revision>1.0.0-SNAPSHOT</revision>"
			)
		)
	}
}

tasks.register<MavenExec>("clean") {
	group = "build"
	description = "Cleans the build directory"
	args("clean")
}

tasks.register<MavenExec>("build") {
	group = "build"
	description = "Builds the project and runs tests"
	args(
		"verify",
		"-Drevision=${project.version}",
		"-Dteamscale.agent.version=${project.version}"
	)
}

tasks.register<MavenExec>("publishToMavenLocal") {
	group = "publishing"
	description = "Publishes the project to the local Maven repository"
	args(
		"install",
		"-Drevision=${project.version}",
		"-Dteamscale.agent.version=${project.version}"
	)
}

tasks.withType<MavenExec> {
	if (name != "clean") {
		dependsOn(":agent:publishToMavenLocal")
		dependsOn(":teamscale-client:publishToMavenLocal")
		dependsOn(":impacted-test-engine:publishToMavenLocal")
	}
}

if (project.hasProperty("sonatypeUsername") &&
	project.hasProperty("sonatypePassword") &&
	project.hasProperty("signing.keyId") &&
	project.hasProperty("gpgDirectory")
) {

	tasks.register<MavenExec>("publishMavenPublicationToSonatypeRepository") {
		group = "publishing"
		description = "Publishes the Maven publication to the Sonatype repository"
		doFirst {
			file("/tmp/maven-settings.xml").writeText(
				"""
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>${project.property("sonatypeUsername")}</username>
            <password>${project.property("sonatypePassword")}</password>
        </server>
    </servers>
</settings>
"""
			)
		}
		args(
			"deploy", "-s", "/tmp/maven-settings.xml",
			"-Dgpg.passphrase=${project.property("signing.password")}",
			"-Dgpg.homedir=${project.property("gpgDirectory")}",
			"-Dgpg.keyname=${project.property("signing.keyId")}",
			"-DsecretKeyRingFile=${project.property("signing.secretKeyRingFile")}"
		)
	}
}
