/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The computer block entity as a whole, before it is taken apart into parts: it follows the data network when a
 * cable next to it or far away is cut and laid again, and every part of it (hardware, system, settings, programs,
 * power) comes back from its tag.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerBlockEntityGameTests {

    private ComputerBlockEntityGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /** Ticks for a cable change to reach the machines on either side of it. */
    private static final int PROPAGATE = 10;
    private static final int PLENTY = 100_000;

    private static final BlockPos BACKBONE = new BlockPos(2, 2, 2);
    private static final BlockPos DESK_CABLE = new BlockPos(4, 2, 2);

    private static final String COUNTER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Counter : IScript {
                int seen;
                public void OnInit() { seen = 0; }
                public void OnTick() { seen = seen + 1; Console.PrintLine("tick " + seen); }
                public void OnDestroy() { }
            }
            """;

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void network_followsACableCutNextToTheComputerAndFarFromIt(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(BACKBONE, ComputingModule.HBW_CABLE);
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(DESK_CABLE, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + PROPAGATE, () -> {
                    helper.assertTrue(mainframe.networkUuid() != null, "the Mainframe owns a network");
                    helper.assertTrue(Objects.equals(pc.networkUuid(), mainframe.networkUuid()),
                            "the computer joins it through the router; got " + pc.networkUuid());
                })
                .thenExecute(() -> helper.setBlock(DESK_CABLE, Blocks.AIR))
                .thenExecuteAfter(PROPAGATE, () -> helper.assertTrue(pc.networkUuid() == null && !pc.networkAttached(),
                        "the cable next to the computer cut: it is off the network; got " + pc.networkUuid()))
                .thenExecute(() -> TestCables.lay(helper, DESK_CABLE, ComputingModule.ETHERNET_CABLE))
                .thenExecuteAfter(PROPAGATE, () -> helper.assertTrue(Objects.equals(pc.networkUuid(), mainframe.networkUuid()),
                        "laid again: it is back on the same network; got " + pc.networkUuid()))
                .thenExecute(() -> helper.setBlock(BACKBONE, Blocks.AIR))
                .thenExecuteAfter(PROPAGATE, () -> helper.assertTrue(pc.networkUuid() == null,
                        "the backbone cut far from the computer: its side has no Mainframe; got " + pc.networkUuid()))
                .thenExecute(() -> TestCables.lay(helper, BACKBONE, ComputingModule.HBW_CABLE))
                .thenExecuteAfter(PROPAGATE, () -> helper.assertTrue(Objects.equals(pc.networkUuid(), mainframe.networkUuid()),
                        "the backbone laid again: back on the Mainframe's network; got " + pc.networkUuid()))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void parts_allComeBackFromTheComputersTag(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        pc.console().setComputerName("desk");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = pc.programs().start("counter.sgs", COUNTER, 1, pc);
                    helper.assertTrue(started.ok(), "the counter starts: " + started.message());
                    for (int i = 0; i < 3; i++) {
                        pc.programs().tick(PLENTY);
                    }
                    final List<String> said = pc.programs().byId(started.id()).process().console();
                    helper.assertTrue(!said.isEmpty(), "the counter has said something before the save");

                    final var registries = helper.getLevel().registryAccess();
                    final CompoundTag saved = pc.saveWithFullMetadata(registries);
                    final PersonalComputerBlockEntity fresh =
                            new PersonalComputerBlockEntity(pc.getBlockPos(), pc.getBlockState());
                    fresh.loadWithComponents(saved, registries);

                    helper.assertTrue(Objects.equals(fresh.installedOsId(), pc.installedOsId())
                                    && fresh.installedOsId() != null,
                            "the system comes back; got " + fresh.installedOsId());
                    helper.assertTrue("desk".equals(fresh.console().computerName()),
                            "the settings come back; name " + fresh.console().computerName());
                    for (final int slot : new int[]{PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                            PersonalComputerBlockEntity.CPU_SLOT, PersonalComputerBlockEntity.RAM_SLOTS_START,
                            PersonalComputerBlockEntity.PSU_SLOT, PersonalComputerBlockEntity.DISK_SLOTS_START}) {
                        helper.assertTrue(ItemStack.isSameItemSameComponents(fresh.getHardware().getStackInSlot(slot),
                                        pc.getHardware().getStackInSlot(slot)),
                                "the part in slot " + slot + " comes back as it was");
                    }
                    helper.assertTrue(fresh.programs().view().size() == 1, "the program comes back; got "
                            + fresh.programs().view().size());
                    final var kept = fresh.programs().byId(fresh.programs().view().getFirst().id()).process().console();
                    helper.assertTrue(kept.equals(said), "with what it had said; got " + kept);

                    pc.loadWithComponents(saved, registries);
                    helper.assertTrue(pc.isRunning(), "read back in the world, the computer is still switched on");
                })
                .thenSucceed();
    }

    /** A restart closes every program the machine was running, as on any machine: the next run starts with none. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void restart_closesEveryProgramTheMachineRan(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!pc.needsPost() && !pc.booting(), "the machine comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(pc.programs().start("counter.sgs", COUNTER, 1, pc).ok(), "the counter starts");
                    pc.restart();
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.programs().isEmpty(), "the restart closes the counter"))
                .thenSucceed();
    }

    /*
     * One name for the machine and its system, whichever side it is typed on: the assembly screen names the
     * machine's console, and the prompt's setting names the machine. Each used to keep a name of its own.
     */
    @GameTest(template = ARENA)
    public static void name_isTheSameOnTheAssemblyScreenAndInTheSystem(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    pc.setCustomName("Desk");
                    helper.assertTrue("Desk".equals(pc.console().computerName()),
                            "named on the assembly screen, the system says it: " + pc.console().computerName());

                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    helper.assertTrue(cli.setConfig("name", "Lab").ok(), "the prompt sets the name");
                    helper.assertTrue("Lab".equals(pc.customName()),
                            "named at the prompt, the assembly screen says it: " + pc.customName());
                    helper.assertTrue("Lab".equals(pc.console().computerName()),
                            "and the system does too: " + pc.console().computerName());
                })
                .thenSucceed();
    }

    /* A disk from a named machine names an unnamed one, and a machine with a name keeps it over the disk's. */
    @GameTest(template = ARENA)
    public static void name_travelsOnTheDiskToAMachineWithNoneAndYieldsToOneWithItsOwn(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity named = world.placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final PersonalComputerBlockEntity blank = world.placeRunningPersonalComputer(new BlockPos(2, 2, 5));
        final PersonalComputerBlockEntity other = world.placeRunningPersonalComputer(new BlockPos(5, 2, 5));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    named.setCustomName("Desk");
                    named.setChanged(); // writes the console, the name on it, onto the disk
                    final ItemStack travelling = named.systemDisk().copy();

                    blank.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START, travelling);
                    helper.assertTrue("Desk".equals(blank.console().computerName())
                                    && "Desk".equals(blank.customName()),
                            "the disk names the machine that had no name; got " + blank.customName());

                    other.setCustomName("Lab");
                    other.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                            travelling.copy());
                    helper.assertTrue("Lab".equals(other.customName())
                                    && "Lab".equals(other.console().computerName()),
                            "a machine with a name keeps it, and its system takes it; got "
                                    + other.console().computerName());
                })
                .thenSucceed();
    }
}
