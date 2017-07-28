package eu.cqse.teamscale.jacoco.converter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimerUtils {

	private static final Logger LOGGER = LogManager.getLogger(TimerUtils.class);

	public static <T, X extends Throwable> T time(String description, TimerAction<T, X> action) throws X {
		long startTime = System.nanoTime();
		try {
			return action.run();
		} finally {
			long endTime = System.nanoTime();
			LOGGER.debug("{} took {}s", description, (endTime - startTime) / 1_000_000_000l);
		}
	}

	public interface TimerAction<T, X extends Throwable> {

		public T run() throws X;

	}

}
