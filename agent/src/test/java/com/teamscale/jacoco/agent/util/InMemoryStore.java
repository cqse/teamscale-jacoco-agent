package com.teamscale.jacoco.agent.util;

import com.teamscale.jacoco.agent.store.upload.delay.ICachingXmlStore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fake store that simply stores XMLs in a list.
 */
public class InMemoryStore implements ICachingXmlStore {

	private final List<String> xmls = new ArrayList<>();

	public List<String> getXmls() {
		return xmls;
	}

	@Override
	public Stream<String> streamCachedXmls() {
		return xmls.stream();
	}

	@Override
	public void clear() {
		xmls.clear();
	}

	@Override
	public void store(String xml) {
		xmls.add(xml);
	}

	@Override
	public String describe() {
		return "in memory";
	}
}
