package bar;

import org.junit.jupiter.api.Test;
import foo.SUT;

/** Dummy test class */
public class UnitTest {

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
