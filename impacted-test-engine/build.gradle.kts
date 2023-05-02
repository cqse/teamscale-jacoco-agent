plugins {
    `java-library`
    id("com.teamscale.java-convention")
    id("com.teamscale.coverage")
    id("com.teamscale.shadow-convention")
    id("com.teamscale.publish")
}

publishAs {
    readableName.set("Impacted Test Engine")
    description.set("A JUnit 5 engine that handles retrieving impacted tests from Teamscale and organizes their execution")
}

dependencies {
    implementation(project(":teamscale-client"))
    implementation(project(":report-generator"))
    implementation(project(":tia-client"))

    compileOnly(libs.junit.platform.engine)
    compileOnly(libs.junit.platform.commons)
    testImplementation(libs.junit.platform.engine)
}
