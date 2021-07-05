plugins {
    jacoco
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
    }
}
tasks.named("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}