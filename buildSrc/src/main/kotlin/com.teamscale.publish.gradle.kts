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
}

signing {
    sign(publishing.publications["maven"])
}
