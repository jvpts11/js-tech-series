/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import java.nio.charset.StandardCharsets;

/**
 * A QR code made from a line of text, the way ISO/IEC 18004 lays one out, so a phone pointed at the screen reads it.
 *
 * <p>It does one thing: the text goes in as bytes (byte mode), at the medium level of error correction (M), in the
 * smallest of the first ten versions that holds it, under whichever of the eight masks the standard's penalty rules
 * score lowest. What comes out is the grid of modules and nothing else: the code carries exactly the text it was
 * given, and nobody is sent anywhere by reading it.
 */
public final class QrCode {

    /** The most text this makes a code of: what version 10 at level M holds in bytes. */
    public static final int MOST_BYTES = 213;

    /* Byte mode's indicator, and level M's two bits in the format information. */
    private static final int BYTE_MODE = 0b0100;
    private static final int LEVEL_M = 0b00;
    /* The polynomial of the field the error correction works in, and the two BCH generators and the format mask. */
    private static final int FIELD = 0x11D;
    private static final int FORMAT_GENERATOR = 0x537;
    private static final int FORMAT_MASK = 0x5412;
    private static final int VERSION_GENERATOR = 0x1F25;
    /*
     * Level M for versions 1 to 10, one row each: error correction codewords per block, then the blocks of the first
     * group and the data codewords in each, then the same for the second group.
     */
    private static final int[][] BLOCKS = {
            {10, 1, 16, 0, 0}, {16, 1, 28, 0, 0}, {26, 1, 44, 0, 0}, {18, 2, 32, 0, 0}, {24, 2, 43, 0, 0},
            {16, 4, 27, 0, 0}, {18, 4, 31, 0, 0}, {22, 2, 38, 2, 39}, {22, 3, 36, 2, 37}, {26, 4, 43, 1, 44}};
    /* Where the alignment patterns' centres stand, on both axes, for versions 2 to 10. */
    private static final int[][] ALIGNMENT = {
            {}, {6, 18}, {6, 22}, {6, 26}, {6, 30}, {6, 34}, {6, 22, 38}, {6, 24, 42}, {6, 26, 46}, {6, 28, 50}};
    /* The penalty rules' weights: a run, a block of four, a finder-like row, a step off half dark. */
    private static final int RUN_PENALTY = 3;
    private static final int BLOCK_PENALTY = 3;
    private static final int FINDER_PENALTY = 40;
    private static final int BALANCE_PENALTY = 10;

    private QrCode() {
    }

