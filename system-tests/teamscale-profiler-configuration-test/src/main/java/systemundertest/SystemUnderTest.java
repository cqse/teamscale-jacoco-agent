package systemundertest;

/** Fake system under test to generate some coverage. */
public class SystemUnderTest {

	public static void main(String[] args) {
		try {
			System.out.println("Production code");
			// Wait for 62 seconds so that one heartbeat is sent
			Thread.sleep(62000);
		} catch (InterruptedException e) {
			// ignore exception
		}
	}

}
