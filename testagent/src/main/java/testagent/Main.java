package testagent;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {

	public static void main(String[] args) {
		System.out.println("running...");
		try {
			Thread.sleep(1000 * 60 * 10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		if (System.getProperty("com.teamscale.imhere") != null) {
			return;
		}
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String log = "ran " + bean.getName() + "\n";
		System.out.println(log);
		Files.write(Paths.get("/home/k/premain.log"), log.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);

		System.setProperty("com.teamscale.imhere", "true");
	}


}
