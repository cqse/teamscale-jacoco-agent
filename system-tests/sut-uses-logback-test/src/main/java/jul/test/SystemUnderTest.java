package jul.test;

import org.slf4j.LoggerFactory;

public class SystemUnderTest {

	public static void main(String[] args) {
		LoggerFactory.getLogger(SystemUnderTest.class).warn("This warning is to test logging in the SUT");
	}

}
