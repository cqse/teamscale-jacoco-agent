package com.teamscale.jacoco.agent.upload;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;

/**
 * Base class for wrapper uploaders that allow uploading the same coverage to
 * multiple locations.
 */
public abstract class DelayedMultiUploaderBase implements IUploader {

	/** Logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	@Override
	public synchronized void upload(CoverageFile file) {
		Collection<IUploader> wrappedUploaders = getWrappedUploaders();
		wrappedUploaders.forEach(uploader -> file.acquireReference());
		if (wrappedUploaders.isEmpty()) {
			logger.warn("No commits have been found yet to which coverage should be uploaded. Discarding coverage");
		} else {
			for (IUploader wrappedUploader : wrappedUploaders) {
				wrappedUploader.upload(file);
			}
		}
	}

	@Override
	public String describe() {
		Collection<IUploader> wrappedUploaders = getWrappedUploaders();
		if (!wrappedUploaders.isEmpty()) {
			return wrappedUploaders.stream().map(IUploader::describe).collect(Collectors.joining(", "));
		}
		return "Temporary stand-in until commit is resolved";
	}

	/** Returns the actual uploaders that this multiuploader wraps. */
	protected abstract Collection<IUploader> getWrappedUploaders();
}
