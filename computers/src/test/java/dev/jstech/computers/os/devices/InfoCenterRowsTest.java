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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InfoCenterRowsTest {

    @Test
    void rows_listEachGraphicsCardTheAudioAndThePortsWithWhatIsOnThem() {
        final List<String> rows = InfoCenterRows.rows(map()).stream()
                .map(row -> row.port().english() + " | " + row.device().english()).toList();

        assertEquals(List.of("Pyrix Radiance HD 7970 | ", "Output 1 | Monitor \"Left\"", "Output 2 | free",
                "Audio | ", "Audio on the board | Artisan Cobble \"Desk\"", "USB | ", "USB 1 | DVD Drive",
                "USB 2 | USB 3 Hub (7 ports)", "on the hub | 1 of 7 in use", "USB 3 | free"), rows);
    }

    @Test
    void rows_markTheKindsTheFreePortsAndTheDisabledDevices() {
        final List<InfoCenterRows.Row> rows = InfoCenterRows.rows(map());

        assertEquals(InfoCenterRows.Kind.HEADING, rows.getFirst().kind());
        assertTrue(rows.get(2).free());
        assertFalse(rows.get(2).isDevice(), "a free port stands for nothing to disable");
        assertTrue(rows.get(6).disabled(), "the DVD drive is disabled");
        assertEquals(30L, rows.get(6).pos());
        assertEquals("dvd_drive", rows.get(6).deviceIcon());
        assertEquals("usb3_port", rows.get(6).icon());
        assertEquals(InfoCenterRows.Kind.HUB_COUNT, rows.get(8).kind());
        assertFalse(rows.get(8).isDevice());
    }

    @Test
    void rows_nameAVintageBoardsPortsAsSerialAndParallel() {
        final DeviceMap vintage = new DeviceMap("lab", Text.literal("Board"), List.of(), List.of(), List.of(),
                List.of(), List.of(), DeviceMap.PortFamily.SERIAL_PARALLEL,
                List.of(DeviceMap.Port.free(Text.literal("Serial (COM1)")),
                        DeviceMap.Port.free(Text.literal("Parallel (LPT1)"))), List.of(), List.of());

        final List<InfoCenterRows.Row> rows = InfoCenterRows.rows(vintage);

        assertEquals("Serial and parallel", rows.getFirst().port().english());
        assertEquals("parallel_port", rows.get(2).icon());
    }

    /* A Standard machine: a graphics card with two outputs, the board's audio, three USB ports and a hub. */
    private static DeviceMap map() {
        final DeviceMap.Device left = device("monitor", "Monitor \"Left\"", 10L, false, List.of());
        final DeviceMap.Device desk = device("speaker", "Artisan Cobble \"Desk\"", 20L, false, List.of());
        final DeviceMap.Device dvd = device("dvd_drive", "DVD Drive", 30L, true, List.of());
        final List<DeviceMap.Port> hubPorts = new ArrayList<>();
        hubPorts.add(new DeviceMap.Port(Text.literal("Hub port 1"),
                List.of(device("redstone_interface", "Redstone Interface \"Lamps\"", 50L, false, List.of()))));
        for (int i = 2; i <= 7; i++) {
            hubPorts.add(DeviceMap.Port.free(Text.literal("Hub port " + i)));
        }
        final DeviceMap.Device hub = device("usb_hub", "USB 3 Hub (7 ports)", 40L, false, hubPorts);
        return new DeviceMap("studio", Text.literal("MF ATX Standard Motherboard"), List.of(), List.of(), List.of(),
                List.of(new DeviceMap.VideoCard(Text.literal("Pyrix Radiance HD 7970"), List.of(
                        new DeviceMap.Port(Text.literal("Output 1"), List.of(left)),
                        DeviceMap.Port.free(Text.literal("Output 2"))))),
                List.of(new DeviceMap.AudioSource(Text.literal("Audio on the board"), List.of(
                        new DeviceMap.Port(Text.literal("Audio output"), List.of(desk))))),
                DeviceMap.PortFamily.USB3, List.of(
                        new DeviceMap.Port(Text.literal("USB 1"), List.of(dvd)),
                        new DeviceMap.Port(Text.literal("USB 2"), List.of(hub)),
                        DeviceMap.Port.free(Text.literal("USB 3"))), List.of(), List.of());
    }

    private static DeviceMap.Device device(final String icon, final String name, final long pos,
                                           final boolean disabled, final List<DeviceMap.Port> ports) {
        return new DeviceMap.Device(icon, Text.literal(name), pos, disabled, ports);
    }
}
