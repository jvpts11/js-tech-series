/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.MachineSwap;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestCli;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Swap (computers/docs/design/hardware.md): a Frames sizes its page file as it comes up, one and a half times the
 * memory on XP, and it weighs on its volume; a Linux machine uses every swap partition on its disks, promises its
 * memory and its swap together, and shows them at the prompt with free and swapon.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SwapGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath("jsc", "ubuntu");
    private static final int SWAP_MB = 4_096;

    private SwapGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void pageFile_framesXpSizesItsOwnAsItComesUp(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = EraNetwork.runningComputer(world, EraKit.LEGACY, AT);
        final long freeBefore = OsDisks.systemDiskFreeWeight(pc.systemDisk());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the system comes up"))
                .thenExecute(() -> {
                    final int ram = pc.ramTotalMb();
                    final int page = OsDisks.systemsOn(pc.systemDisk()).pageMbOf(pc.installedOsId());
                    helper.assertTrue(page == ram * 3 / 2, "XP gives itself one and a half times the memory: "
                            + page + " of " + ram);
                    final MachineSwap.Swap swap = MachineSwap.of(pc);
                    helper.assertTrue(swap.sizeMb() == page && "pagefile.sys".equals(swap.pageFile()),
                            "its swap is that page file: " + swap);
                    final RamLedger ledger = pc.ramLedger();
                    helper.assertTrue(ledger.promisableMb() == ram + page, "promising the memory and the page file");
                    helper.assertTrue(OsDisks.systemDiskFreeWeight(pc.systemDisk()) < freeBefore,
                            "and the page file weighs on C: as files do");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void swapPartition_linuxPromisesItsMemoryAndItsSwap(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(AT, UBUNTU);
        // A second disk with one swap partition at its start.
        final ItemStack second = new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500));
        final DiskLayout implicit = DiskPartitions.layout(second);
        DiskPartitions.relayout(second, DiskLayout.empty(implicit.scheme(), implicit.diskMb())
                .create(0L, SWAP_MB, FileSystemType.SWAP, PartitionRole.DATA).layout());
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + 1, second);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the system comes up"))
                .thenExecute(() -> {
                    final MachineSwap.Swap swap = MachineSwap.of(pc);
                    helper.assertTrue(swap.sizeMb() == SWAP_MB && swap.areas().size() == 1, "the swap partition is"
                            + " its swap: " + swap);
                    helper.assertTrue(pc.ramLedger().promisableMb() == pc.ramTotalMb() + SWAP_MB,
                            "it promises its memory and its swap together");
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    helper.assertTrue(TestCli.contains(shell.run("free -h", cli), "Swap:"),
                            "free shows the swap");
                    helper.assertTrue(TestCli.contains(shell.run("swapon --show", cli), "/dev/sdb1"),
                            "swapon names its partition");
                })
                .thenSucceed();
    }

    /* Whether the machine is on and its system has come up. */
    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }
}
