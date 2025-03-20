package systemundertest

/** Fake system under test that is exercised by the fake [testframework.CustomTestFramework].  */
class SystemUnderTest {
	/** Returns 5.  */
	fun foo() = 5

	/** Returns 6.  */
	fun bar() = 6
}
