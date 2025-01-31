plugins {
    `java-library`
    com.teamscale.`kotlin-convention`
    com.teamscale.coverage
    com.teamscale.`shadow-convention`
    com.teamscale.publish
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
    testImplementation(libs.junit.jupiter.params)
	testImplementation(libs.mockito.kotlin)
}
