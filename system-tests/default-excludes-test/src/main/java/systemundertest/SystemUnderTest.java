package systemundertest;

import notexcluded.sun.NotExcludedClass;
import shadow.ShadowedClass;

/** Fake system under test to generate some coverage. */
public class SystemUnderTest {

	public int foo() {
		return new ShadowedClass().x() + new NotExcludedClass().y();
	}

}
