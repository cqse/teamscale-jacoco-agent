package eu.cqse.teamscale.report.util;

import org.junit.Test;

import static eu.cqse.teamscale.report.util.ClasspathWildcardIncludeFilter.getClassName;
import static org.assertj.core.api.Assertions.assertThat;

public class ClasspathWildcardIncludeFilterTest {

	/** Tests path to class name conversion. */
	@Test
	public void testPathToClassNameConversion() {
		assertThat(getClassName("file.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar");
		assertThat(getClassName("file.jar@com/foo/Bar$Goo.class")).isEqualTo("com.foo.Bar.Goo");
		assertThat(getClassName("file1.jar@goo/file2.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar");
		assertThat(getClassName("com/foo/Bar.class")).isEqualTo("com.foo.Bar");
		assertThat(getClassName(
				"C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class"))
				.isEqualTo("com.customer.something.SomeClass");
	}


	@Test
	public void testMatching() {
		assertThat(new ClasspathWildcardIncludeFilter(null, "org.junit.*")).rejects("/junit-jupiter-engine-5.1.0.jar@org/junit/jupiter/engine/Constants.class");
		assertThat(new ClasspathWildcardIncludeFilter(null, "org.junit.*")).rejects("org/junit/platform/commons/util/ModuleUtils$ModuleReferenceScanner.class");
	}
}