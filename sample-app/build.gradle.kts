// Needed to make git properties work with Java 8,
// see https://github.com/n0mer/gradle-git-properties/issues/195#issuecomment-982326268
buildscript {
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit") {
            version {
                strictly("5.13.0.202109080827-r")
            }
        }
    }
}

plugins {
    application
    com.teamscale.`java-convention`
    com.teamscale.coverage
    alias(libs.plugins.gitProperties)
}

version = "unspecified"

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Main"
    }
    // make it a fat jar
    from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
}

dependencies {
    // this logback version is the oldest one available that I could get to work and possibly incompatible
    // with the one used in the agent. This way, we can test if the shadowing works correctly
    implementation("ch.qos.logback:logback-core:1.0.0")
    implementation("ch.qos.logback:logback-classic:1.0.0")
}
