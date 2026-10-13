/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkstationFactsTest {

    private static final String NETWORK = "3f9c2a61-7b0e-4d25-9a4c-e81d06b7f352";

    @Test
    void groups_readTheWayTheApprovedWindowShowsThem() {
        final WorkstationFacts facts = new WorkstationFacts("player", "unix", NETWORK, "UNIX System V 3.2",
                "vel64", "CDE 2.5.2", Text.literal("Integra Servo 2620"), 2000, 8192, 612, 3072, 512_000, 1_229);
        assertEquals(List.of("Workstation", "System", "Hardware"),
                facts.groups().stream().map(group -> group.title().english()).toList());
        assertEquals(List.of("User Name=player", "Host Name=unix", "Network=" + NETWORK,
                "Operating System=UNIX System V 3.2", "Architecture=vel64", "Window System=CDE 2.5.2",
                "Processor=Integra Servo 2620, 2000 MHz", "Physical Memory=8192 MB", "Memory in Use=612 MB",
                "Video Memory=3072 MB", "System Disk=500 GB, 1.2 GB used"), rows(facts));
    }

    @Test
    void network_saysSoWhenTheMachineIsOnNone() {
        final WorkstationFacts facts =
                new WorkstationFacts("player", "debian", "", "", "", "", Text.EMPTY, 0, 0, 0, 0, 0, 0);
        assertEquals("Network=" + WorkstationFacts.NO_NETWORK.english(), rows(facts).get(2));
    }

    @Test
    void processor_readsNoneWithoutOneAndLeavesAnUnknownClockOff() {
        assertEquals("Processor=None",
                rows(new WorkstationFacts("", "", "", "", "", "", Text.EMPTY, 1000, 0, 0, 0, 0, 0)).get(6));
        assertEquals("Processor=Integra 486DX2", rows(new WorkstationFacts("", "", "", "", "", "",
                Text.literal("Integra 486DX2"), 0, 0, 0, 0, 0, 0)).get(6));
    }

    @Test
    void usedAmounts_neverReadAsMoreThanThereIs() {
        final WorkstationFacts facts =
                new WorkstationFacts("", "", "", "", "", "", Text.EMPTY, 0, 512, 900, 0, 1_024, 5_000);
        assertEquals(512, facts.memoryUsedMb());
        assertEquals(1_024, facts.diskUsedMb());
        assertEquals(1.0, facts.memoryShare());
    }

    @Test
    void memoryShare_isNoughtOnAMachineWithNoMemory() {
        assertEquals(0.0, new WorkstationFacts(null, null, null, null, null, null, null, -1, 0, 0, 0, 0, 0)
                .memoryShare());
    }

    private static List<String> rows(final WorkstationFacts facts) {
        final List<String> out = new ArrayList<>();
        for (final WorkstationFacts.Group group : facts.groups()) {
            for (final WorkstationFacts.Row row : group.rows()) {
                out.add(row.label().english() + "=" + row.value().english());
            }
        }
        return out;
    }
}
