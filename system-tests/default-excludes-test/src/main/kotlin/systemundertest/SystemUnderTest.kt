package systemundertest

import notexcluded.sun.NotExcludedClass
import shadow.ShadowedClass

/** Fake system under test to generate some coverage.  */
class SystemUnderTest {
	fun foo() = ShadowedClass().x() + NotExcludedClass().y()
}
