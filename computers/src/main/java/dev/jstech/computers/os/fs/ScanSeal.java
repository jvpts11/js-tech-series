/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The key the running world seals its scans with ({@link ScanFile}): the world keeps it, and it is handed here while the
 * world runs, so a disk can tell a scan the world wrote from one written by hand. With no world running there is no
 * key, and no scan is taken as good.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class ScanSeal {

    private static volatile byte @Nullable [] current;

    private ScanSeal() {
    }

    /** The running world's key, or empty while no world runs. */
    public static Optional<byte[]> key() {
        final byte[] key = current;
        return key == null ? Optional.empty() : Optional.of(key.clone());
    }

    /** Hands over the running world's key, or takes it back with null when the world stops. */
    public static void use(final byte @Nullable [] key) {
        current = key == null ? null : key.clone();
    }
}
