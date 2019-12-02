package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.jacoco.agent.store.IXmlStore;

import java.util.stream.Stream;

/**
 * Store that temporarily caches XMLs. The XMLs can later be read again and forwarded to their final destination.
 */
public interface ICachingXmlStore extends IXmlStore {

	/** Streams all cached XMLs. This is a {@link Stream} so that only one XML file is held in memory at any time. */
	public Stream<String> streamCachedXmls();

	/** Deletes all cached XMLs. */
	public void clear();

}
