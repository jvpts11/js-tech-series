/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.network.transfer.OrderedPieces;
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
    private final OrderedPieces order;
    private final OutputStream out;
    private final MessageDigest digest = MediaId.sha256();

    /**
     * @param expected the recording the pieces make, by its name, kind and size
     * @param pieces   how many pieces it comes in
     * @param out      where its bytes are written, which this closes when the last piece has come
     */
    public MediaReceiver(final MediaId expected, final int pieces, final OutputStream out) {
        this.expected = expected;
        this.order = new OrderedPieces(pieces, expected.bytes());
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
        this.order.accept(index, data.length);
        this.out.write(data);
        this.digest.update(data);
        if (this.order.complete()) {
            this.out.close();
        }
    }

    /** Whether every piece has come. */
    public boolean complete() {
        return this.order.complete();
    }

    /** How many of its bytes have come, for a bar that shows it. */
    public long received() {
        return this.order.received();
    }

    /** Whether every piece has come and they are exactly the recording they were sent as. */
    public boolean verified() {
        return complete() && received() == this.expected.bytes()
                && HexFormat.of().formatHex(this.digest.digest()).equals(this.expected.hash());
    }
}
