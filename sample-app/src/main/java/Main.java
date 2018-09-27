import org.slf4j.LoggerFactory;

/** Main class. */
public class Main {

	/** Main method. */
	public static void main(String[] args) {
		LoggerFactory.getLogger("testlogger").error("testing logging with incompatible logback version");
	}

}
