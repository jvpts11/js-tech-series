/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.core.text.GameText;
import java.util.List;

/**
 * The drive-letter and parent-folder rules the file explorer and the file dialog share, so a fix to one
 * reaches the other.
 */
final class DrivePaths {

    private DrivePaths() {
    }

    /** A medium's name with its drive letter after it, or on its own where the desktop has no letters. */
    static String onDrive(final String label, final String letter) {
        return letter.isEmpty() ? label : GameText.resolve(FileDialogTexts.ON_DRIVE.with(label, letter));
    }

    /** The parent directory of {@code path} (everything before the final slash), or empty at the root. */
    static String parentOf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    /**
     * The drive letter of a volume: the system disk is C:, then the removable media in the order the tree
     * lists them. Empty where the desktop has no letters, and for a key that is not a listed medium.
     */
    static String letterOf(final boolean posix, final List<DiskFilesPayload.WireVolume> volumes,
                           final String key) {
        if (posix) {
            return "";
        }
        if (key.isEmpty()) {
            return "C:";
        }
        int n = 0;
        for (final DiskFilesPayload.WireVolume volume : volumes) {
            if (volume.removable()) {
                n++;
                if (volume.key().equals(key)) {
                    return (char) ('C' + n) + ":";
                }
            }
        }
        return "";
    }
}
