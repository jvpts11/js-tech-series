/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Arrays;

/**
 * Unpacks the LZ4 blocks USD's crate files keep their tables in, as its library packs them: a first byte counting the
 * blocks, 0 for one that runs to the end, and before each of several its length; each block runs of literal bytes and
 * copies from what came before, as the LZ4 block format lays them. A block that reaches outside what it has unpacked
 * is refused. Pure.
 */
final class Lz4 {

    private static final int MORE = 15;
    private static final int LEAST_MATCH = 4;
    /* The most one table unpacks into. */
    private static final int MOST = 1 << 29;

    private Lz4() {
    }

    /**
     * The bytes {@code packed} unpacks into, about {@code expected} of them.
     *
     * @throws ModelFormatException when it is not packed as it should be
     */
    static byte[] decompress(final byte[] packed, final int expected) {
        if (packed.length == 0) {
            return new byte[0];
        }
        final Output out = new Output(Math.max(16, Math.min(expected, MOST)));
        final int blocks = packed[0] & 0xFF;
        if (blocks == 0) {
            block(packed, 1, packed.length, out);
        } else {
            int at = 1;
            for (int i = 0; i < blocks; i++) {
                final int size = packed[at] & 0xFF | (packed[at + 1] & 0xFF) << 8 | (packed[at + 2] & 0xFF) << 16
                        | (packed[at + 3] & 0xFF) << 24;
                at += 4;
                if (size < 0 || at + size > packed.length) {
                    throw new ModelFormatException("an LZ4 block runs past its data");
                }
                block(packed, at, at + size, out);
                at += size;
            }
        }
        return out.bytes();
    }

    /* One block, from {@code at} to {@code end}, onto what came before. */
    private static void block(final byte[] in, final int start, final int end, final Output out) {
        int at = start;
        while (at < end) {
            final int token = in[at++] & 0xFF;
            int literals = token >>> 4;
            if (literals == MORE) {
                int more;
                do {
                    more = in[at++] & 0xFF;
                    literals += more;
                } while (more == 0xFF);
            }
            if (at + literals > end) {
                throw new ModelFormatException("an LZ4 block's literals run past its end");
            }
            out.write(in, at, literals);
            at += literals;
            if (at >= end) {
                return;
            }
            final int offset = in[at] & 0xFF | (in[at + 1] & 0xFF) << 8;
            at += 2;
            int length = token & MORE;
            if (length == MORE) {
                int more;
                do {
                    more = in[at++] & 0xFF;
                    length += more;
                } while (more == 0xFF);
            }
            length += LEAST_MATCH;
            if (offset == 0 || offset > out.size) {
                throw new ModelFormatException("an LZ4 block copies from before its start");
            }
            out.copy(offset, length);
        }
    }

    /* What is unpacked so far, in an array that grows. */
    private static final class Output {

        private byte[] bytes;
        private int size;

        private Output(final int first) {
            this.bytes = new byte[first];
        }

        void write(final byte[] from, final int at, final int count) {
            room(count);
            System.arraycopy(from, at, this.bytes, this.size, count);
            this.size += count;
        }

        /* Copies {@code count} bytes from {@code back} before the end, byte by byte, as they may overlap. */
        void copy(final int back, final int count) {
            room(count);
            final int from = this.size - back;
            for (int i = 0; i < count; i++) {
                this.bytes[this.size + i] = this.bytes[from + i];
            }
            this.size += count;
        }

        byte[] bytes() {
            return this.size == this.bytes.length ? this.bytes : Arrays.copyOf(this.bytes, this.size);
        }

        private void room(final int more) {
            if (this.size + more > MOST) {
                throw new ModelFormatException("an LZ4 table unpacks into more than a model may");
            }
            if (this.size + more > this.bytes.length) {
                this.bytes = Arrays.copyOf(this.bytes, Math.max(this.bytes.length * 2, this.size + more));
            }
        }
    }
}
