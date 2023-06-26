plugins {
    `java-library`
    com.teamscale.`java-convention`
}

dependencies {
}

tasks.jar {
    manifest {
        attributes["Premain-Class"] = "testagent.Main"
        attributes["Main-Class"] = "testagent.Main"
    }
}

