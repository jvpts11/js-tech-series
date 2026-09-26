/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * A recording arriving in pieces, written as they come and checked when the last has come: the right number of bytes,
 * and bytes whose SHA-256 is the name they were sent under. Pieces come in order, over a connection that keeps order,
 * so one out of place means the sending went wrong and the recording is refused rather than put together in any
 * order.
 */
public final class MediaReceiver {

    private final MediaId expected;
    private final int pieces;
    private final OutputStream out;
    private final MessageDigest digest = MediaId.sha256();
    private int next;
    private long received;

    /**
     * @param expected the recording the pieces make, by its name, kind and size
     * @param pieces   how many pieces it comes in
     * @param out      where its bytes are written, which this closes when the last piece has come
     */
    public MediaReceiver(final MediaId expected, final int pieces, final OutputStream out) {
        if (pieces <= 0) {
            throw new IllegalArgumentException("a recording comes in one piece at least: " + pieces);
        }
        this.expected = expected;
        this.pieces = pieces;
        this.out = out;
    }

    /** The recording these pieces make. */
    public MediaId expected() {
        return expected;
    }

    /**
     * Takes the next piece.
     *
     * @throws IOException when it is not the next one, there are more than were said, or they come to more bytes
     *                     than the recording has
     */
    public void accept(final int index, final byte[] data) throws IOException {
        if (index != next || next >= pieces) {
            throw new IOException("piece " + index + " came where piece " + next + " of " + pieces + " was due");
        }
        if (received + data.length > expected.bytes()) {
            throw new IOException("more bytes than the " + expected.bytes() + " the recording has");
        }
        out.write(data);
        digest.update(data);
        received += data.length;
        next++;
        if (next == pieces) {
            out.close();
        }
    }

    /** Whether every piece has come. */
    public boolean complete() {
        return next == pieces;
    }

    /** How many of its bytes have come, for a bar that shows it. */
    public long received() {
        return received;
    }

    /** Whether every piece has come and they are exactly the recording they were sent as. */
    public boolean verified() {
        return complete() && received == expected.bytes()
                && HexFormat.of().formatHex(digest.digest()).equals(expected.hash());
    }
}
