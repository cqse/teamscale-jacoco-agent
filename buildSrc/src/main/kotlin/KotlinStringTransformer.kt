import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.objectweb.asm.*
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString

/**
 * Reverts the transformation from "kotlin" to "shadow/kotlin" within strings of
 * "org/jacoco/core/internal/analysis/filter" to make JaCoCo correctly process Kotlin class files.
 */
fun revertKotlinPackageChanges(task: ShadowJar) {
	val zip = task.archiveFile.get().asFile.toPath()
	FileSystems.newFileSystem(zip, null as ClassLoader?).use { fs ->
		Files.walk(fs.getPath("/")).forEach { path ->
			if (!Files.isRegularFile(path)) return@forEach
			if (path.extension == "class" && shouldTransform(path.pathString)) {
				transformClass(path)
			}
		}
	}
}

private fun transformClass(file: Path) {
	Files.newInputStream(file).use { ins ->
		val classReader = ClassReader(ins)
		val classWriter = ClassWriter(classReader, 0)
		val classVisitor = StringReplacerClassVisitor(classWriter)
		classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

		ins.close()
		Files.delete(file)
		Files.write(file, classWriter.toByteArray())
	}
}

private fun shouldTransform(path: String): Boolean {
	return path.contains("org/jacoco/core/internal/analysis/filter")
}

private class StringReplacerClassVisitor(classNode: ClassVisitor?) : ClassVisitor(Opcodes.ASM9, classNode) {

	override fun visitMethod(
		access: Int,
		name: String,
		desc: String,
		signature: String?,
		exceptions: Array<String>?
	): MethodVisitor {
		return StringReplacerMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions))
	}

	override fun visitField(
		access: Int, name: String?, desc: String?,
		signature: String?, cst: Any?
	): FieldVisitor {
		return super.visitField(access, name, desc, signature, replaceString(cst))
	}
}

private class StringReplacerMethodVisitor(mv: MethodVisitor?) : MethodVisitor(Opcodes.ASM9, mv) {
	override fun visitLdcInsn(cst: Any?) {
		super.visitLdcInsn(replaceString(cst))
	}
}

private fun replaceString(cst: Any?): Any? {
	if (cst is String) {
		if (cst.contains("shadow/kotlin")) {
			return cst.replace("shadow/kotlin", "kotlin")
		} else if (cst.contains("shadow.kotlin")) {
			return cst.replace("shadow.kotlin", "kotlin")
		}
	}
	return cst;
}
