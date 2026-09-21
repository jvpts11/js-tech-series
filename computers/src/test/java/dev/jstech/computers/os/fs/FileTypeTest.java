/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeTest {

    @Test
    void fromExtension_resolvesKnownExtensionsCaseInsensitively() {
        assertEquals(java.util.Optional.of(FileType.IQL), FileType.fromExtension("iql"));
        assertEquals(java.util.Optional.of(FileType.IQL), FileType.fromExtension("IQL"));
        assertEquals(java.util.Optional.empty(), FileType.fromExtension("zip"));
    }

    @Test
    void of_givesTheKnownKindOrOtherForAnyOtherExtension() {
        assertEquals(FileType.SGS, FileType.of("SGS"));
        assertEquals(FileType.OTHER, FileType.of("fk"));
        assertEquals(FileType.OTHER, FileType.of(""));
        assertEquals(FileType.OTHER, FileType.of(null));
        assertTrue(FileType.OTHER.userEditable(), "a file of an unknown kind is text like any other");
    }

    @Test
    void fromExtension_neverAnswersOtherForAMissingExtension() {
        assertEquals(java.util.Optional.empty(), FileType.fromExtension(""));
    }

    @Test
    void virtualProjectionsAreTheDatAndTheInstallerFiles() {
        /*
         * A projection is generated, never stored: the .dat from a disk's storage, what an installer
         * shows when opened, and what an installed program leaves in its folder. Every one of them is
         * also closed to the player's edits.
         *
         * A .sys is not one of them. The file that starts a system is really on the disk, because a machine
         * a player cannot wreck is not a machine they own: it can be deleted, and then the machine says at
         * its next start which file it wanted and will not run.
         */
        final java.util.Set<FileType> virtual = java.util.EnumSet.of(FileType.DAT, FileType.EXE, FileType.SH,
                FileType.PKG, FileType.INF, FileType.BIN, FileType.INI, FileType.FON);
        for (FileType t : FileType.values()) {
            assertEquals(virtual.contains(t), t.virtualProjection(), t + " virtual");
            if (virtual.contains(t)) assertFalse(t.userEditable(), t + " must not be editable");
        }
    }

    @Test
    void datIsNotUserEditable() {
        assertFalse(FileType.DAT.userEditable());
        assertFalse(FileType.LOG.userEditable());
        assertTrue(FileType.TXT.userEditable());
    }
}
