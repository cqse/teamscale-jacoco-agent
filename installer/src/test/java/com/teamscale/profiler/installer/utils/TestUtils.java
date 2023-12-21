package com.teamscale.profiler.installer.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Generic utilities for tests. */
public class TestUtils {

	private static final EnumSet<AclEntryPermission> READ_ONLY_DENIED_PERMISSIONS = EnumSet.of(
			AclEntryPermission.WRITE_DATA, AclEntryPermission.DELETE, AclEntryPermission.DELETE_CHILD,
			AclEntryPermission.ADD_FILE, AclEntryPermission.ADD_SUBDIRECTORY, AclEntryPermission.APPEND_DATA,
			AclEntryPermission.WRITE_ATTRIBUTES);

	/**
	 * Changes the given path to read-only and asserts that the change was successful. Depending on the operating
	 * system, this may or may not also mark subpaths as read-only, so do not rely on this.
	 */
	public static void makePathReadOnly(Path path) throws IOException {
		if (SystemUtils.IS_OS_WINDOWS) {
			// File#setWritable doesn't work under Windows 11 (always returns false).
			// So we manually set the readonly attribute and some ACLs. Unlike under Linux, this only prevent deletion
			// of this specific path, not its subpaths.
			// Adapted from https://stackoverflow.com/a/25747561/1396068
			DosFileAttributeView dosAttributes = Files.getFileAttributeView(path, DosFileAttributeView.class);
			dosAttributes.setReadOnly(true);

			AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
			UserPrincipal owner = view.getOwner();

			List<AclEntry> newAcl = new ArrayList<>(view.getAcl());
			newAcl.add(0, AclEntry.newBuilder()
					.setType(AclEntryType.DENY)
					.setPrincipal(owner)
					.setPermissions(READ_ONLY_DENIED_PERMISSIONS)
					.setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
					.build());
			view.setAcl(newAcl);
		} else {
			assertThat(path.toFile().setWritable(false, false))
					.withFailMessage("Failed to mark " + path + " as writable = " + false).isTrue();
		}
	}
}
