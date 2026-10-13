/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.block.part.ExternalStorageBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.core.multipart.PartType;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The External Storage Bus: the network counts the inventory it faces as storage of its own, as much of it as the
 * filter lets through; reads it, writes it in the turn of its priority, never writes a read-only one nor sees into a
 * write-only one; and reaches it ten times slower than its own storage.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ExternalStorageGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos SOUTH_CHEST = new BlockPos(2, 2, 3);
    private static final BlockPos EAST_CHEST = new BlockPos(3, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);

    private ExternalStorageGameTests() {
    }

    /** What the faced chest holds is the network's: its storage counts it. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void external_showsTheNetworkItsChest(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        chest(helper, SOUTH_CHEST, new ItemStack(Items.EMERALD, 20));
        mount(helper, Direction.SOUTH, ComputingParts.EXTERNAL.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.EMERALD) == 20,
                        "the network holds the chest's 20 emeralds; got " + stock(helper, mainframe, Items.EMERALD)))
                .thenSucceed();
    }

    /** The Legacy bus's filter decides what of the chest the network sees. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void external_showsOnlyWhatItsFilterLists(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        chest(helper, SOUTH_CHEST, new ItemStack(Items.EMERALD, 20), new ItemStack(Items.DIAMOND, 5));
        final ExternalStorageBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.LEGACY_EXTERNAL.get());
        bus.setFilterSlot(0, new ItemStack(Items.EMERALD), "");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.EMERALD) == 20,
                        "the emeralds are seen"))
                .thenExecute(() -> helper.assertTrue(stock(helper, mainframe, Items.DIAMOND) == 0,
                        "and the diamonds are not"))
                .thenSucceed();
    }

    /** A write-only chest is never seen into. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void external_writeOnly_hidesWhatItHolds(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        chest(helper, SOUTH_CHEST, new ItemStack(Items.EMERALD, 20));
        final ExternalStorageBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.LEGACY_EXTERNAL.get());
        bus.setAccess(BusSettings.WRITE_ONLY, "");
        helper.startSequence()
                .thenExecuteAfter(40, () -> helper.assertTrue(stock(helper, mainframe, Items.EMERALD) == 0,
                        "nothing of it is seen; got " + stock(helper, mainframe, Items.EMERALD)))
                .thenSucceed();
    }

    /** What comes in fills the chest of a priority above the network's own storage first. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void external_priority_fillsTheChestFirst(final GameTestHelper helper) {
        network(helper);
        final Container external = chest(helper, SOUTH_CHEST);
        chest(helper, EAST_CHEST, new ItemStack(Items.EMERALD, 10));
        final ExternalStorageBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.EXTERNAL.get());
        bus.setPriority(5, "");
        mount(helper, Direction.EAST, ComputingParts.IMPORT.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(external.countItem(Items.EMERALD) == 10,
                        "the ten emeralds went into the chest of priority 5; it holds "
                                + external.countItem(Items.EMERALD)))
                .thenExecute(() -> helper.assertTrue(bus.busy(), "what the network put through it lights its lamps"))
                .thenSucceed();
    }

    /** A read-only chest is never filled, whatever its priority: what comes in goes to the network's storage. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void external_readOnly_isNeverFilled(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final Container external = chest(helper, SOUTH_CHEST);
        chest(helper, EAST_CHEST, new ItemStack(Items.EMERALD, 10));
        final ExternalStorageBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.EXTERNAL.get());
        bus.setPriority(5, "");
        bus.setAccess(BusSettings.READ_ONLY, "");
        mount(helper, Direction.EAST, ComputingParts.IMPORT.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.EMERALD) == 10,
                        "the ten emeralds are in the network"))
                .thenExecute(() -> helper.assertTrue(external.countItem(Items.EMERALD) == 0,
                        "and none in the read-only chest"))
                .thenSucceed();
    }

    /** An Export Bus sends what the network has in the chest of an External Storage Bus. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void external_isReadLikeTheNetworksOwnStorage(final GameTestHelper helper) {
        network(helper);
        final Container external = chest(helper, SOUTH_CHEST, new ItemStack(Items.EMERALD, 10));
        final Container out = chest(helper, EAST_CHEST);
        mount(helper, Direction.SOUTH, ComputingParts.EXTERNAL.get());
        final ExportBusPart export = mount(helper, Direction.EAST, ComputingParts.EXPORT.get());
        export.setFilter(new ItemStack(Items.EMERALD));
        export.setKeep(64, "");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(out.countItem(Items.EMERALD) == 10,
                        "the ten went from the external chest to the exported one; it holds "
                                + out.countItem(Items.EMERALD)))
                .thenExecute(() -> helper.assertTrue(external.countItem(Items.EMERALD) == 0, "and left the first"))
                .thenSucceed();
    }

    /** The network reaches an external inventory at a tenth of what the bus's cable carries. */
    @GameTest(template = ARENA)
    public static void external_isTenTimesSlower(final GameTestHelper helper) {
        network(helper);
        chest(helper, SOUTH_CHEST);
        final ExternalStorageBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.EXTERNAL.get());
        helper.assertTrue(bus.throughput() == Math.max(1L, bus.cableCarries() / ExternalStorageBusPart.SLOWER),
                "a tenth of the cable's " + bus.cableCarries() + "; got " + bus.throughput());
        helper.succeed();
    }

    /* A running Mainframe with an HBW cable beside it, where the buses go, and a seeded rack north of the cable. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
        world.placeSeededRack(RACK);
        return mainframe;
    }

    /* A barrel at {@code at} holding {@code stacks}. */
    private static Container chest(final GameTestHelper helper, final BlockPos at, final ItemStack... stacks) {
        helper.setBlock(at, Blocks.BARREL);
        if (!(helper.getBlockEntity(at) instanceof Container container)) {
            throw new IllegalStateException("no barrel at " + at.toShortString());
        }
        for (int slot = 0; slot < stacks.length; slot++) {
            container.setItem(slot, stacks[slot].copy());
        }
        return container;
    }

    private static <T extends AbstractBusPart> T mount(final GameTestHelper helper, final Direction face,
                                                       final PartType<T> type) {
        final T bus = type.create();
        TestCables.cable(helper, CABLE).addPart(face, bus);
        return bus;
    }

    private static long stock(final GameTestHelper helper, final MainframeBlockEntity mainframe, final Item item) {
        return mainframe.networkUuid() == null ? 0L
                : NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).count(item);
    }
}
