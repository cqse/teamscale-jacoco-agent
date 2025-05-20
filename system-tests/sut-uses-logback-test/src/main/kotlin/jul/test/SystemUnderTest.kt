package jul.test

import org.slf4j.LoggerFactory

object SystemUnderTest {
	@JvmStatic
	fun main(args: Array<String>) {
		LoggerFactory.getLogger(SystemUnderTest::class.java).warn("This warning is to test logging in the SUT")
	}
}
