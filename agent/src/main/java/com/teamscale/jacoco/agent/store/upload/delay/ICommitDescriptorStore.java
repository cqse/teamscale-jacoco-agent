package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;

/**
 * Store that requires a {@link CommitDescriptor} in order to be able to store the XML file.
 */
public interface ICommitDescriptorStore {

	/** Stores the given XML permanently under the given commit. */
	public void store(String xml, CommitDescriptor commit);

	/** Human-readable description of the store. */
	public String describe();
}
