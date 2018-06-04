import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import shadow.org.codehaus.plexus.util.IOUtil
import org.gradle.api.file.FileTreeElement
import java.io.InputStream

/**
 * Relocates the log4j-provider.properties file.
 *
 * This files contains a reference to a Log4J class that is read
 * at runtime. Since we use the shadow plugin during the build to relocate
 * all of our dependencies to different packages, we need to update this
 * class name to the relocated class name.
 * This is currently not handled by the log4j relocation plugin we use.
 */
public class Log4JResourceTransformer implements Transformer {

	private static final String PROPERTY_FILE = "log4j-provider.properties"

	private List<String> lines = null
	private String path = null

	boolean canTransformResource(FileTreeElement element) {
		return element.relativePath.lastName.equalsIgnoreCase(PROPERTY_FILE)
	}

	void transform(TransformerContext context) {
		path = context.path
		lines = context.is.readLines()
		lines = lines.collect { relocate(it, context) }
		context.is.close()
	}

	boolean hasTransformedResource() {
		return lines != null
	}

	void modifyOutputStream(ZipOutputStream os) {
		os.putNextEntry(new ZipEntry(path))
		def writer = os.newPrintWriter()
		lines.each {
			writer.println(it)
		}
		writer.flush()
		lines = null
	}

	private String relocate(String line, TransformerContext context) {
		def matcher = line =~ /^(.*)(org.apache.*)$/
		if (!matcher) {
			return line
		}

		def prefix = matcher.group(1)
		def className = matcher.group(2)
		return prefix + relocateClassName(className, context)
	}

	private String relocateClassName(String className, TransformerContext context) {
		def relocatorContext = RelocateClassContext.builder().className(className).stats(context.stats).build()
		for (def relocator : context.relocators) {
			if (relocator.canRelocateClass(relocatorContext)) {
				return relocator.relocateClass(relocatorContext)
			}
		}
		return className
	}
}

