package com.teamscale.jacoco.agent.logging;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A Buffer that cn be closed and ignores all inputs once closed. */
class CloseableBuffer<T> implements Iterable<T> {
	private final List<T> BUFFER = new ArrayList<>();

	private boolean closed = false;

	/** Append to the buffer.
	 *
	 * @return whether the given object was appended to the buffer. Returns {@code false} if the buffer is closed.
	 * */
	public boolean append(T object) {
			if (closed) {
				return BUFFER.add(object);
			}
			return false;
	}

	/** Close the buffer. */
	public void close() {
			closed = true;
	}

	/** Clear the buffer. */
	public void clear() {
		BUFFER.clear();
	}


	@NotNull
	@Override
	public Iterator<T> iterator() {
		return BUFFER.iterator();
	}
}
