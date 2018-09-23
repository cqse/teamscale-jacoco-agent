import org.slf4j.LoggerFactory;

public class Main {

	public static void main(String[] args) {
		System.out.println("starting sample-app");
		LoggerFactory.getLogger("testlogger").error("testing logging with incompatible logback version");
	}

}
