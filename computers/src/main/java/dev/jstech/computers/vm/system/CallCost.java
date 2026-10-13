/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a call costs a program beyond the one instruction that makes it: a fixed part, and parts that grow with how much
 * the call brings back or moves, so asking for one thing and asking for a hundred thousand are not the same question.
 *
 * @param fixed    what it costs before anything is counted
 * @param perRow   what every row it brings back adds
 * @param perBlock what every {@link #BLOCK_BYTES} it reads or writes adds, a part of a block counting as a block
 */
@TextHolder
public record CallCost(int fixed, int perRow, int perBlock) {

    /** How many bytes make one block, the measure reading and writing are priced by. */
    public static final int BLOCK_BYTES = 4096;

    /** A call that costs nothing beyond its instruction. */
    public static final CallCost FREE = new CallCost(0, 0, 0);

    private static final TextKey COSTS_NOTHING = TextKey.of("jsc.vm.call_cost.free", "free");
    private static final TextKey PLUS_PER_ROW = TextKey.of("jsc.vm.call_cost.plus_per_row",
            "%s plus %s for every row it brings back");
    private static final TextKey PLUS_PER_BLOCK = TextKey.of("jsc.vm.call_cost.plus_per_block",
            "%s plus %s for every %s KB it reads or writes");
    private static final TextKey ONE = TextKey.of("jsc.vm.call_cost.one", "one");

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

    /**
     * How a tooltip says it, in its reader's language: {@code free}, {@code 50},
     * {@code 50 plus one for every row it brings back}.
     */
    public Text text() {
        if (this.equals(FREE)) {
            return COSTS_NOTHING.text();
        }
        Text said = Text.literal(String.valueOf(this.fixed));
        if (this.perRow > 0) {
            said = PLUS_PER_ROW.with(said, count(this.perRow));
        }
        if (this.perBlock > 0) {
            said = PLUS_PER_BLOCK.with(said, count(this.perBlock), BLOCK_BYTES / 1024);
        }
        return said;
    }

    /** The same in English, the language the machine keeps what it writes down in. */
    public String describe() {
        return this.text().english();
    }

    private static Text count(final int amount) {
        return amount == 1 ? ONE.text() : Text.literal(String.valueOf(amount));
    }
}
