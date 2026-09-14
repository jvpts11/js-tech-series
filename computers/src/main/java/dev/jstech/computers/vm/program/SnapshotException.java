/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * A snapshot that cannot be written or read back as it stands: the program holds something that is not on its heap,
 * or a save is of another format, was taken from another listing, or holds what no snapshot writes.
 *
 * <p>It never reaches the program. Whatever writes or reads a machine's programs catches it, says so in the server's
 * log and leaves that one program out, so a damaged save costs one program and never the machine or the world.
 */
public final class SnapshotException extends RuntimeException {

    public SnapshotException(final String message) {
        super(message);
    }
}
