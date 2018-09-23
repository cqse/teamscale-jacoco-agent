import org.apache.logging.log4j.LogManager;

/** Main class. */
public class Main {

	/** Main method. */
	public static void main(String[] args) {
		LogManager.getLogger("testlogger").error("testing logging with incompatible log4j version");
	}

}
