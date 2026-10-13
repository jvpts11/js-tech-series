/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MsdViewTest {

    private static final long WELL = 7L;
    private static final long FLOPPY = 9L;

    private FakeMachine machine;

    @BeforeEach
    void setUp() {
        this.machine = new FakeMachine();
    }

    @Test
    void screen_saysWhatEachButtonFound() {
        final List<String> screen = MsdView.screen(this.machine, MsdState.OPENING);

        assertTrue(says(screen, "Integra 486DX2"), "the processor");
        assertTrue(says(screen, "640K, 15360K Ext"), "the memory as that program counted it");
        assertTrue(says(screen, "Pyrix 3D Blaster"), "the graphics card");
        assertTrue(says(screen, "Thin coax") && !says(screen, "on the board"), "the network, by its cable");
        assertTrue(says(screen, "MC-DOS"), "the system");
        assertTrue(says(screen, "A: C:"), "the drive letters");
    }

    @Test
    void screen_listsTheBoardsPortsByTheirDosNames() {
        final List<String> screen = MsdView.screen(this.machine, MsdState.OPENING.openingPorts(0));

        assertEquals(2, MsdScreen.portsSaid(screen));
        assertTrue(screen.get(MsdScreen.PORTS_TOP).contains("COM1:")
                && screen.get(MsdScreen.PORTS_TOP).contains("Redstone Interface")
                && screen.get(MsdScreen.PORTS_TOP).contains("On"));
        assertTrue(screen.get(MsdScreen.PORTS_TOP + 1).contains("LPT1:")
                && screen.get(MsdScreen.PORTS_TOP + 1).contains("Floppy Drive"));
    }

    @Test
    void screen_opensTheDialogOnTheFirstPortOfTheKindItWasOpenedFor() {
        final List<String> screen = MsdView.screen(this.machine, MsdState.OPENING.openingPorts(0)
                .asking(MsdView.LPT));

        assertEquals(1, MsdScreen.pickedSaid(screen), "LPT1 is the second port of a Vintage board");
    }

    @Test
    void screen_disablesThePickedPortsDeviceOnTheMachine() {
        final List<String> screen = MsdView.screen(this.machine, MsdState.OPENING.openingPorts(0)
                .asking(MsdView.DISABLE));

        assertEquals(List.of(WELL), this.machine.disabled, "the machine was asked to disable it");
        assertTrue(screen.get(MsdScreen.PORTS_TOP).contains("Off (disabled)"),
                "and the screen that comes back is the machine's as it then stands");
    }

    @Test
    void screen_enablesItAgain() {
        this.machine.disabled.add(WELL);

        MsdView.screen(this.machine, MsdState.OPENING.openingPorts(0).asking(MsdView.ENABLE));

        assertTrue(this.machine.disabled.isEmpty());
    }

    @Test
    void screen_asksNothingOfAFreePort() {
        this.machine.floppyPlugged = false;

        MsdView.screen(this.machine, MsdState.OPENING.openingPorts(1).asking(MsdView.DISABLE));

        assertTrue(this.machine.disabled.isEmpty());
    }

    @Test
    void memory_countsWhatLiesAboveTheFirstMegabyteAsExtended() {
        assertEquals("640K, 15360K Ext", MsdView.memory(16));
        assertEquals("640K, 3072K Ext", MsdView.memory(4));
        assertEquals("640K", MsdView.memory(1));
    }

    private static boolean says(final List<String> screen, final String text) {
        return screen.stream().anyMatch(line -> line.contains(text));
    }

    /* A Vintage machine with a Redstone Interface on COM1 and a floppy drive on LPT1. */
    private static final class FakeMachine implements ICliComputer {

        final List<Long> disabled = new ArrayList<>();
        boolean floppyPlugged = true;

        @Override public DeviceMap devices() {
            final DeviceMap.Port com = new DeviceMap.Port(Text.literal("Serial (COM1)"), List.of(
                    new DeviceMap.Device("redstone_interface", Text.literal("Redstone Interface \"Well\""), WELL,
                            this.disabled.contains(WELL), List.of())));
            final DeviceMap.Port lpt = this.floppyPlugged
                    ? new DeviceMap.Port(Text.literal("Parallel (LPT1)"), List.of(new DeviceMap.Device(
                            "floppy_drive", Text.literal("Floppy Drive"), FLOPPY, this.disabled.contains(FLOPPY),
                            List.of())))
                    : DeviceMap.Port.free(Text.literal("Parallel (LPT1)"));
            return new DeviceMap("lab", Text.literal("MF AT Vintage Motherboard"),
                    List.of(Text.literal("Integra 486DX2")), List.of(), List.of(),
                    List.of(new DeviceMap.VideoCard(Text.literal("Pyrix 3D Blaster"), List.of())), List.of(),
                    DeviceMap.PortFamily.SERIAL_PARALLEL, List.of(com, lpt),
                    List.of(DeviceMap.boardNetwork(Text.literal("Thin coax"))),
                    List.of());
        }

        @Override public boolean setDeviceDisabled(final long pos, final boolean disable) {
            if (disable) {
                this.disabled.add(pos);
            } else {
                this.disabled.remove(pos);
            }
            return true;
        }

        @Override public MemoryUse memory() {
            return new MemoryUse(16, 2, 0L);
        }

        @Override public SystemInfo systemInfo() {
            return new SystemInfo("mc_dos", "MC-DOS", "JSC mc_dos", "lab", "dos", "", "Integra 486DX2", 16, 0L, 0L,
                    0, 0L);
        }

        @Override public List<MountInfo> mounts() {
            return List.of(new MountInfo('a', "fd0", 1L, 1L, true), new MountInfo('c', "hd0", 64L, 32L, true));
        }
    }
}
