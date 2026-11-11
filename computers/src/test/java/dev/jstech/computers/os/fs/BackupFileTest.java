/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackupFileTest {

    @Test
    void read_readsBackWhatWriteWrote() {
        final BackupFile file = new BackupFile(UUID.fromString("00000000-0000-0000-0000-000000000042"), 123_456L);
        assertEquals(file, BackupFile.read(file.write()));
    }

    @Test
    void read_isNullForAnythingThatIsNoBackupFile() {
        assertNull(BackupFile.read(null));
        assertNull(BackupFile.read("hello"));
        assertNull(BackupFile.read(BackupFile.MAGIC + "\nnot-a-uuid 12\n"));
        assertNull(BackupFile.read(BackupFile.MAGIC + "\n" + UUID.randomUUID() + "\n"));
    }

    @Test
    void bytesOf_weighsABackupFileWhatItsBackupWeighs() {
        final String content = new BackupFile(UUID.randomUUID(), 9_000_000L).write();
        assertEquals(9_000_000L, StoredFile.bytesOf(FileType.BAK, content));
        assertEquals(content.length(), StoredFile.bytesOf(FileType.TXT, content));
    }
}
