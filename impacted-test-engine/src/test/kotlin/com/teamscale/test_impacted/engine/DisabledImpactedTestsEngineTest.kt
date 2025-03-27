package com.teamscale.test_impacted.engine

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.UniqueId
import org.mockito.kotlin.*
import java.util.*

/** Test setup where the engine was not consciously enabled.  */
internal class DisabledImpactedTestsEngineTest {

	private val discoveryRequest = mock<EngineDiscoveryRequest>()
	private val configurationParameters = mock<ConfigurationParameters>()

	private val root = UniqueId.forEngine("root")

	@Test
	fun verifyDoesNotExecuteTestsWhenDisabled() {
		whenever(configurationParameters.get(any()))
			.thenReturn(Optional.empty())
		whenever(discoveryRequest.configurationParameters)
			.thenReturn(configurationParameters)
		val discover = ImpactedTestEngine().discover(discoveryRequest, root)
		Assertions.assertTrue(discover.children.isEmpty())
		verify(discoveryRequest).configurationParameters
		verifyNoMoreInteractions(discoveryRequest)
	}
}
