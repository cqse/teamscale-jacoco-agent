plugins {
    id("application")
    id("com.example.java-convention")
    id("com.example.aggregate")
}

application {
    mainClass = "com.example.app.Main"
}

dependencies {
    implementation(project(":lib"))
}
