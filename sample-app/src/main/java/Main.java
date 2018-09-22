import org.apache.logging.log4j.LogManager;

public class Main {

	public static void main(String[] args) {
		LogManager.getLogger("testlogger").info("testing logging with incompatible log4j version");
	}

}
