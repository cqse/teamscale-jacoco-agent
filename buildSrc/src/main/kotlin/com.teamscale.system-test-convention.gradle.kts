plugins {
    id("com.teamscale.java-convention")
}

tasks.test {
    dependsOn(":agent:shadowJar")
}

dependencies {
    testImplementation(project(":common-system-test"))
}
