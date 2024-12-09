package com.teamscale.profiler.installer.utils

import org.apache.commons.lang3.SystemUtils
import org.assertj.core.api.Assertions
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.*
import java.util.*

/** Generic utilities for tests.  */
object TestUtils {
	private val READ_ONLY_DENIED_PERMISSIONS = EnumSet.of(
		AclEntryPermission.WRITE_DATA,
		AclEntryPermission.DELETE,
		AclEntryPermission.DELETE_CHILD,
		AclEntryPermission.ADD_FILE,
		AclEntryPermission.ADD_SUBDIRECTORY,
		AclEntryPermission.APPEND_DATA,
		AclEntryPermission.WRITE_ATTRIBUTES
	)

	/**
	 * Changes the given path to read-only and asserts that the change was successful. Depending on the operating
	 * system, this may or may not also mark subpaths as read-only, so do not rely on this.
	 */
	@Throws(IOException::class)
	fun makePathReadOnly(path: Path) {
		if (SystemUtils.IS_OS_WINDOWS) {
			// File#setWritable doesn't work under Windows 11 (always returns false).
			// So we manually set the readonly attribute and some ACLs. Unlike under Linux, this only prevent deletion
			// of this specific path, not its subpaths.
			// Adapted from https://stackoverflow.com/a/25747561/1396068
			val dosAttributes = Files.getFileAttributeView(
				path,
				DosFileAttributeView::class.java
			)
			dosAttributes.setReadOnly(true)

			val view = Files.getFileAttributeView(
				path,
				AclFileAttributeView::class.java
			)
			val owner = view.owner
			view.acl = view.acl.toTypedArray().toMutableList().apply {
				val element = AclEntry.newBuilder()
					.setType(AclEntryType.DENY)
					.setPrincipal(owner)
					.setPermissions(READ_ONLY_DENIED_PERMISSIONS)
					.setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
					.build()
				add(0, element)
			}
		} else {
			Assertions.assertThat(path.toFile().setWritable(false, false))
				.withFailMessage("Failed to mark $path as writable = false").isTrue()
		}
	}
}
