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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeviceMapTest {

    @Test
    void portName_isUsbCountedFromOne() {
        assertEquals("USB 1", DeviceMap.PortFamily.USB.portName(0).english());
        assertEquals("USB 4", DeviceMap.PortFamily.USB3.portName(3).english());
    }

    @Test
    void portName_onAVintageBoardIsASerialAndAParallelPortInTurn() {
        assertEquals("Serial (COM1)", DeviceMap.PortFamily.SERIAL_PARALLEL.portName(0).english());
        assertEquals("Parallel (LPT1)", DeviceMap.PortFamily.SERIAL_PARALLEL.portName(1).english());
        assertEquals("Serial (COM2)", DeviceMap.PortFamily.SERIAL_PARALLEL.portName(2).english());
        assertEquals("parallel_port", DeviceMap.PortFamily.SERIAL_PARALLEL.icon(1));
        assertEquals("serial_port", DeviceMap.PortFamily.SERIAL_PARALLEL.icon(2));
    }

    @Test
    void named_addsTheNameAPlayerGave() {
        assertEquals("Redstone Interface \"Door\"",
                DeviceMap.named(Text.literal("Redstone Interface"), "Door").english());
        assertEquals("CD Drive", DeviceMap.named(Text.literal("CD Drive"), "").english());
    }

    @Test
    void devicePorts_countTheBoardsAndTheHubsInUse() {
        final DeviceMap map = mapWithAHub();

        assertEquals(4 + 4, map.devicePortsTotal());
        assertEquals(4, map.hubPorts());
        // The CD drive, the hub itself, and the floppy drive on the hub's first port.
        assertEquals(3, map.devicePortsInUse());
    }

    @Test
    void devices_listEveryDeviceHubsAndWhatHangsFromThemIncluded() {
        final DeviceMap map = mapWithAHub();

        assertEquals(List.of(1L, 2L, 3L, 4L), map.devices().stream().map(DeviceMap.Device::pos).toList());
        assertTrue(map.device(3L).isHub());
        assertEquals("Floppy Drive", map.device(4L).name().english());
        assertNull(map.device(99L));
    }

    @Test
    void port_isFreeWithNothingPlugged() {
        assertTrue(DeviceMap.Port.free(Text.literal("USB 2")).isFree());
        assertFalse(mapWithAHub().devicePorts().getFirst().isFree());
    }

    @Test
    void iconOf_givesOneIconToEveryEraOfAHubAnInterfaceAndAnEncoder() {
        assertEquals("usb_hub", DeviceMap.iconOf("transition_hub"));
        assertEquals("redstone_interface", DeviceMap.iconOf("vintage_redstone_interface"));
        assertEquals("pattern_encoder", DeviceMap.iconOf("advanced_pattern_encoder"));
        assertEquals("legacy_monitor", DeviceMap.iconOf("legacy_monitor"));
        assertEquals("cd_drive", DeviceMap.iconOf("cd_drive"));
    }

    /* A Legacy machine: a monitor, a CD drive and a four-port hub with a floppy drive on its first port. */
    private static DeviceMap mapWithAHub() {
        final DeviceMap.Device monitor = device("legacy_monitor", "Legacy Monitor", 1L, List.of());
        final DeviceMap.Device cd = device("cd_drive", "CD Drive", 2L, List.of());
        final DeviceMap.Device floppy = device("floppy_drive", "Floppy Drive", 4L, List.of());
        final DeviceMap.Device hub = device("usb_hub", "Legacy Hub (4 ports)", 3L, List.of(
                new DeviceMap.Port(DeviceMap.hubPortName(1), List.of(floppy)),
                DeviceMap.Port.free(DeviceMap.hubPortName(2)), DeviceMap.Port.free(DeviceMap.hubPortName(3)),
                DeviceMap.Port.free(DeviceMap.hubPortName(4))));
        final List<DeviceMap.Port> ports = List.of(
                new DeviceMap.Port(DeviceMap.PortFamily.USB.portName(0), List.of(cd)),
                new DeviceMap.Port(DeviceMap.PortFamily.USB.portName(1), List.of(hub)),
                DeviceMap.Port.free(DeviceMap.PortFamily.USB.portName(2)),
                DeviceMap.Port.free(DeviceMap.PortFamily.USB.portName(3)));
        return new DeviceMap("office-pc", Text.literal("MF ATX Legacy Motherboard"), List.of(), List.of(), List.of(),
                List.of(new DeviceMap.VideoCard(Text.literal("Visara Vertex 8800 GT"), List.of(
                        new DeviceMap.Port(DeviceMap.outputName(1), List.of(monitor)),
                        DeviceMap.Port.free(DeviceMap.outputName(2))))),
                List.of(), DeviceMap.PortFamily.USB, ports, List.of(), List.of());
    }

    private static DeviceMap.Device device(final String icon, final String name, final long pos,
                                           final List<DeviceMap.Port> ports) {
        return new DeviceMap.Device(icon, Text.literal(name), pos, false, ports);
    }
}
