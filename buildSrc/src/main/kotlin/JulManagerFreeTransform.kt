import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.objectweb.asm.*
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.slf4j.Logger
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream


/**
 * The JulManagerFreeTransform class implements an artifact transform for jar file. It transforms them by creating a
 * new jar file with the same entries, but rewriting all contained class files. It replaces all calls to
 * java.util.logging.Logger.getLogger(...) with Logger.global, which works without instantiating a LogManager, which
 * is needed to make the agent work in JBoss Wildfly (see TS-35526).
 */
@CacheableTransform
abstract class JulManagerFreeTransform : TransformAction<TransformParameters.None> {

	/** The jar file to be transformed. */
	@get:PathSensitive(PathSensitivity.NAME_ONLY)
	@get:InputArtifact
	abstract val inputArtifact: Provider<FileSystemLocation>

	override fun transform(outputs: TransformOutputs) {
		val originalJar = inputArtifact.get().asFile
		val target = outputs.file(originalJar.nameWithoutExtension + "-jul-manager-free.jar")

		JarInputStream(originalJar.inputStream()).use { input ->
			JarOutputStream(target.outputStream(), input.manifest).use { output ->
				var jarEntry = input.nextJarEntry
				while (jarEntry != null) {
					transformJarEntry(jarEntry, input, output)
					output.closeEntry()
					jarEntry = input.nextJarEntry
				}
			}
		}
	}

	private fun transformJarEntry(
		jarEntry: JarEntry,
		input: JarInputStream,
		output: JarOutputStream
	) {
		if (jarEntry.name.endsWith(".class")) {
			val classReader = ClassReader(input)
			val classWriter = ClassWriter(classReader, 0)
			val stringReplacer = StringValueReplacer(classWriter) {
				if (it.startsWith("logback.")) "shadow.$it" else it
			}
			val classVisitor = IdentifyStaticGetLoggerCalls(stringReplacer)
			classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
			val rewrittenClass = classWriter.toByteArray()

			val newSize = rewrittenClass.size.toLong()
			val je = JarEntry(jarEntry.name)
			je.size = newSize
			output.putNextEntry(je)
			output.write(rewrittenClass)
		} else {
			output.putNextEntry(jarEntry)
			input.copyTo(output)
		}
	}

	private class IdentifyStaticGetLoggerCalls(
		next: ClassVisitor?
	) : ClassVisitor(
		Opcodes.ASM9, next
	) {
		override fun visitMethod(
			access: Int,
			name: String,
			descriptor: String,
			signature: String?,
			exceptions: Array<String>?
		): MethodVisitor {
			return StaticGetLoggerReplacer(
				api,
				super.visitMethod(access, name, descriptor, signature, exceptions),
				access,
				name,
				descriptor
			)
		}
	}
}

private class StaticGetLoggerReplacer(
	api: Int,
	visitMethod: MethodVisitor?,
	access: Int,
	name: String,
	descriptor: String,
) : GeneratorAdapter(
	api,
	visitMethod,
	access,
	name,
	descriptor
) {
	override fun visitMethodInsn(
		opcode: Int,
		owner: String,
		name: String,
		descriptor: String,
		isInterface: Boolean
	) {
		val method = Method(name, descriptor)
		if (opcode == Opcodes.INVOKESTATIC && GET_LOGGER_METHODS.contains(method)) {
			// pop the arguments off the stack
			for (ignored in method.argumentTypes) {
				pop() // all the arguments we match are strings with a size of 1
			}
			getStatic(LOGGER_TYPE, "global", LOGGER_TYPE)
			logger.info("Adjusted logger call in {}::{}", owner, name)
		} else {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
		}
	}

	companion object {
		private val logger: Logger = Logging.getLogger(JulManagerFreeTransform::class.java)
		private val LOGGER_TYPE = Type.getType(java.util.logging.Logger::class.java)
		private val STRING_TYPE = Type.getType(String::class.java)
		private val GET_LOGGER_METHODS = setOf(
			Method("getLogger", LOGGER_TYPE, arrayOf(STRING_TYPE)),
			Method("getLogger", LOGGER_TYPE, arrayOf(STRING_TYPE, STRING_TYPE))
		)
	}
}

/** Replaces string constants within a class file using the given replace function. */
private class StringValueReplacer(
	classVisitor: ClassVisitor?,
	private val stringReplacer: (string: String) -> String,
) : ClassVisitor(Opcodes.ASM9, classVisitor) {

	private fun transformConstant(value: Any?): Any? {
		return if (value is String) stringReplacer(value) else value
	}

	override fun visitField(
		access: Int,
		name: String?,
		descriptor: String?,
		signature: String?,
		value: Any?
	): FieldVisitor {
		return super.visitField(access, name, descriptor, signature, transformConstant(value))
	}

	override fun visitMethod(
		access: Int,
		name: String,
		descriptor: String,
		signature: String?,
		exceptions: Array<String>?
	): MethodVisitor {
		val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
		return object : MethodVisitor(Opcodes.ASM9, mv) {
			override fun visitLdcInsn(cst: Any?) {
				super.visitLdcInsn(transformConstant(cst))
			}
		}
	}
}
