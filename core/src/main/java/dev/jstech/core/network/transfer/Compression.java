/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Bytes made smaller for the way and back. Unpacking stops at a limit the caller sets, so a few bytes that would
 * unpack into a great many are refused before they fill the memory.
 */
public final class Compression {

    private static final int BUFFER = 8 * 1024;

    private Compression() {
    }

    /** {@code bytes} made smaller. */
    public static byte[] pack(final byte[] bytes) {
        final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(bytes);
            deflater.finish();
            final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(BUFFER, bytes.length / 2));
            final byte[] buffer = new byte[BUFFER];
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * {@code packed} unpacked.
     *
     * @throws IOException when the bytes are not packed, or unpack into more than {@code mostBytes}
     */
    public static byte[] unpack(final byte[] packed, final int mostBytes) throws IOException {
        final Inflater inflater = new Inflater();
        try {
            inflater.setInput(packed);
            final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(mostBytes, packed.length * 4 + 16));
            final byte[] buffer = new byte[BUFFER];
            while (!inflater.finished()) {
                final int made = inflater.inflate(buffer);
                if (made == 0 && !inflater.finished() && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new IOException("the bytes end before they unpack");
                }
                if (out.size() + made > mostBytes) {
                    throw new IOException("the bytes unpack into more than " + mostBytes);
                }
                out.write(buffer, 0, made);
            }
            return out.toByteArray();
        } catch (final DataFormatException e) {
            throw new IOException("the bytes are not packed", e);
        } finally {
            inflater.end();
        }
    }
}
