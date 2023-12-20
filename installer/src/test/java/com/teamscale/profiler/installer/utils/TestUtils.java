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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Generic utilities for tests. */
public class TestUtils {

	/** Changes the given path to read-only and asserts that the change was successful. */
	public static void makePathReadOnly(Path path) throws IOException {
		if (SystemUtils.IS_OS_WINDOWS) {
			AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);

			List<AclEntry> newAcl = new ArrayList<>(view.getAcl());
			newAcl.add(AclEntry.newBuilder()
					.setType(AclEntryType.DENY)
					.setPrincipal(view.getOwner())
					.setPermissions(AclEntryPermission.WRITE_DATA, AclEntryPermission.DELETE,
							AclEntryPermission.DELETE_CHILD,
							AclEntryPermission.ADD_FILE, AclEntryPermission.ADD_SUBDIRECTORY,
							AclEntryPermission.APPEND_DATA)
					.setFlags(AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.FILE_INHERIT)
					.build());
			view.setAcl(newAcl);
		} else {
			assertThat(path.toFile().setWritable(false, false))
					.withFailMessage("Failed to mark " + path + " as writable = " + false).isTrue();
		}
	}
}
