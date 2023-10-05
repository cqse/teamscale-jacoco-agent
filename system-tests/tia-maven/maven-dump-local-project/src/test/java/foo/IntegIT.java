package bar;

import org.junit.jupiter.api.Test;

import foo.SUT;

public class IntegIT {

	@Test
	public void itBla() throws Exception {
		new SUT().bla();
		Thread.sleep(2000);
	}

	@Test
	public void itFoo() throws Exception {
		new SUT().foo();
		Thread.sleep(2000);
	}
}
