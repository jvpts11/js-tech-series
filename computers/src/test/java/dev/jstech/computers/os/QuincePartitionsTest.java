/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuincePartitionsTest {

    @Test
    void weigh_givesEachProgramItsPreferredBlockWhileThatMuchIsFree() {
        final List<Integer> blocks = QuincePartitions.weigh(List.of(new QuincePartitions.Partition(4, 8),
                new QuincePartitions.Partition(2, 16)), 64);
        assertEquals(List.of(8, 16), blocks);
    }

    @Test
    void weigh_givesTheLaterProgramWhatIsLeftWhenItsPreferredBlockIsNotFree() {
        final List<Integer> blocks = QuincePartitions.weigh(List.of(new QuincePartitions.Partition(4, 24),
                new QuincePartitions.Partition(4, 16)), 32);
        assertEquals(List.of(24, 8), blocks);
    }

    @Test
    void weigh_neverGivesMoreThanIsFree() {
        final List<Integer> blocks = QuincePartitions.weigh(List.of(new QuincePartitions.Partition(4, 24),
                new QuincePartitions.Partition(4, 16)), 20);
        assertEquals(List.of(20, 0), blocks);
    }

    @Test
    void opening_takesThePreferredSizeThenTheFreeBlockAboveTheMinimumThenRefuses() {
        final QuincePartitions.Partition block = new QuincePartitions.Partition(8, 16);
        assertEquals(16, QuincePartitions.opening(block, 40));
        assertEquals(10, QuincePartitions.opening(block, 10));
        assertEquals(0, QuincePartitions.opening(block, 7));
    }

    @Test
    void partition_keepsTheMinimumAtMostThePreferredAndBothAtLeastOne() {
        final QuincePartitions.Partition block = new QuincePartitions.Partition(40, 12);
        assertEquals(12, block.minimumMb());
        assertEquals(1, new QuincePartitions.Partition(0, 0).preferredMb());
    }

    @Test
    void suggested_halvesTheSuggestedSizeForTheMinimum() {
        final QuincePartitions.Partition block = QuincePartitions.Partition.suggested(16);
        assertEquals(8, block.minimumMb());
        assertEquals(16, block.preferredMb());
        assertEquals(new QuincePartitions.Partition(32, 32), QuincePartitions.Partition.fixed(32));
    }
}
