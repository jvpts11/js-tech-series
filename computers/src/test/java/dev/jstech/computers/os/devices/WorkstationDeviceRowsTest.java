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
import org.junit.jupiter.api.Test;

class WorkstationDeviceRowsTest {

    @Test
    void rows_nameTheVideoTheAudioAndTheVintagePortsAsTheWorkstationDid() {
        final List<String> rows = WorkstationDeviceRows.rows(vintage()).stream()
                .map(row -> row.label().english() + " = " + row.value().english()).toList();

        assertEquals(List.of("Video = Pyrix 3D Blaster: Vintage Monitor", "Audio = Artisan Tone Blaster: free",
                "COM1 = Redstone Interface \"Well\"", "LPT1 = Floppy Drive"), rows);
    }

    @Test
    void rows_standForTheDevicesAndMarkTheDisabledAndTheFree() {
        final List<WorkstationDeviceRows.Row> rows = WorkstationDeviceRows.rows(vintage());

        assertEquals(10L, rows.get(0).pos());
        assertTrue(rows.get(1).free());
        assertFalse(rows.get(1).isDevice(), "a free output stands for nothing to disable");
        assertTrue(rows.get(2).disabled());
        assertEquals(30L, rows.get(2).pos());
    }

    @Test
    void rows_followAHubWithItsOwnPorts() {
        final DeviceMap.Device hub = new DeviceMap.Device("usb_hub", Text.literal("Legacy Hub (2 ports)"), 50L,
                false, List.of(DeviceMap.Port.free(Text.literal("Hub port 1")),
                DeviceMap.Port.free(Text.literal("Hub port 2"))));
        final DeviceMap usb = new DeviceMap("lab", Text.literal("Board"), List.of(), List.of(), List.of(), List.of(),
                List.of(), DeviceMap.PortFamily.USB, List.of(new DeviceMap.Port(Text.literal("USB 1"), List.of(hub))),
                List.of(), List.of());

        final List<String> rows = WorkstationDeviceRows.rows(usb).stream()
                .map(row -> row.label().english() + " = " + row.value().english()).toList();

        assertEquals(List.of("USB 1 = Legacy Hub (2 ports)", "Hub port 1 = free", "Hub port 2 = free"), rows);
    }

    /* A Vintage workstation: a card with its monitor, a sound card with nothing on it, COM1 and LPT1 both taken. */
    private static DeviceMap vintage() {
        return new DeviceMap("lab", Text.literal("MF AT Vintage Motherboard"), List.of(), List.of(), List.of(),
                List.of(new DeviceMap.VideoCard(Text.literal("Pyrix 3D Blaster"), List.of(new DeviceMap.Port(
                        Text.literal("Output 1"), List.of(device("Vintage Monitor", 10L, false)))))),
                List.of(new DeviceMap.AudioSource(Text.literal("Artisan Tone Blaster"),
                        List.of(DeviceMap.Port.free(Text.literal("Audio output"))))),
                DeviceMap.PortFamily.SERIAL_PARALLEL, List.of(
                        new DeviceMap.Port(Text.literal("Serial (COM1)"),
                                List.of(device("Redstone Interface \"Well\"", 30L, true))),
                        new DeviceMap.Port(Text.literal("Parallel (LPT1)"),
                                List.of(device("Floppy Drive", 40L, false)))), List.of(), List.of());
    }

    private static DeviceMap.Device device(final String name, final long pos, final boolean disabled) {
        return new DeviceMap.Device("", Text.literal(name), pos, disabled, List.of());
    }
}
