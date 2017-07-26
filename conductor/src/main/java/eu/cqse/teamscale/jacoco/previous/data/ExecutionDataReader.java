/*******************************************************************************
 * Copyright (c) 2009, 2015 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package eu.cqse.teamscale.jacoco.previous.data;

import eu.cqse.teamscale.jacoco.previous.internal.data.CompactDataInput;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

/**
 * Deserialization of execution data from binary streams.
 */
public class ExecutionDataReader {

	/** File format version, will be incremented for each incompatible change. */
	private static final char FORMAT_VERSION = 0x1006;

	/** Magic number in header for file format identification. */
	private static final char MAGIC_NUMBER = 0xC0C0;

	/** Block identifier for file headers. */
	private static final byte BLOCK_HEADER = 0x01;

	/** Block identifier for session information. */
	private static final byte BLOCK_SESSIONINFO = 0x10;

	/** Block identifier for execution data of a single class. */
	private static final byte BLOCK_EXECUTIONDATA = 0x11;

	/** Underlying data input */
	protected final CompactDataInput in;

	private ISessionInfoVisitor sessionInfoVisitor = null;

	private IExecutionDataVisitor executionDataVisitor = null;

	private boolean firstBlock = true;

	/**
	 * Creates a new reader based on the given input stream input. Depending on
	 * the nature of the underlying stream input should be buffered as most data
	 * is read in single bytes.
	 * 
	 * @param input
	 *            input stream to read execution data from
	 */
	public ExecutionDataReader(final InputStream input) {
		this.in = new CompactDataInput(input);
	}

	/**
	 * Sets an listener for session information.
	 * 
	 * @param visitor
	 *            visitor to retrieve session info events
	 */
	public void setSessionInfoVisitor(final ISessionInfoVisitor visitor) {
		this.sessionInfoVisitor = visitor;
	}

	/**
	 * Sets an listener for execution data.
	 * 
	 * @param visitor
	 *            visitor to retrieve execution data events
	 */
	public void setExecutionDataVisitor(final IExecutionDataVisitor visitor) {
		this.executionDataVisitor = visitor;
	}

	/**
	 * Reads all data and reports it to the corresponding visitors. The stream
	 * is read until its end or a command confirmation has been sent.
	 * 
	 * @return <code>true</code> if additional data can be expected after a
	 *         command has been executed. <code>false</code> if the end of the
	 *         stream has been reached.
	 * @throws IOException
	 *             might be thrown by the underlying input stream
	 */
	public boolean read() throws IOException {
		try {
			byte type;
			do {
				type = in.readByte();
				if (firstBlock && type != BLOCK_HEADER) {
					throw new IOException("Invalid execution data file.");
				}
				firstBlock = false;
			} while (readBlock(type));
			return true;
		} catch (final EOFException e) {
			return false;
		}
	}

	/**
	 * Reads a block of data identified by the given id. Subclasses may
	 * overwrite this method to support additional block types.
	 * 
	 * @param blocktype
	 *            block type
	 * @return <code>true</code> if there are more blocks to read
	 * @throws IOException
	 *             might be thrown by the underlying input stream
	 */
	private boolean readBlock(final byte blocktype) throws IOException {
		switch (blocktype) {
		case BLOCK_HEADER:
			readHeader();
			return true;
		case BLOCK_SESSIONINFO:
			readSessionInfo();
			return true;
		case BLOCK_EXECUTIONDATA:
			readExecutionData();
			return true;
		default:
			throw new IOException(format("Unknown block type %x.", blocktype));
		}
	}

	private void readHeader() throws IOException {
		if (in.readChar() != MAGIC_NUMBER) {
			throw new IOException("Invalid execution data file.");
		}
		final char version = in.readChar();
		if (version != FORMAT_VERSION) {
			throw new IOException(format("Incompatible version %x.", (int) version));
		}
	}

	private void readSessionInfo() throws IOException {
		if (sessionInfoVisitor == null) {
			throw new IOException("No session info visitor.");
		}
		final String id = in.readUTF();
		final long start = in.readLong();
		final long dump = in.readLong();
		sessionInfoVisitor.visitSessionInfo(new SessionInfo(id, start, dump));
	}

	private void readExecutionData() throws IOException {
		if (executionDataVisitor == null) {
			throw new IOException("No execution data visitor.");
		}
		final long id = in.readLong();
		final String name = in.readUTF();
		final boolean[] probes = in.readBooleanArray();
		executionDataVisitor.visitClassExecution(new ExecutionData(id, name, probes));
	}

}