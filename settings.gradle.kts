pluginManagement {
    plugins {
        kotlin("jvm") version "2.1.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

include(":agent")
include(":report-generator")
include(":teamscale-gradle-plugin")
include(":teamscale-client")
include(":sample-app")
include(":impacted-test-engine")
include(":tia-client")
include(":tia-runlisteners")
include(":common-system-test")
include(":sample-debugging-app")
include(":teamscale-maven-plugin")
include(":installer")

file("system-tests").listFiles { file: File -> !file.isHidden && file.isDirectory }?.forEach { folder ->
    include(":system-tests:${folder.name}")
}
