plugins {
	java
	jacoco
}

jacoco {
	toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
	reports {
		xml.required = true
	}
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}
