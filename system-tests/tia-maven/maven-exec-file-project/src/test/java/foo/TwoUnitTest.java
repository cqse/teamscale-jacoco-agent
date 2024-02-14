package bar;

import org.junit.jupiter.api.Test;
import foo.SUT;

/** Dummy test class 2 */
public class TwoUnitTest {

	/** Dummy test*/
	@Test
	public void itBla() throws Exception {
		new SUT().bla();
	}
	/** Dummy test*/
	@Test
	public void itFoo() throws Exception {
		new SUT().foo();
	}
}
