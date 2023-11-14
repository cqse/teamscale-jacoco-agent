plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
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

file("system-tests").listFiles()?.forEach { folder ->
    include(":system-tests:${folder.name}")
}
