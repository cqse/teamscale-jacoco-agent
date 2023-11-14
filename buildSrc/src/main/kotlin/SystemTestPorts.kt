import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test

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

		/** The provider for the build service. You must call #registerWith before using this property. */
		lateinit var provider: Provider<SystemTestPorts>

		/** Registers the SystemTestPorts with the given project. */
		fun registerWith(project: Project) {
			provider =
				project.gradle.sharedServices.registerIfAbsent("system-test-ports", SystemTestPorts::class.java) {
					maxParallelUsages.set(1)
				}
		}

		/** @see SystemTestPorts.pickFreePort */
		fun pickFreePort(): Int {
			return provider.get().pickFreePort()
		}
	}

}

private val teamscalePortMap = mutableMapOf<Test, Int>()

/** The port that the fake Teamscale should use during the system test. Guaranteed conflict-free. */
val Test.teamscalePort: Int
	get() {
		synchronized(teamscalePortMap) {
			return teamscalePortMap.getOrPut(this) { SystemTestPorts.pickFreePort() }
		}
	}

private val agentPortMap = mutableMapOf<Test, Int>()

/** The port that the agent should use during the system test. Guaranteed conflict-free. */
val Test.agentPort: Int
	get() {
		synchronized(agentPortMap) {
			return agentPortMap.getOrPut(this) { SystemTestPorts.pickFreePort() }
		}
	}
