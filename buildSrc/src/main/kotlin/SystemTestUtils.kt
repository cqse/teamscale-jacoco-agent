import org.gradle.api.tasks.testing.Test

/**
 * Utilities for system tests.
 */
object SystemTestUtils {

	private var nextFreePort = 6000
	private val lock = Object()

	/**
	 * Used ports should be unique for each system test to avoid collisions during parallel execution.
	 * Thus, we assign a new port each time this is called.
	 * Since tests may run in different workers that don't share the #nextFreePort variable, we use the worker number
	 * to avoid collisions (given that we don't have more than 100 system tests per worker).
	 */
	fun pickFreePort(): Int {
		synchronized(lock) {
			val pickedPort = nextFreePort
			nextFreePort += 1
			return pickedPort
		}
	}

}

/** The port that the fake Teamscale should use during the system test. Guaranteed conflict-free. */
val Test.teamscalePort: Int by lazy { SystemTestUtils.pickFreePort() }

/** The port that the agent should use during the system test. Guaranteed conflict-free. */
val Test.agentPort: Int by lazy { SystemTestUtils.pickFreePort() }
