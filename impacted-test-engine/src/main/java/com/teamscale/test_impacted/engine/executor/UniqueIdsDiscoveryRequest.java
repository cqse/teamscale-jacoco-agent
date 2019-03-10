package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/** A discovery request for discovering a set of tests by their {@link UniqueId}. */
public class UniqueIdsDiscoveryRequest implements EngineDiscoveryRequest {

	private final List<UniqueIdSelector> uniqueIdSelectors;

	private final ConfigurationParameters configurationParameters;

	UniqueIdsDiscoveryRequest(Set<UniqueId> uniqueIds, ConfigurationParameters configurationParameters) {
		uniqueIdSelectors = uniqueIds.stream().map(DiscoverySelectors::selectUniqueId).collect(toList());
		this.configurationParameters = configurationParameters;
	}

	@Override
	public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
		if (selectorType.equals(UniqueIdSelector.class)) {
			return this.uniqueIdSelectors.stream().filter(selectorType::isInstance).map(selectorType::cast)
					.collect(toList());
		}
		return Collections.emptyList();
	}

	@Override
	public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
		return Collections.emptyList();
	}

	@Override
	public ConfigurationParameters getConfigurationParameters() {
		return configurationParameters;
	}
}