    /**
     * The modules of the code for {@code text}, rows first, {@code true} for a dark module; the quiet zone around it
     * is left to whoever draws it.
     *
     * @throws IllegalArgumentException when the text is longer than {@link #MOST_BYTES} bytes
     */
    public static boolean[][] encode(final String text) {
        boolean[][] best = null;
        int lowest = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            final boolean[][] candidate = encode(text, mask);
            final int score = penalty(candidate);
            if (score < lowest) {
                lowest = score;
                best = candidate;
            }
        }
        return best;
    }

    /** The same, under the mask {@code mask} (0 to 7) rather than the one the penalty rules would pick. */
    public static boolean[][] encode(final String text, final int mask) {
        if (mask < 0 || mask > 7) {
            throw new IllegalArgumentException("a mask is 0 to 7; got " + mask);
        }
        final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        final int version = versionFor(bytes.length);
        final int size = version * 4 + 17;
        final boolean[][] dark = new boolean[size][size];
        final boolean[][] function = new boolean[size][size];
        drawFunctionPatterns(dark, function, version);
        placeData(dark, function, codewords(bytes, version));
        applyMask(dark, function, mask);
        drawFormat(dark, function, mask);
        return dark;
    }

    /** The smallest version that holds {@code length} bytes. */
    static int versionFor(final int length) {
        for (int version = 1; version <= BLOCKS.length; version++) {
            final int countBits = version < 10 ? 8 : 16;
            if (4 + countBits + length * 8 <= dataCodewords(version) * 8) {
                return version;
            }
        }
        throw new IllegalArgumentException("a code of version 10 holds at most " + MOST_BYTES + " bytes; got "
                + length);
    }

    /** The score the standard's four penalty rules give a finished code; the lowest is the one to show. */
    static int penalty(final boolean[][] dark) {
        final int size = dark.length;
        int score = 0;
        for (int i = 0; i < size; i++) {
            score += linePenalty(dark, i, true) + linePenalty(dark, i, false);
        }
        for (int y = 0; y < size - 1; y++) {
            for (int x = 0; x < size - 1; x++) {
                final boolean c = dark[y][x];
                if (c == dark[y][x + 1] && c == dark[y + 1][x] && c == dark[y + 1][x + 1]) {
                    score += BLOCK_PENALTY;
                }
            }
        }
        int darkCount = 0;
        for (final boolean[] row : dark) {
            for (final boolean module : row) {
                darkCount += module ? 1 : 0;
            }
        }
        final int total = size * size;
        // How many steps of five per cent the dark share stands from half, rounded down.
        final int steps = Math.abs(darkCount * 20 - total * 10) / total;
        return score + steps * BALANCE_PENALTY;
    }

    private static int dataCodewords(final int version) {
        final int[] row = BLOCKS[version - 1];
        return row[1] * row[2] + row[3] * row[4];
    }

    /* The data and its error correction, interleaved block by block as they are read off the grid. */
    private static int[] codewords(final byte[] bytes, final int version) {
        final int capacity = dataCodewords(version);
        final BitBuffer bits = new BitBuffer();
        bits.append(BYTE_MODE, 4);
        bits.append(bytes.length, version < 10 ? 8 : 16);
        for (final byte b : bytes) {
            bits.append(b & 0xFF, 8);
        }
        bits.append(0, Math.min(4, capacity * 8 - bits.length()));
        bits.append(0, (8 - bits.length() % 8) % 8);
        final int[] data = new int[capacity];
        final int filled = bits.length() / 8;
        for (int i = 0; i < filled; i++) {
            data[i] = bits.byteAt(i);
        }
        // The rest is filled with the two pad bytes in turn.
        for (int i = filled; i < capacity; i++) {
            data[i] = (i - filled) % 2 == 0 ? 0xEC : 0x11;
        }
        final int[] row = BLOCKS[version - 1];
        final int eccLength = row[0];
        final int blocks = row[1] + row[3];
        final int[][] dataBlocks = new int[blocks][];
        final int[][] eccBlocks = new int[blocks][];
        final int[] divisor = divisor(eccLength);
        int at = 0;
        for (int b = 0; b < blocks; b++) {
            final int length = b < row[1] ? row[2] : row[4];
            dataBlocks[b] = new int[length];
            System.arraycopy(data, at, dataBlocks[b], 0, length);
            at += length;
            eccBlocks[b] = remainder(dataBlocks[b], divisor);
        }
        final int[] out = new int[capacity + blocks * eccLength];
        int k = 0;
        final int longest = Math.max(row[2], row[4]);
        for (int i = 0; i < longest; i++) {
            for (final int[] block : dataBlocks) {
                if (i < block.length) {
                    out[k++] = block[i];
                }
            }
        }
        for (int i = 0; i < eccLength; i++) {
            for (final int[] block : eccBlocks) {
                out[k++] = block[i];
            }
        }
        return out;
    }

    /* The Reed-Solomon generator polynomial of that degree, its leading term left out. */
    private static int[] divisor(final int degree) {
        final int[] result = new int[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = multiply(result[j], root);
                if (j + 1 < degree) {
                    result[j] ^= result[j + 1];
                }
            }
            root = multiply(root, 0x02);
        }
        return result;
    }

    /* What is left of the block divided by the generator: its error correction codewords. */
    private static int[] remainder(final int[] block, final int[] divisor) {
        final int[] result = new int[divisor.length];
        for (final int codeword : block) {
            final int factor = codeword ^ result[0];
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) {
                result[i] ^= multiply(divisor[i], factor);
            }
        }
        return result;
    }

    /* Multiplication in the field of 256 elements the standard's error correction is worked in. */
    private static int multiply(final int x, final int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * FIELD);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    private static void drawFunctionPatterns(final boolean[][] dark, final boolean[][] function, final int version) {
        final int size = dark.length;
        for (int i = 0; i < size; i++) {
            set(dark, function, 6, i, i % 2 == 0);
            set(dark, function, i, 6, i % 2 == 0);
        }
        finder(dark, function, 3, 3);
        finder(dark, function, size - 4, 3);
        finder(dark, function, 3, size - 4);
        final int[] centres = ALIGNMENT[version - 1];
        for (int i = 0; i < centres.length; i++) {
            for (int j = 0; j < centres.length; j++) {
                // The three corners the finders stand in take no alignment pattern.
                final boolean corner = i == 0 && j == 0 || i == 0 && j == centres.length - 1
                        || i == centres.length - 1 && j == 0;
                if (!corner) {
                    alignment(dark, function, centres[i], centres[j]);
                }
            }
        }
        // The format areas are reserved now and written once the mask is known; the dark module goes in with them.
        drawFormat(dark, function, 0);
        if (version >= 7) {
            drawVersion(dark, function, version);
        }
    }

    /* A finder pattern centred there, with the light separator around it, cut where it meets the edge. */
    private static void finder(final boolean[][] dark, final boolean[][] function, final int cx, final int cy) {
        final int size = dark.length;
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                final int x = cx + dx;
                final int y = cy + dy;
                if (x >= 0 && x < size && y >= 0 && y < size) {
                    final int distance = Math.max(Math.abs(dx), Math.abs(dy));
                    set(dark, function, x, y, distance != 2 && distance != 4);
                }
            }
        }
    }

    private static void alignment(final boolean[][] dark, final boolean[][] function, final int cx, final int cy) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                set(dark, function, cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    /* The format information, level and mask under its BCH code, in both of its places, and the dark module. */
    private static void drawFormat(final boolean[][] dark, final boolean[][] function, final int mask) {
        final int size = dark.length;
        final int data = LEVEL_M << 3 | mask;
        int rem = data;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ ((rem >>> 9) * FORMAT_GENERATOR);
        }
        final int bits = (data << 10 | rem) ^ FORMAT_MASK;
        for (int i = 0; i <= 5; i++) {
            set(dark, function, 8, i, bit(bits, i));
        }
        set(dark, function, 8, 7, bit(bits, 6));
        set(dark, function, 8, 8, bit(bits, 7));
        set(dark, function, 7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) {
            set(dark, function, 14 - i, 8, bit(bits, i));
        }
        for (int i = 0; i < 8; i++) {
            set(dark, function, size - 1 - i, 8, bit(bits, i));
        }
        for (int i = 8; i < 15; i++) {
            set(dark, function, 8, size - 15 + i, bit(bits, i));
        }
        set(dark, function, 8, size - 8, true);
    }

    /* The version information of versions 7 and up, under its own BCH code, beside two of the finders. */
    private static void drawVersion(final boolean[][] dark, final boolean[][] function, final int version) {
        final int size = dark.length;
        int rem = version;
        for (int i = 0; i < 12; i++) {
            rem = (rem << 1) ^ ((rem >>> 11) * VERSION_GENERATOR);
        }
        final int bits = version << 12 | rem;
        for (int i = 0; i < 18; i++) {
            final boolean on = bit(bits, i);
            final int a = size - 11 + i % 3;
            final int b = i / 3;
            set(dark, function, a, b, on);
            set(dark, function, b, a, on);
        }
    }

    /* The codewords' bits, laid in the two-column zigzag from the bottom right corner, around everything reserved. */
    private static void placeData(final boolean[][] dark, final boolean[][] function, final int[] codewords) {
        final int size = dark.length;
        int i = 0;
        int right = size - 1;
        while (right >= 1) {
            // The vertical timing pattern takes column 6 whole, so the pair to its left starts one further in.
            if (right == 6) {
                right = 5;
            }
            final boolean upward = ((right + 1) & 2) == 0;
            for (int vertical = 0; vertical < size; vertical++) {
                final int y = upward ? size - 1 - vertical : vertical;
                for (int j = 0; j < 2; j++) {
                    final int x = right - j;
                    if (!function[y][x] && i < codewords.length * 8) {
                        dark[y][x] = bit(codewords[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
            right -= 2;
        }
    }

    private static void applyMask(final boolean[][] dark, final boolean[][] function, final int mask) {
        final int size = dark.length;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!function[y][x] && masked(mask, x, y)) {
                    dark[y][x] = !dark[y][x];
                }
            }
        }
    }

    private static boolean masked(final int mask, final int x, final int y) {
        return switch (mask) {
            case 0 -> (x + y) % 2 == 0;
            case 1 -> y % 2 == 0;
            case 2 -> x % 3 == 0;
            case 3 -> (x + y) % 3 == 0;
            case 4 -> (x / 3 + y / 2) % 2 == 0;
            case 5 -> x * y % 2 + x * y % 3 == 0;
            case 6 -> (x * y % 2 + x * y % 3) % 2 == 0;
            default -> ((x + y) % 2 + x * y % 3) % 2 == 0;
        };
    }

    /* Rule 1 (runs of five or more) and rule 3 (the finder-like 1:1:3:1:1 with four light beside it) on one line. */
    private static int linePenalty(final boolean[][] dark, final int index, final boolean row) {
        final int size = dark.length;
        int score = 0;
        int run = 1;
        for (int i = 1; i <= size; i++) {
            if (i < size && module(dark, index, i, row) == module(dark, index, i - 1, row)) {
                run++;
                continue;
            }
            if (run >= 5) {
                score += RUN_PENALTY + run - 5;
            }
            run = 1;
        }
        for (int i = 0; i + 6 < size; i++) {
            if (module(dark, index, i, row) && !module(dark, index, i + 1, row) && module(dark, index, i + 2, row)
                    && module(dark, index, i + 3, row) && module(dark, index, i + 4, row)
                    && !module(dark, index, i + 5, row) && module(dark, index, i + 6, row)
                    && (light(dark, index, i - 4, i - 1, row) || light(dark, index, i + 7, i + 10, row))) {
                score += FINDER_PENALTY;
            }
        }
        return score;
    }

    /* Whether the modules from {@code from} to {@code to} are all light, the space past the edge counting as light. */
    private static boolean light(final boolean[][] dark, final int index, final int from, final int to,
                                 final boolean row) {
        for (int i = from; i <= to; i++) {
            if (i >= 0 && i < dark.length && module(dark, index, i, row)) {
                return false;
            }
        }
        return true;
    }

    private static boolean module(final boolean[][] dark, final int index, final int i, final boolean row) {
        return row ? dark[index][i] : dark[i][index];
    }

    private static void set(final boolean[][] dark, final boolean[][] function, final int x, final int y,
                            final boolean on) {
        dark[y][x] = on;
        function[y][x] = true;
    }

    private static boolean bit(final int value, final int index) {
        return ((value >>> index) & 1) != 0;
    }

    /** The bits of the data as they are put together, most significant first. */
    private static final class BitBuffer {

        private final StringBuilder bits = new StringBuilder();

        void append(final int value, final int count) {
            for (int i = count - 1; i >= 0; i--) {
                bits.append((value >>> i & 1) == 1 ? '1' : '0');
            }
        }

        int length() {
            return bits.length();
        }

        int byteAt(final int index) {
            return Integer.parseInt(bits.substring(index * 8, index * 8 + 8), 2);
        }
    }
}
