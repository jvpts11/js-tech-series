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
import dev.jstech.tests.JsTests;
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
        world.setBlock(BACKBONE, ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(DESK_CABLE, ComputingModule.ETHERNET_CABLE.get());
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
                .thenExecute(() -> helper.setBlock(DESK_CABLE, ComputingModule.ETHERNET_CABLE.get()))
                .thenExecuteAfter(PROPAGATE, () -> helper.assertTrue(Objects.equals(pc.networkUuid(), mainframe.networkUuid()),
                        "laid again: it is back on the same network; got " + pc.networkUuid()))
                .thenExecute(() -> helper.setBlock(BACKBONE, Blocks.AIR))
                .thenExecuteAfter(PROPAGATE, () -> helper.assertTrue(pc.networkUuid() == null,
                        "the backbone cut far from the computer: its side has no Mainframe; got " + pc.networkUuid()))
                .thenExecute(() -> helper.setBlock(BACKBONE, ComputingModule.HBW_CABLE.get()))
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
                    final MachinePrograms.Started started = pc.sigma().start("counter.sgs", COUNTER, 1, pc);
                    helper.assertTrue(started.ok(), "the counter starts: " + started.message());
                    for (int i = 0; i < 3; i++) {
                        pc.sigma().tick(PLENTY);
                    }
                    final List<String> said = pc.sigma().byId(started.id()).process().console();
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
                    helper.assertTrue(fresh.sigma().all().size() == 1, "the program comes back; got "
                            + fresh.sigma().all().size());
                    helper.assertTrue(fresh.sigma().all().getFirst().process().console().equals(said),
                            "with what it had said; got " + fresh.sigma().all().getFirst().process().console());

                    pc.loadWithComponents(saved, registries);
                    helper.assertTrue(pc.isRunning(), "read back in the world, the computer is still switched on");
                })
                .thenSucceed();
    }
}
