package systemundertest;

/** Fake system under test that is exercised by the fake {@link testframework.CustomTestFramework}. */
public class SystemUnderTest {

	/** Returns 5. */
	public int foo() {
		return 5;
	}

	/** Returns 6. */
	public int bar() {
		return 6;
	}
}
