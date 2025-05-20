package jul.test

/**
 * The `SystemUnderTest` class is responsible for asserting that the LogManager is not initialized yet.
 */
object SystemUnderTest {
	var logManagerInitialized: Boolean = false

	@JvmStatic
	fun main(args: Array<String>) {
		check(!logManagerInitialized) {
			"Expected the LogManager to not be initialized by the agent. " +
					"This is required to make the agent work in certain contexts like JBoss Wildfly. " +
					"It wants to set it's own LogManager, but cannot do so if our agent has already initialized it " +
					"accidentally e.g. by calling `java.util.logging.Logger.getLogger(...)`. " +
					"This should be called nowhere, because we transform all class files automatically to use " +
					"Logger.global instead which works without initializing the LogManager."
		}
	}
}
