/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

/**
 * How a run of bytes too large for one message is cut into pieces of a fixed size: how many pieces, and where each
 * starts and how long it is. Even an empty run is one piece, so the other side always hears that it came.
 */
public final class Pieces {

    private Pieces() {
    }

    /** How many pieces of {@code pieceBytes} a run of {@code bytes} comes in; one at least. */
    public static int count(final long bytes, final int pieceBytes) {
        check(pieceBytes);
        if (bytes < 0) {
            throw new IllegalArgumentException("a run of bytes is never shorter than nothing: " + bytes);
        }
        final long pieces = Math.max(1L, (bytes + pieceBytes - 1) / pieceBytes);
        if (pieces > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("a run of " + bytes + " bytes is too long to send in pieces");
        }
        return (int) pieces;
    }

    /** Where piece {@code index} starts. */
    public static long start(final int index, final int pieceBytes) {
        check(pieceBytes);
        return (long) index * pieceBytes;
    }

    /** How long piece {@code index} of a run of {@code bytes} is: the size of a piece, or what is left for the last. */
    public static int length(final int index, final long bytes, final int pieceBytes) {
        return (int) Math.max(0L, Math.min(pieceBytes, bytes - start(index, pieceBytes)));
    }

    private static void check(final int pieceBytes) {
        if (pieceBytes <= 0) {
            throw new IllegalArgumentException("a piece holds one byte at least: " + pieceBytes);
        }
    }
}
