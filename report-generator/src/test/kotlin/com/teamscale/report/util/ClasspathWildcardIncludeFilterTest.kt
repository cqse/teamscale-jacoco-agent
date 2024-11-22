package com.teamscale.report.util

import com.teamscale.report.util.ClasspathWildcardIncludeFilter.Companion.getClassName
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClasspathWildcardIncludeFilterTest {
	/** Tests path to class name conversion.  */
	@Test
	fun testPathToClassNameConversion() {
		assertThat(getClassName("file.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar")
		assertThat(getClassName("file.jar@com/foo/Bar\$Goo.class")).isEqualTo("com.foo.Bar.Goo")
		assertThat(getClassName("file1.jar@goo/file2.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar")
		assertThat(getClassName("com/foo/Bar.class")).isEqualTo("com.foo.Bar")
		assertThat(getClassName("com/foo/Bar")).isEqualTo("com.foo.Bar")
		assertThat(
			getClassName(
				"C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class"
			)
		).isEqualTo("com.customer.something.SomeClass")
	}


	@Test
	fun testMatching() {
		assertThat(
			ClasspathWildcardIncludeFilter(null, "org.junit.*")
				.isIncluded("/junit-jupiter-engine-5.1.0.jar@org/junit/jupiter/engine/Constants.class")
		).isFalse()
		assertThat(
			ClasspathWildcardIncludeFilter(null, "org.junit.*")
				.isIncluded("org/junit/platform/commons/util/ModuleUtils\$ModuleReferenceScanner.class")
		).isFalse()
	}
}