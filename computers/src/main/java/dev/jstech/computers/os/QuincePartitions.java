/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed memory partitions of Quince's classic systems (computers/docs/design/quince.md): each program opens in a
 * block of its own and holds all of it until it quits, whatever it uses inside.
 *
 * <p>A program's Get Info sets its block: one size on the Quince System, a minimum and a preferred size on Quince OS 9,
 * beside the size its maker suggests. It opens in a block of the preferred size when that much is free, and otherwise
 * in the largest free block if that is at least its minimum; with less than that it is refused.
 *
 * <p>The open programs are weighed in the order they opened, each taking its block out of what the ones before it left,
 * so the machine and the desktop showing it work out the same blocks from the same windows.
 */
public final class QuincePartitions {

    /** The smallest block a program is allowed, in megabytes. */
    public static final int LEAST_MB = 1;

    /** The largest block a program can be given, in megabytes: more than any machine of the classic ages holds. */
    public static final int MOST_MB = 4_096;

    private QuincePartitions() {
    }

    /**
     * A program's block as its Get Info sets it.
     *
     * @param minimumMb   the least it opens in, with less than its preferred size free
     * @param preferredMb what it opens in when that much is free
     */
    public record Partition(int minimumMb, int preferredMb) {

        public Partition {
            preferredMb = Math.max(LEAST_MB, Math.min(MOST_MB, preferredMb));
            minimumMb = Math.max(LEAST_MB, Math.min(preferredMb, minimumMb));
        }

        /** The block a program its maker suggests {@code suggestedMb} for opens in until somebody changes it. */
        public static Partition suggested(final int suggestedMb) {
            final int size = Math.max(LEAST_MB, suggestedMb);
            return new Partition(Math.max(LEAST_MB, (size + 1) / 2), size);
        }

        /** One size, both the least and the most, as the Quince System's Get Info sets it. */
        public static Partition fixed(final int sizeMb) {
            return new Partition(sizeMb, sizeMb);
        }
    }

    /**
     * The block each open program holds, in the order they opened: its preferred size when that much is still free,
     * else everything still free when that is at least its minimum. A program left with less than its minimum (its Get
     * Info changed after it opened) holds what is left, never less than nothing.
     *
     * @param partitions the blocks of the open programs, in the order they opened
     * @param freeMb     what the machine has free for programs before any of them opened
     */
    public static List<Integer> weigh(final List<Partition> partitions, final int freeMb) {
        final List<Integer> blocks = new ArrayList<>(partitions.size());
        int left = Math.max(0, freeMb);
        for (final Partition partition : partitions) {
            final int block = Math.min(partition.preferredMb(), left);
            blocks.add(block);
            left -= block;
        }
        return blocks;
    }

    /**
     * The block a program would open in with {@code freeMb} free, or zero when it is refused: its preferred size, or
     * all that is free when that is at least its minimum.
     */
    public static int opening(final Partition partition, final int freeMb) {
        if (freeMb >= partition.preferredMb()) {
            return partition.preferredMb();
        }
        return freeMb >= partition.minimumMb() ? freeMb : 0;
    }
}
