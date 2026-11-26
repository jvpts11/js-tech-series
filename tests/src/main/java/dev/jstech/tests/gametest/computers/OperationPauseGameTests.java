/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.IPersistentOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.computers.CraftingRig;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * An order in the Operations list as a player handles it: one line an order, its machine steps only in its detail;
 * paused, it feeds its machines nothing new and never times out, and goes on from where it stopped; restored after a
 * restart while its steps ran, it stands in the list waiting to resume, and is saved again as it is.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OperationPauseGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final int ORDER = 16;
    /* Long enough, paused, for a step that ran to time out, were time counted. */
    private static final int PAUSED_TICKS = 260;

    private OperationPauseGameTests() {
    }

    /**
     * Paused while its kiln step runs, an order lets the kiln finish what it holds and feeds it nothing new, and does
     * not time out; resumed, it makes the whole order. Its kiln step never stands in the list as a line of its own.
     */
    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void pause_holdsAnOrderAndResumeFinishesIt(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final AtomicReference<NetworkCraftOperation> craft = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobble);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    craft.set(net.mainframe().submitNetworkCraft(StorageKey.of(Items.STONE), ORDER, false, "pause"));
                    helper.assertTrue(craft.get() != null, "the order is planned through the kiln's recipe");
                })
                .thenWaitUntil(() -> helper.assertTrue(stepRunning(craft.get().liveRecord()),
                        "its kiln step runs"))
                .thenExecute(() -> {
                    final List<OperationRecord> live = net.mainframe().activeOperationRecords();
                    helper.assertTrue(live.size() == 1 && live.getFirst().id().equals(craft.get().operationId()),
                            "one line in the list, the order's, its kiln step in its detail: " + live);
                    helper.assertTrue(net.mainframe().pauseOperation(craft.get().operationId(), true),
                            "the order pauses");
                    final OperationRecord paused = craft.get().liveRecord();
                    helper.assertTrue(paused.hold() == OperationRecord.HOLD_PAUSED && paused.subs().stream()
                            .anyMatch(step -> step.state() == OperationRecord.SubRow.SUB_PAUSED),
                            "it reads as held, its running step as paused: " + paused);
                })
                .thenWaitUntil(() -> helper.assertTrue(rig.inMachine(Items.COBBLESTONE) == 0,
                        "the kiln finishes what it held"))
                .thenExecuteAfter(PAUSED_TICKS, () -> {
                    helper.assertTrue(rig.inMachine(Items.COBBLESTONE) == 0, "and is fed nothing new while paused");
                    helper.assertTrue(!craft.get().isDone(), "paused, the order does not time out: "
                            + craft.get().liveRecord());
                    helper.assertTrue(stored(net, helper) < ORDER, "nor makes the rest");
                    helper.assertTrue(net.mainframe().pauseOperation(craft.get().operationId(), false),
                            "it goes on");
                })
                .thenWaitUntil(() -> helper.assertTrue(craft.get().isDone(), "the order settles"))
                .thenExecute(() -> {
                    helper.assertTrue(craft.get().toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "resumed, it completes: " + craft.get().toRecord().cause());
                    helper.assertTrue(stored(net, helper) == ORDER, "the whole order in the network, not "
                            + stored(net, helper));
                })
                .thenSucceed();
    }

    /**
     * Restored after a restart while its kiln step ran, an order stands in the list waiting to resume, under its own
     * id, its step in its detail; saved again before it plans, it is saved as that order; and it makes the rest once
     * its step settles. It vanished from the list meanwhile, and a second save lost it.
     */
    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void restart_anOrderWaitsToResumeInTheListAndIsSavedAgain(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final AtomicReference<NetworkCraftOperation> craft = new AtomicReference<>();
        final CompoundTag[] snapshot = new CompoundTag[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobble);
                })
                .thenExecuteAfter(SETTLE, () -> craft.set(net.mainframe().submitNetworkCraft(
                        StorageKey.of(Items.STONE), ORDER, false, "restart")))
                .thenWaitUntil(() -> helper.assertTrue(stepRunning(craft.get().liveRecord()),
                        "its kiln step runs"))
                .thenExecute(() -> {
                    snapshot[0] = net.mainframe().saveWithoutMetadata(helper.getLevel().registryAccess());
                    world.setBlock(MAINFRAME, Blocks.AIR);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    world.setBlock(MAINFRAME, ComputingModule.MAINFRAME.get());
                    mainframe(world).loadWithComponents(snapshot[0], helper.getLevel().registryAccess());
                })
                .thenWaitUntil(() -> {
                    final OperationRecord waiting = recordOf(mainframe(world), craft.get().operationId());
                    helper.assertTrue(waiting != null && waiting.hold() == OperationRecord.HOLD_RESUMING
                                    && waiting.status() == OperationRecord.STATUS_WAITING && !waiting.subs().isEmpty(),
                            "the order stands in the list waiting to resume, its step in its detail: "
                                    + mainframe(world).activeOperationRecords());
                })
                .thenExecute(() -> {
                    final CompoundTag again = mainframe(world).saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    final ListTag saved = again.getList("ActiveOperations", Tag.TAG_COMPOUND);
                    boolean kept = false;
                    for (int i = 0; i < saved.size(); i++) {
                        final CompoundTag op = saved.getCompound(i);
                        kept |= NetworkCraftOperation.KIND.equals(op.getString(IPersistentOperation.KIND_KEY))
                                && op.hasUUID(IPersistentOperation.ID_KEY)
                                && op.getUUID(IPersistentOperation.ID_KEY).equals(craft.get().operationId());
                    }
                    helper.assertTrue(kept, "saved again while it waits, as the order: " + saved);
                })
                .thenWaitUntil(() -> helper.assertTrue(stored(net, helper) >= ORDER,
                        "the rest of the order made once its step settles: " + stored(net, helper)))
                .thenExecute(() -> helper.assertTrue(stored(net, helper) == ORDER,
                        "the order made once, nothing twice: " + stored(net, helper)))
                .thenSucceed();
    }

    /* Whether one of the order's steps is running now. */
    private static boolean stepRunning(final OperationRecord record) {
        return record.subs().stream().anyMatch(step -> step.state() == OperationRecord.SubRow.SUB_STREAMING);
    }

    private static MainframeBlockEntity mainframe(final TestWorldBuilder world) {
        return world.blockEntity(MAINFRAME, MainframeBlockEntity.class);
    }

    /* The live record of the Operation {@code id}, or null. */
    private static OperationRecord recordOf(final MainframeBlockEntity mainframe, final UUID id) {
        return mainframe.activeOperationRecords().stream().filter(record -> record.id().equals(id)).findFirst()
                .orElse(null);
    }

    private static long stored(final TestWorldBuilder.CraftingNetwork net, final GameTestHelper helper) {
        final NetworkStorage storage = net.storage(helper.getLevel());
        return storage.count(StorageKey.of(Items.STONE));
    }
}
