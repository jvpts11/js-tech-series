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
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.msd.MsdScreen;
import dev.jstech.computers.program.cli.msd.MsdState;
import dev.jstech.computers.program.cli.msd.MsdView;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Vintage systems' diagnostics on a real machine: {@code msd} gives the terminal to the screen, which shows the
 * machine's parts and its COM and LPT ports with what is attached to each, and a device disabled from it is disabled
 * on the machine. The other systems do not have it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MsdGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final int SETTLE = 20;
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final ResourceLocation MC_NET = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net");
    private static final ResourceLocation FRAMES_95 =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95");

    private MsdGameTests() {
    }

    @GameTest(template = ARENA)
    public static void mcDos_msdShowsThePortsAndDisablesADeviceOnOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = vintageMachine(helper, MC_DOS);
        helper.setBlock(COMPUTER.south(), ComputingModule.FLOPPY_DRIVE.get());
        final long floppy = helper.absolutePos(COMPUTER.south()).asLong();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell.Response response = CliCommands.shellFor(cli, 80).run("msd", cli);
                    helper.assertTrue(response.handOver() != null && "msd".equals(response.handOver().editor())
                                    && MsdState.names(response.handOver().path()),
                            "msd gives the terminal to the diagnostics screen");
                    final List<String> main = MsdView.screen(cli, MsdState.of(response.handOver().path()));
                    helper.assertTrue(says(main, "Midsoft Diagnostics 2.01") && says(main, "Integra 486SX")
                                    && says(main, "640K, 3072K Ext") && says(main, "MC-DOS"),
                            "the main screen says what the machine is made of; got\n" + String.join("\n", main));
                    final List<String> ports = MsdView.screen(cli, MsdState.OPENING.openingPorts(0));
                    final int row = rowOf(ports, "Floppy Drive");
                    helper.assertTrue(row >= 0 && (ports.get(MsdScreen.PORTS_TOP + row).contains("COM1:")
                                    || ports.get(MsdScreen.PORTS_TOP + row).contains("LPT1:")),
                            "the drive stands on one of the board's ports by its DOS name; got\n"
                                    + String.join("\n", ports));
                    final List<String> disabled = MsdView.screen(cli,
                            MsdState.OPENING.openingPorts(row).asking(MsdView.DISABLE));
                    helper.assertTrue(pc.isDisabled(floppy), "the machine disabled the drive");
                    helper.assertTrue(disabled.get(MsdScreen.PORTS_TOP + row).contains("Off (disabled)"),
                            "and the screen that comes back says so");
                    MsdView.screen(cli, MsdState.OPENING.openingPorts(row).asking(MsdView.ENABLE));
                    helper.assertFalse(pc.isDisabled(floppy), "Enable gives it back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mcNet_hasMsdToo(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = vintageMachine(helper, MC_NET);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell.Response response = CliCommands.shellFor(cli, 80).run("msd", cli);
                    helper.assertTrue(response.handOver() != null && "msd".equals(response.handOver().editor()),
                            "MC-NET has no Device Manager either, so it has the diagnostics");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void frames_hasNoMsd(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(COMPUTER, FRAMES_95);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    helper.assertTrue(CliCommands.shellFor(cli, 80).run("msd", cli).handOver() == null,
                            "Frames has its Device Manager instead");
                })
                .thenSucceed();
    }

    /* A Vintage machine's parts, the system installed and the machine on. */
    private static PersonalComputerBlockEntity vintageMachine(final GameTestHelper helper,
                                                              final ResourceLocation os) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(HardwareItems.GPU_VGA_256.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.installOs(os);
        computer.togglePower();
        return computer;
    }

    /* Which port row of the dialog names that device, or -1 when none does. */
    private static int rowOf(final List<String> screen, final String device) {
        for (int i = 0; i < MsdScreen.portsSaid(screen); i++) {
            if (screen.get(MsdScreen.PORTS_TOP + i).contains(device)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean says(final List<String> screen, final String text) {
        return screen.stream().anyMatch(line -> line.contains(text));
    }
}
