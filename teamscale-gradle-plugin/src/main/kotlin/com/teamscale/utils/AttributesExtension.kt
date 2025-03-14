package com.teamscale.utils

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.named

/** Value used for VERIFICATION_TYPE_ATTRIBUTE to mark the binary testwise coverage variants. */
const val TESTWISE_COVERAGE_REPORT = "testwise-coverage"

/** Value used for VERIFICATION_TYPE_ATTRIBUTE to mark the JUnit report variants. */
const val JUNIT_REPORT = "junit"

/** Configures the attributes for a configuration that consumes/exposes JaCoCo binary data. */
fun AttributeContainer.jacocoResults(objectFactory: ObjectFactory, testSuiteName: Provider<String>) {
	attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named<Category>(Category.VERIFICATION))
	attributeProvider(
		TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
		testSuiteName.map { objectFactory.named<TestSuiteName>(it) })
	attribute(
		VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
		objectFactory.named<VerificationType>(VerificationType.JACOCO_RESULTS)
	)
}

/** Configures the attributes for a configuration that consumes/exposes testwise coverage binary data. */
fun AttributeContainer.testwiseCoverageResults(objectFactory: ObjectFactory, testSuiteName: Provider<String>) {
	attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named<Category>(Category.VERIFICATION))
	attributeProvider(
		TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
		testSuiteName.map { objectFactory.named<TestSuiteName>(it) })
	attribute(
		VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
		objectFactory.named<VerificationType>(TESTWISE_COVERAGE_REPORT)
	)
}

/** Configures the attributes for a configuration that consumes/exposes JUnit reports. */
fun AttributeContainer.junitReports(objectFactory: ObjectFactory, testSuiteName: Provider<String>) {
	attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named<Category>(Category.VERIFICATION))
	attributeProvider(
		TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
		testSuiteName.map { objectFactory.named<TestSuiteName>(it) })
	attribute(
		VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
		objectFactory.named<VerificationType>(JUNIT_REPORT)
	)
}

/** Configures the attributes for a configuration that consumes classes from dependent projects. */
fun AttributeContainer.classDirectories(objectFactory: ObjectFactory) {
	attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objectFactory.named<LibraryElements>(LibraryElements.CLASSES))
}

/** Configures the artifact type to filter by. */
fun AttributeContainer.artifactType(artifactType: String) {
	attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, artifactType)
}
