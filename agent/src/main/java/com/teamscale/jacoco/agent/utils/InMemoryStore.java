package com.teamscale.jacoco.agent.utils;

import com.teamscale.jacoco.agent.store.upload.delay.ICachingXmlStore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fake store that simply stores XMLs in a list.
 */
public class InMemoryStore implements ICachingXmlStore {

	/** The uploaded XMLs. */
	public final List<String> xmls = new ArrayList<>();

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
