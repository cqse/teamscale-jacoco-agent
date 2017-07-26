/*******************************************************************************
 * Copyright (c) 2009, 2017 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package eu.cqse.teamscale.jacoco.common.cache;

import eu.cqse.teamscale.jacoco.current.analysis.CachingClassAnalyzer;
import org.jacoco.core.internal.ContentTypeDetector;
import org.jacoco.core.internal.Java9Support;
import org.jacoco.core.internal.Pack200Streams;
import org.jacoco.core.internal.analysis.StringPool;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.internal.flow.ClassProbesAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An {@link AnalyzerCache} instance processes a set of Java class files and
 * builds a {@link ProbeLookup} for each of them. The {@link AnalyzerCache} offers
 * several methods to analyze classes from a variety of sources.
 * <p>
 * It's core is a copy of {@link org.jacoco.core.analysis.Analyzer} that has been
 * extended with caching functionality to speed up report generation.
 */
public class AnalyzerCache {

    private final StringPool stringPool = new StringPool();
    private final ProbesCache probesCache;
    private boolean useCurrentFormat;

    /**
     * Creates a new analyzer filling the given cache.
     */
    public AnalyzerCache(ProbesCache probesCache, boolean useCurrentBinaryFormat) {
        this.probesCache = probesCache;
        this.useCurrentFormat = useCurrentBinaryFormat;
    }

    /**
     * Creates an ASM class visitor for analysis.
     *
     * @return ASM visitor to write class definition to
     */
    private ClassVisitor createCurrentAnalyzingVisitor(long classId, String className) {
        CachingClassAnalyzer classAnalyzer = new CachingClassAnalyzer(stringPool,
                probesCache.addClass(classId, className));
        return new ClassProbesAdapter(classAnalyzer, false);
    }

    private ClassVisitor createPreviousAnalyzingVisitor(long classId, String className) {
        eu.cqse.teamscale.jacoco.previous.analysis.CachingClassAnalyzer classAnalyzer = new eu.cqse.teamscale.jacoco.previous.analysis.CachingClassAnalyzer(stringPool,
                probesCache.addClass(classId, className));
        return new eu.cqse.teamscale.jacoco.previous.internal.flow.ClassProbesAdapter(classAnalyzer, false);
    }

    /**
     * Analyzes the class given as a ASM reader.
     *
     * @param reader reader with class definitions
     */
    public void analyzeClass(final ClassReader reader) {
        long classId = CRC64.checksum(reader.b);
        if (probesCache.containsClassId(classId)) {
            return;
        }
        final ClassVisitor visitor;
        if(useCurrentFormat) {
            visitor = createCurrentAnalyzingVisitor(classId, reader.getClassName());
        } else {
            visitor = createPreviousAnalyzingVisitor(classId, reader.getClassName());
        }
        reader.accept(visitor, 0);
    }

    /**
     * Analyzes the class definition from a given in-memory buffer.
     *
     * @param buffer   class definitions
     * @param location a location description used for exception messages
     * @throws IOException if the class can't be analyzed
     */
    public void analyzeClass(final byte[] buffer, final String location) throws IOException {
        try {
            analyzeClass(new ClassReader(Java9Support.downgradeIfRequired(buffer)));
        } catch (final RuntimeException cause) {
            throw analyzerError(location, cause);
        }
    }

    /**
     * Analyzes the class definition from a given input stream.
     *
     * @param input    stream to read class definition from
     * @param location a location description used for exception messages
     * @throws IOException if the stream can't be read or the class can't be analyzed
     */
    public void analyzeClass(final InputStream input, final String location)
            throws IOException {
        try {
            analyzeClass(Java9Support.readFully(input), location);
        } catch (final RuntimeException e) {
            throw analyzerError(location, e);
        }
    }

    private IOException analyzerError(final String location,
                                      final Exception cause) {
        final IOException ex = new IOException(String.format(
                "Error while analyzing %s.", location));
        ex.initCause(cause);
        return ex;
    }

    /**
     * Analyzes all classes found in the given input stream. The input stream
     * may either represent a single class file, a ZIP archive, a Pack200
     * archive or a gzip stream that is searched recursively for class files.
     * All other content types are ignored.
     *
     * @param input    input data
     * @param location a location description used for exception messages
     * @return number of class files found
     * @throws IOException if the stream can't be read or a class can't be analyzed
     */
    public int analyzeAll(final InputStream input, final String location)
            throws IOException {
        final ContentTypeDetector detector;
        try {
            detector = new ContentTypeDetector(input);
        } catch (IOException e) {
            throw analyzerError(location, e);
        }
        switch (detector.getType()) {
            case ContentTypeDetector.CLASSFILE:
                analyzeClass(detector.getInputStream(), location);
                return 1;
            case ContentTypeDetector.ZIPFILE:
                return analyzeZip(detector.getInputStream(), location);
            case ContentTypeDetector.GZFILE:
                return analyzeGzip(detector.getInputStream(), location);
            case ContentTypeDetector.PACK200FILE:
                return analyzePack200(detector.getInputStream(), location);
            default:
                return 0;
        }
    }

    /**
     * Analyzes all class files contained in the given file or folder. Class
     * files as well as ZIP files are considered. Folders are searched
     * recursively.
     *
     * @param file file or folder to look for class files
     * @return number of class files found
     * @throws IOException if the file can't be read or a class can't be analyzed
     */
    public int analyzeAll(final File file) throws IOException {
        int count = 0;
        if (file.isDirectory()) {
            for (final File f : file.listFiles()) {
                count += analyzeAll(f);
            }
        } else {
            final InputStream in = new FileInputStream(file);
            try {
                count += analyzeAll(in, file.getPath());
            } finally {
                in.close();
            }
        }
        return count;
    }

    private int analyzeZip(final InputStream input, final String location)
            throws IOException {
        final ZipInputStream zip = new ZipInputStream(input);
        ZipEntry entry;
        int count = 0;
        while ((entry = nextEntry(zip, location)) != null) {
            count += analyzeAll(zip, location + "@" + entry.getName());
        }
        return count;
    }

    private ZipEntry nextEntry(ZipInputStream input, String location)
            throws IOException {
        try {
            return input.getNextEntry();
        } catch (IOException e) {
            throw analyzerError(location, e);
        }
    }

    private int analyzeGzip(final InputStream input, final String location)
            throws IOException {
        GZIPInputStream gzipInputStream;
        try {
            gzipInputStream = new GZIPInputStream(input);
        } catch (IOException e) {
            throw analyzerError(location, e);
        }
        return analyzeAll(gzipInputStream, location);
    }

    private int analyzePack200(final InputStream input, final String location)
            throws IOException {
        InputStream unpackedInput;
        try {
            unpackedInput = Pack200Streams.unpack(input);
        } catch (IOException e) {
            throw analyzerError(location, e);
        }
        return analyzeAll(unpackedInput, location);
    }
}
