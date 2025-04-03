plugins {
	jacoco
}

jacoco {
	toolVersion = "0.8.13"
}

tasks.named<JacocoReport>("jacocoTestReport") {
	reports {
		xml.required.set(true)
	}
}
tasks.named("test") {
	finalizedBy(tasks.named("jacocoTestReport"))
}
