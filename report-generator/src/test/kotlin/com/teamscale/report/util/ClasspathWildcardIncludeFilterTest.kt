package com.teamscale.report.util

import org.junit.Test

import com.teamscale.report.util.ClasspathWildcardIncludeFilter.getClassName
import org.assertj.core.api.Assertions.assertThat

class ClasspathWildcardIncludeFilterTest {

    /** Tests path to class name conversion.  */
    @Test
    fun testPathToClassNameConversion() {
        assertThat(Companion.getClassName("file.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar")
        assertThat(Companion.getClassName("file.jar@com/foo/Bar\$Goo.class")).isEqualTo("com.foo.Bar.Goo")
        assertThat(Companion.getClassName("file1.jar@goo/file2.jar@com/foo/Bar.class")).isEqualTo("com.foo.Bar")
        assertThat(Companion.getClassName("com/foo/Bar.class")).isEqualTo("com.foo.Bar")
        assertThat(
            Companion.getClassName(
                "C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class"
            )
        )
            .isEqualTo("com.customer.something.SomeClass")
    }


    @Test
    fun testMatching() {
        assertThat(
            ClasspathWildcardIncludeFilter(
                null,
                "org.junit.*"
            )
        ).rejects("/junit-jupiter-engine-5.1.0.jar@org/junit/jupiter/engine/Constants.class")
        assertThat(
            ClasspathWildcardIncludeFilter(
                null,
                "org.junit.*"
            )
        ).rejects("org/junit/platform/commons/util/ModuleUtils\$ModuleReferenceScanner.class")
    }
}