/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.TestShell.text;

/**
 * The device tools at a Unix prompt: on Linux {@code lspci} lists what sits on the board and {@code lsusb} what is
 * plugged into the ports, {@code lsusb -t} as a tree with the hubs and the free ports; on FreeBSD {@code pciconf -lv}
 * and {@code usbconfig}. A device the computer disabled is marked by each.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DeviceCommandGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final int SETTLE = 20;
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu");
    private static final ResourceLocation FREEBSD = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "freebsd");

    private DeviceCommandGameTests() {
    }

    @GameTest(template = ARENA)
    public static void linux_lspciAndLsusbListTheBoardAndThePorts(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, UBUNTU);
        world.setBlock(COMPUTER.south(), ComputingModule.CD_DRIVE.get());
        world.setBlock(COMPUTER.west(), ComputingModule.STANDARD_HUB.get());
        final BlockPos sensorAt = COMPUTER.west().north();
        helper.setBlock(sensorAt, ComputingModule.VINTAGE_REDSTONE_INTERFACE.get().defaultBlockState()
                .setValue(RedstoneInterfaceBlock.FACING, Direction.NORTH));
        final long drive = helper.absolutePos(COMPUTER.south()).asLong();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (helper.getBlockEntity(sensorAt) instanceof RedstoneInterfaceBlockEntity sensor) {
                        sensor.rename("Lamps");
                    }
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final String lspci = text(shell.run("lspci", cli));
                    helper.assertTrue(lspci.contains("00:00.0 Host bridge: ") && lspci.contains("USB controller: "
                                    + "USB 3 controller, 8 ports") && lspci.contains("VGA compatible controller: ")
                                    && lspci.contains("Audio device: Audio on the board"),
                            "lspci lists the board, its USB controller, its audio and the graphics card; got " + lspci);
                    final String tree = text(shell.run("lsusb -t", cli));
                    helper.assertTrue(tree.startsWith("/:  Bus 01.Port 1: USB 3 controller, 8 ports")
                                    && tree.contains("|__ Port") && tree.contains(": CD Drive")
                                    && tree.contains(": Standard Hub (7 ports)")
                                    && tree.contains("        |__ Port 1: Vintage Redstone Interface \"Lamps\"")
                                    && tree.contains("(free)"),
                            "lsusb -t draws the ports as a tree, the hub's under it, the free ones too; got " + tree);
                    helper.assertTrue(text(shell.run("lsusb", cli)).contains("Bus 001 Device 002: "),
                            "lsusb lists the devices one a line");
                    pc.setDisabled(drive, true);
                    helper.assertTrue(text(shell.run("lsusb -t", cli)).contains("CD Drive [disabled]"),
                            "a disabled device is marked so");
                    helper.assertTrue(text(shell.run("lsusb -x", cli)).contains("invalid option"),
                            "an option it does not take is refused");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void freebsd_pciconfAndUsbconfigListTheBoardAndThePorts(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = freebsdMainframe(helper);
        helper.setBlock(COMPUTER.east(), ComputingModule.CD_DRIVE.get());
        final long drive = helper.absolutePos(COMPUTER.east()).asLong();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final String pci = text(shell.run("pciconf -lv", cli));
                    helper.assertTrue(pci.contains("hostb0@pci0:0:0:0:  class=0x060000")
                                    && pci.contains("    device   = '") && pci.contains("    class    = bridge"),
                            "pciconf -lv lists the board with its class, verbosely; got " + pci);
                    helper.assertTrue(text(shell.run("pciconf", cli)).contains("usage: pciconf"),
                            "asked nothing, it says how it is used");
                    mainframe.setDisabled(drive, true);
                    final String usb = text(shell.run("usbconfig", cli));
                    helper.assertTrue(usb.startsWith("ugen0.1: <USB")
                                    && usb.contains("ugen0.2: <CD Drive> at usbus0, cfg=255 md=HOST"),
                            "usbconfig lists the controller and the drive, disabled at configuration 255; got "
                                    + usb);
                })
                .thenSucceed();
    }

    /* A powered Mainframe with a full, 64-bit build and a disk, FreeBSD installed on it. */
    private static MainframeBlockEntity freebsdMainframe(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(COMPUTER) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + COMPUTER);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        if (!mainframe.installOs(FREEBSD)) {
            throw new IllegalStateException("failed to install FreeBSD on the test Mainframe");
        }
        return mainframe;
    }
}
