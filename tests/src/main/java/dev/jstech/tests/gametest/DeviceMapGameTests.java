/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceMaps;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * A machine's Device Manager map, read off it in the world: every port by kind with what its cables link to it, a
 * hub's ports under the hub, the free ones, a Vintage board's serial and parallel ports, and a device the computer
 * disabled marked as such.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DeviceMapGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final int SETTLE = 20;

    private DeviceMapGameTests() {
    }

    @GameTest(template = ARENA)
    public static void map_namesEachPortAndWhatIsPluggedIn(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(COMPUTER.east(), Direction.EAST);
        world.setBlock(COMPUTER.north(), ComputingModule.SPEAKER.get());
        world.setBlock(COMPUTER.south(), ComputingModule.CD_DRIVE.get());
        world.setBlock(COMPUTER.west(), ComputingModule.STANDARD_HUB.get());
        final BlockPos sensorAt = COMPUTER.west().north();
        helper.setBlock(sensorAt, ComputingModule.VINTAGE_REDSTONE_INTERFACE.get().defaultBlockState()
                .setValue(RedstoneInterfaceBlock.FACING, Direction.NORTH));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (helper.getBlockEntity(sensorAt) instanceof RedstoneInterfaceBlockEntity sensor) {
                        sensor.rename("Gate");
                    }
                    final DeviceMap map = DeviceMaps.of(helper.getLevel(), pc);
                    final DeviceMap.Port firstOutput = map.video().getFirst().outputs().getFirst();
                    helper.assertTrue(english(firstOutput).equals("Output 1: Monitor"),
                            "the graphics card's first output carries the monitor; got " + english(firstOutput));
                    helper.assertTrue(map.video().getFirst().outputs().stream().skip(1)
                            .allMatch(DeviceMap.Port::isFree), "and its other outputs are free");
                    final DeviceMap.AudioSource audio = map.audio().getFirst();
                    helper.assertTrue(audio.name().english().equals("Audio on the board")
                                    && audio.outputs().getFirst().plugged().size() == 1,
                            "the board's audio drives the speaker; got " + audio.name().english());
                    helper.assertTrue(map.family() == DeviceMap.PortFamily.USB3, "a Standard board has USB 3");
                    final List<String> names = map.devices().stream().map(d -> d.name().english()).toList();
                    helper.assertTrue(names.contains("CD Drive") && names.contains("Standard Hub (7 ports)")
                                    && names.contains("Vintage Redstone Interface \"Gate\""),
                            "the drive, the hub and the interface on the hub are listed; got " + names);
                    helper.assertTrue(map.devicePortsTotal() == 8 + 7,
                            "eight ports on the board and seven on the hub; got " + map.devicePortsTotal());
                    helper.assertTrue(map.devicePortsInUse() == 3,
                            "three in use, the hub's own port counted; got " + map.devicePortsInUse());
                    final DeviceMap.Device hub = map.devices().stream().filter(DeviceMap.Device::isHub).findFirst()
                            .orElseThrow();
                    helper.assertTrue(english(hub.ports().getFirst()).equals("Hub port 1: Vintage Redstone Interface"
                            + " \"Gate\""), "the interface is on the hub's first port; got "
                            + english(hub.ports().getFirst()));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void map_onAVintageBoardHasASerialAndAParallelPort(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = vintagePc(helper);
        helper.setBlock(COMPUTER.west(), ComputingModule.FLOPPY_DRIVE.get());
        helper.setBlock(COMPUTER.south(), ComputingModule.FLOPPY_DRIVE.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final DeviceMap map = DeviceMaps.of(helper.getLevel(), pc);
                    helper.assertTrue(map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL,
                            "a Vintage board has serial and parallel ports");
                    final List<String> ports = map.devicePorts().stream().map(DeviceMapGameTests::english).toList();
                    helper.assertTrue(ports.equals(List.of("Serial (COM1): Floppy Drive",
                            "Parallel (LPT1): Floppy Drive")), "COM1 and LPT1, a drive on each; got " + ports);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void map_marksADeviceTheComputerDisabled(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.setBlock(COMPUTER.south(), ComputingModule.CD_DRIVE.get());
        final long drive = helper.absolutePos(COMPUTER.south()).asLong();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    pc.setDisabled(drive, true);
                    final DeviceMap.Device off = DeviceMaps.of(helper.getLevel(), pc).device(drive);
                    helper.assertTrue(off != null && off.disabled(), "the disabled drive is marked so");
                    pc.setDisabled(drive, false);
                    final DeviceMap.Device on = DeviceMaps.of(helper.getLevel(), pc).device(drive);
                    helper.assertTrue(on != null && !on.disabled(), "and no longer once it is enabled");
                })
                .thenSucceed();
    }

    /* A port as a Device Manager lists it: its name, and what is plugged into it. */
    private static String english(final DeviceMap.Port port) {
        final String name = port.name().english();
        return port.isFree() ? name + ": free" : name + ": " + port.plugged().getFirst().name().english();
    }

    /* A Vintage personal computer at COMPUTER, its board's two device ports free. */
    private static PersonalComputerBlockEntity vintagePc(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(COMPUTER) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no computer at " + COMPUTER);
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        return computer;
    }
}
