plugins {
    jacoco
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.isEnabled = true
    }
}
tasks.named("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}