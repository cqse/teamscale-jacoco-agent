import com.github.jengelman.gradle.plugins.shadow.ShadowExtension

plugins {
    java
    `maven-publish`
    signing
}

val extension = extensions.create<PublicationInfoExtension>("publishAs", project)
extension.artifactId.set(name)

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Javadoc>("javadoc") {
    (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            extension.applyTo(this)

            pluginManager.withPlugin("java-library") {
                from(components["java"])
            }

            val publication = this
            pluginManager.withPlugin("com.github.johnrengelman.shadow") {
                val shadowExtension = extensions.getByName<ShadowExtension>("shadow")
                shadowExtension.component(publication)
            }
        }
    }

    repositories {
        maven {
            if (VersionUtils.isTaggedRelease()) {
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            } else {
                setUrl("https://oss.sonatype.org/content/repositories/snapshots")
            }
            if (project.hasProperty("sonatypeUsername") && project.hasProperty("sonatypePassword")) {
                credentials {
                    username = project.property("sonatypeUsername") as String
                    password = project.property("sonatypePassword") as String
                }
            }
        }
    }
}

signing {
    setRequired({
        // Do not require signine for deployment to maven local
        gradle.taskGraph.hasTask("publish")
    })
    sign(publishing.publications["maven"])
}
