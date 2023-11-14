package jul.test;

import java.util.logging.LogManager;

/**
 * A custom log manager extending the base LogManager class.
 *
 * This class initializes the LogManager and sets a flag in the SystemUnderTest class
 * to indicate that the log manager has been initialized.
 *
 * The manager seems unused, but is referenced in the system property of the runWithoutGradleWorker task.
 */
public class CustomLogManager extends LogManager {
	public CustomLogManager() {
		SystemUnderTest.logManagerInitialized = true;
	}
}
