/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class QueueSpeedsTest {

    @Test
    void cardQueue_runsNoFasterThanTheCard() {
        // A card that says nothing of its design runs a gigahertz at the P6's worth: its power is its cores.
        assertEquals(500L, QueueSpeeds.cardQueue(1000L, card(500, PcieGeneration.PCIE_3_0), PcieGeneration.PCIE_3_0));
    }

    @Test
    void cardQueue_runsNoFasterThanTheProcessor() {
        assertEquals(300L, QueueSpeeds.cardQueue(300L, card(500, PcieGeneration.PCIE_3_0), PcieGeneration.PCIE_3_0));
    }

    @Test
    void cardQueue_isCutByAnOlderSlot() {
        assertEquals(400L, QueueSpeeds.cardQueue(1000L, card(800, PcieGeneration.PCIE_3_0), PcieGeneration.PCIE_2_0),
                "one generation older halves the card");
    }

    @Test
    void cardQueue_neverStops() {
        assertEquals(1L, QueueSpeeds.cardQueue(0L, card(1, PcieGeneration.PCIE_3_0), PcieGeneration.PCIE_3_0));
    }

    @Test
    void heldByCard_isTrueOnlyForACardBelowTheProcessor() {
        final long[] speeds = {480L, 480L, 210L};
        assertFalse(QueueSpeeds.heldByCard(speeds, 0), "the processor's own queue is never held by a card");
        assertFalse(QueueSpeeds.heldByCard(speeds, 1), "a card at full speed holds nothing");
        assertTrue(QueueSpeeds.heldByCard(speeds, 2), "a weak card holds its queue");
        assertFalse(QueueSpeeds.heldByCard(speeds, 3), "a queue that is not there is held by nothing");
    }

    private static GpuSpec card(final int cores, final PcieGeneration bus) {
        return new GpuSpec(HardwareEra.STANDARD, bus, cores, 2048, 150);
    }
}
