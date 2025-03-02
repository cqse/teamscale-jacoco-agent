package com.teamscale.utils

import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.TestSuiteName
import org.gradle.api.attributes.VerificationType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.named

const val TESTWISE_COVERAGE_REPORT = "testwise-coverage"
const val JUNIT_REPORT = "junit"

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

fun AttributeContainer.testwiseCoverageReports(objectFactory: ObjectFactory, testSuiteName: String) {
	attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named<Category>(Category.VERIFICATION))
	attribute(
		TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
		objectFactory.named<TestSuiteName>(testSuiteName)
	)
	attribute(
		VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
		objectFactory.named<VerificationType>(TESTWISE_COVERAGE_REPORT)
	)
}

fun AttributeContainer.junitReports(objectFactory: ObjectFactory, testSuiteName: String) {
	attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named<Category>(Category.VERIFICATION))
	attribute(
		TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
		objectFactory.named<TestSuiteName>(testSuiteName)
	)
	attribute(
		VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
		objectFactory.named<VerificationType>(JUNIT_REPORT)
	)
}
