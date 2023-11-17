import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra

/**
 * Synchronizes access to ports for the system tests so two system tests running in parallel don't
 * accidentally try to use the same port.
 */
abstract class SystemTestPorts : BuildService<BuildServiceParameters.None> {

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

	companion object {

		/** Registers the SystemTestPorts with the given project. */
		fun registerWith(project: Project): Provider<SystemTestPorts> {
			return project.gradle.sharedServices.registerIfAbsent("system-test-ports", SystemTestPorts::class.java) {}
		}
	}
}

/** Provider for the SystemTestPorts build service. */
@Suppress("UNCHECKED_CAST")
var Test.portProvider: Provider<SystemTestPorts>
	get() = extra["portProvider"] as Provider<SystemTestPorts>
	set(value) {
		extra["portProvider"] = value
	}

/** The port that the fake Teamscale should use during the system test. Guaranteed conflict-free. */
var Test.teamscalePort: Int
	get() = extra["teamscalePort"] as Int
	set(value) {
		extra["teamscalePort"] = value
	}

/** The port that the agent should use during the system test. Guaranteed conflict-free. */
var Test.agentPort: Int
	get() = extra["agentPort"] as Int
	set(value) {
		extra["agentPort"] = value
	}
