// This file was taken from: https://github.com/TheBoegl/shadow-log4j-transformer/commit/ab1a29d018b0efd315ab3262b1c2fa01c7eb93e3
// Due to this unresolved bug: https://github.com/TheBoegl/shadow-log4j-transformer/issues/7
// Once the bug is resolved, we can use the plugin directly again and delete the buildSrc folder

/*
 * Copyright (c) 2018 Sebastian Boegl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sebastianboegl.gradle.plugins.shadow.transformers

import com.github.jengelman.gradle.plugins.shadow.ShadowStats
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.logging.log4j.core.config.plugins.processor.PluginCache
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry
import org.gradle.api.file.FileTreeElement
import shadow.org.apache.commons.io.IOUtils
import shadow.org.apache.commons.io.output.CloseShieldOutputStream
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream

import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE

/**
 * @author Eduard Gizatullin
 * Modified from https://github.com/edwgiz/maven-shaded-log4j-transformer/blob/f5c358a7d6102f60826543dbb7df6b3f718fe013/src/main/java/com/github/edwgiz/mavenShadePlugin/log4j2CacheTransformer/PluginsCacheFileTransformer.java
 */
class Log4j2PluginsFileTransformer implements Transformer {

    private final ArrayList<File> tempFiles = new ArrayList<File>()
    private RelocatorStats relocatorStats

    @Override
    boolean canTransformResource(FileTreeElement fileTreeElement) {
        return fileTreeElement != null && PLUGIN_CACHE_FILE.equals(fileTreeElement.relativePath.pathString)
    }

    @Override
    void transform(TransformerContext context) {
        final File tempFile = File.createTempFile("Log4j2Plugins", "dat")
        FileOutputStream fos = new FileOutputStream(tempFile)
        try {
            IOUtils.copyLarge(context.is, fos)
        } finally {
            IOUtils.closeQuietly(fos)
        }
        tempFiles.add(tempFile)
        relocatorStats = new RelocatorStats(context.relocators, context.stats)
    }

    @Override
    boolean hasTransformedResource() {
        return tempFiles.size() > 1 || (!tempFiles.isEmpty() && relocatorStats.hasRelocators())
    }

    @Override
    void modifyOutputStream(ZipOutputStream jos) throws IOException {
        try {
            PluginCache aggregator = new PluginCache()
            aggregator.loadCacheFiles(getUrls())

            relocatePlugin(aggregator, relocatorStats)

            jos.putNextEntry(new ZipEntry(PLUGIN_CACHE_FILE))
            aggregator.writeCache(new CloseShieldOutputStream(jos))
        } finally {
            tempFiles.each { it.delete() }
        }
    }

    static void relocatePlugin(PluginCache aggregator, RelocatorStats relocatorStats) {
        for (Map.Entry<String, Map<String, PluginEntry>> categoryEntry : aggregator.getAllCategories().entrySet()) {
            for (Map.Entry<String, PluginEntry> pluginMapEntry : categoryEntry.getValue().entrySet()) {
                PluginEntry pluginEntry = pluginMapEntry.getValue()
                def context = RelocateClassContext.builder().className(pluginEntry.getClassName()).stats(relocatorStats.stats).build()
                Relocator matchingRelocator = findFirstMatchingRelocator(context, relocatorStats.relocators)

                if (matchingRelocator != null) {
                    String newClassName = matchingRelocator.relocateClass(context)
                    pluginEntry.setClassName(newClassName)
                }
            }
        }
    }

    private static Relocator findFirstMatchingRelocator(RelocateClassContext context, List<Relocator> relocators) {
        for (Relocator relocator : relocators) {
            if (relocator.canRelocateClass(context)) {
                return relocator
            }
        }
        return null
    }


    private Enumeration<URL> getUrls() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>()
        for (File tempFile : tempFiles) {
            final URL url = tempFile.toURI().toURL()
            urls.add(url)
        }
        return Collections.enumeration(urls)
    }

    private class RelocatorStats {
        final ShadowStats stats
        final List<Relocator> relocators

        RelocatorStats(List<Relocator> relocators, ShadowStats stats) {
            this.stats = stats
            this.relocators = relocators;
        }

        boolean hasRelocators() {
            return !relocators.empty
        }
    }
}

