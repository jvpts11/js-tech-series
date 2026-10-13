/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import java.io.IOException;

/**
 * The pieces of a run of bytes as they arrive: in order, no more than were said, and no more bytes than the run may
 * hold. Pieces come over a connection that keeps order, so one out of place means the sending went wrong, and the run
 * is refused rather than put together in any order.
 */
public final class OrderedPieces {

    private final int pieces;
    private final long mostBytes;
    private int next;
    private long received;

    /**
     * @param pieces    how many pieces the run comes in
     * @param mostBytes the most bytes they may come to
     */
    public OrderedPieces(final int pieces, final long mostBytes) {
        if (pieces <= 0) {
            throw new IllegalArgumentException("a run comes in one piece at least: " + pieces);
        }
        if (mostBytes < 0) {
            throw new IllegalArgumentException("a run holds no less than nothing: " + mostBytes);
        }
        this.pieces = pieces;
        this.mostBytes = mostBytes;
    }

    /**
     * Takes the next piece, {@code length} bytes long.
     *
     * @throws IOException when it is not the next one, there are more than were said, or they come to more bytes than
     *                     the run may hold
     */
    public void accept(final int index, final int length) throws IOException {
        if (index != this.next || this.next >= this.pieces) {
            throw new IOException("piece " + index + " came where piece " + this.next + " of " + this.pieces
                    + " was due");
        }
        if (length < 0 || this.received + length > this.mostBytes) {
            throw new IOException("more bytes than the " + this.mostBytes + " the run may hold");
        }
        this.received += length;
        this.next++;
    }

    /** Whether every piece has come. */
    public boolean complete() {
        return this.next == this.pieces;
    }

    /** How many bytes have come. */
    public long received() {
        return this.received;
    }
}
