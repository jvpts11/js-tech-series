/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/**
 * What a call costs a program beyond the one instruction that makes it: a fixed part, and parts that grow with how much
 * the call brings back or moves, so asking for one thing and asking for a hundred thousand are not the same question.
 *
 * @param fixed    what it costs before anything is counted
 * @param perRow   what every row it brings back adds
 * @param perBlock what every {@link #BLOCK_BYTES} it reads or writes adds, a part of a block counting as a block
 */
public record CallCost(int fixed, int perRow, int perBlock) {

    /** How many bytes make one block, the measure reading and writing are priced by. */
    public static final int BLOCK_BYTES = 4096;

    /** A call that costs nothing beyond its instruction. */
    public static final CallCost FREE = new CallCost(0, 0, 0);

    public CallCost {
        if (fixed < 0 || perRow < 0 || perBlock < 0) {
            throw new IllegalArgumentException(
                    "a call cannot cost less than nothing: " + fixed + ", " + perRow + ", " + perBlock);
        }
    }

    /** A call that costs the same however much it brings back. */
    public static CallCost of(final int fixed) {
        return new CallCost(fixed, 0, 0);
    }

    /** A call that costs {@code fixed} and one more for every row it brings back. */
    public static CallCost perRow(final int fixed) {
        return new CallCost(fixed, 1, 0);
    }

    /** A call that costs {@code fixed} and {@code perBlock} more for every block it reads or writes. */
    public static CallCost perBlock(final int fixed, final int perBlock) {
        return new CallCost(fixed, 0, perBlock);
    }

    /**
     * What it comes to for a call that brought back {@code rows} rows and read or wrote {@code bytes} bytes, never more
     * than the largest cost there is.
     */
    public int at(final int rows, final long bytes) {
        final long blocks = bytes <= 0 ? 0 : bytes / BLOCK_BYTES + (bytes % BLOCK_BYTES == 0 ? 0 : 1);
        final long byRows = Math.min(Integer.MAX_VALUE, (long) this.perRow * Math.max(0, rows));
        final long byBlocks = Math.min(Integer.MAX_VALUE, (long) this.perBlock * Math.min(Integer.MAX_VALUE, blocks));
        return (int) Math.min(Integer.MAX_VALUE, this.fixed + byRows + byBlocks);
    }

    /** How a tooltip says it: {@code free}, {@code 50}, {@code 50 plus one for every row it brings back}. */
    public String describe() {
        if (this.equals(FREE)) {
            return "free";
        }
        final StringBuilder said = new StringBuilder(String.valueOf(this.fixed));
        if (this.perRow > 0) {
            said.append(" plus ").append(count(this.perRow)).append(" for every row it brings back");
        }
        if (this.perBlock > 0) {
            said.append(" plus ").append(count(this.perBlock)).append(" for every ").append(BLOCK_BYTES / 1024)
                    .append(" KB it reads or writes");
        }
        return said.toString();
    }

    private static String count(final int amount) {
        return amount == 1 ? "one" : String.valueOf(amount);
    }
}
