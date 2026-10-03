/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeviceRowsTest {

    @Test
    void byPort_listsEveryPortByKindWithWhatIsPluggedAndTheFreeOnes() {
        final List<String> rows = labels(DeviceRows.rows(map(), DeviceRows.View.BY_PORT, Set.of()));

        assertEquals(List.of("office-pc", "Video outputs", "Visara Vertex 8800 GT", "Output 1: Legacy Monitor",
                "Output 2: free", "Audio outputs", "Audio on the board: Artisan ToneWorks, Artisan ToneWorks",
                "Device ports", "USB 1: CD Drive", "USB 2: Legacy Hub (4 ports)", "Hub port 1: Floppy Drive",
                "Hub port 2: free", "Hub port 3: free", "Hub port 4: free", "USB 3: free", "USB 4: free",
                "3 of 8 device ports in use (4 on the board, 4 on the hub)"), rows);
    }

    @Test
    void byPort_marksTheFreePortsAndTheDisabledDevices() {
        final List<DeviceRows.Row> rows = DeviceRows.rows(map(), DeviceRows.View.BY_PORT, Set.of());

        assertEquals(DeviceRows.State.FREE, row(rows, "Output 2: free").state());
        assertEquals(DeviceRows.State.DISABLED, row(rows, "USB 1: CD Drive").state());
        assertEquals(2L, row(rows, "USB 1: CD Drive").pos());
        assertFalse(row(rows, "Output 2: free").isDevice());
        assertEquals(DeviceRows.State.SUMMARY, rows.getLast().state());
    }

    @Test
    void foldedBranch_keepsItsRowsOut() {
        final List<DeviceRows.Row> open = DeviceRows.rows(map(), DeviceRows.View.BY_PORT, Set.of());
        final String hubKey = row(open, "USB 2: Legacy Hub (4 ports)").key();

        final List<String> folded = labels(DeviceRows.rows(map(), DeviceRows.View.BY_PORT, Set.of(hubKey)));

        assertTrue(folded.contains("USB 2: Legacy Hub (4 ports)"));
        assertFalse(folded.contains("Hub port 1: Floppy Drive"));
        assertTrue(row(open, "USB 2: Legacy Hub (4 ports)").expandable());
    }

    @Test
    void byType_listsThePartsByWhatTheyAre() {
        final List<String> rows = labels(DeviceRows.rows(map(), DeviceRows.View.BY_TYPE,
                Set.of(DeviceRows.MEMORY_KEY)));

        assertEquals(List.of("office-pc", "Computer", "MF ATX Legacy Motherboard", "Processors", "Integra Duo E6600",
                "Memory", "Disk drives", "Vaultis Link SATA-SSD 64G", "Display adapters", "Visara Vertex 8800 GT",
                "Sound devices", "Audio on the board", "Network adapters", "Ethernet port (on the board)"), rows);
    }

    @Test
    void byConnection_hangsEachDeviceFromWhatItIsPluggedInto() {
        final List<DeviceRows.Row> rows = DeviceRows.rows(map(), DeviceRows.View.BY_CONNECTION, Set.of());

        assertEquals(3, row(rows, "Legacy Monitor").depth());
        assertEquals(2, row(rows, "USB controller").depth());
        assertEquals(3, row(rows, "Legacy Hub (4 ports)").depth());
        assertEquals(4, row(rows, "Floppy Drive").depth());
        assertFalse(labels(rows).contains("Hub port 2: free"));
    }

    private static DeviceRows.Row row(final List<DeviceRows.Row> rows, final String label) {
        return rows.stream().filter(r -> r.label().english().equals(label)).findFirst().orElseThrow();
    }

    private static List<String> labels(final List<DeviceRows.Row> rows) {
        return rows.stream().map(r -> r.label().english()).toList();
    }

    /* A Legacy machine: a monitor on its card, a pair of speakers, a disabled CD drive and a hub with a floppy. */
    private static DeviceMap map() {
        final DeviceMap.Device monitor = device("legacy_monitor", "Legacy Monitor", 1L, false, List.of());
        final DeviceMap.Device cd = device("cd_drive", "CD Drive", 2L, true, List.of());
        final DeviceMap.Device floppy = device("floppy_drive", "Floppy Drive", 4L, false, List.of());
        final DeviceMap.Device left = device("legacy_speaker", "Artisan ToneWorks", 5L, false, List.of());
        final DeviceMap.Device right = device("legacy_speaker", "Artisan ToneWorks", 6L, false, List.of());
        final DeviceMap.Device hub = device("usb_hub", "Legacy Hub (4 ports)", 3L, false, List.of(
                new DeviceMap.Port(DeviceMap.hubPortName(1), List.of(floppy)),
                DeviceMap.Port.free(DeviceMap.hubPortName(2)), DeviceMap.Port.free(DeviceMap.hubPortName(3)),
                DeviceMap.Port.free(DeviceMap.hubPortName(4))));
        final DeviceMap.PortFamily usb = DeviceMap.PortFamily.USB;
        return new DeviceMap("office-pc", Text.literal("MF ATX Legacy Motherboard"),
                List.of(Text.literal("Integra Duo E6600")), List.of(Text.literal("Stratix Layer DDR2-2048")),
                List.of(Text.literal("Vaultis Link SATA-SSD 64G")),
                List.of(new DeviceMap.VideoCard(Text.literal("Visara Vertex 8800 GT"), List.of(
                        new DeviceMap.Port(DeviceMap.outputName(1), List.of(monitor)),
                        DeviceMap.Port.free(DeviceMap.outputName(2))))),
                List.of(new DeviceMap.AudioSource(DeviceMap.boardAudio(),
                        List.of(new DeviceMap.Port(DeviceMap.boardAudio(), List.of(left, right))))),
                usb, List.of(new DeviceMap.Port(usb.portName(0), List.of(cd)),
                        new DeviceMap.Port(usb.portName(1), List.of(hub)),
                        DeviceMap.Port.free(usb.portName(2)), DeviceMap.Port.free(usb.portName(3))),
                List.of(DeviceMap.boardNetwork(Text.literal("Ethernet"))), List.of());
    }

    private static DeviceMap.Device device(final String icon, final String name, final long pos,
                                           final boolean disabled, final List<DeviceMap.Port> ports) {
        return new DeviceMap.Device(icon, Text.literal(name), pos, disabled, ports);
    }
}
