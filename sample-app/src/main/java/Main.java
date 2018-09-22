import org.apache.logging.log4j.LogManager;

public class Main {

	public static void main(String[] args) {
		LogManager.getLogger("testlogger").error("testing logging with incompatible log4j version");
	}

}
