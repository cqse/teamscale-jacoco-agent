package bar;

import org.junit.jupiter.api.Test;
import foo.SUT;

public class sixUnitTest {

	@Test
	public void itBla() throws Exception {
		new SUT().bla();
		// Add sleep so we can simulate test execution and test parallel test execution
		Thread.sleep(10);
	}

	@Test
	public void itFoo() throws Exception {
		new SUT().foo();
		// Add sleep so we can simulate test execution and test parallel test execution
		Thread.sleep(10);
	}


}
