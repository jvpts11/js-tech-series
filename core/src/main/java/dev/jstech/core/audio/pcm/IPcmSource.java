/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.Closeable;
import java.io.IOException;

/**
 * Samples, read a stretch at a time until there are none left: a file being decoded, a tune being synthesised, a
 * program's own sound. What reads it never needs to know which, only the {@link #format()} it comes in. It is read
 * once, from the start to its end, and closed after, which lets go of the file under it.
 */
public interface IPcmSource extends Closeable {

    /** The shape of the samples. */
    PcmFormat format();

    /**
     * Reads up to {@code length} samples into {@code into} from {@code offset}, channels interleaved.
     *
     * @return how many were read, fewer than asked only at the end, and -1 once there are none left
     * @throws IOException when the file under it cannot be read or is not what it said it was
     */
    int read(short[] into, int offset, int length) throws IOException;

    /** Lets go of what it reads from; a source that reads from nothing has nothing to let go of. */
    @Override
    default void close() throws IOException {
    }
}
