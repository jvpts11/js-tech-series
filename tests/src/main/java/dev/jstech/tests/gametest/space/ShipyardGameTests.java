/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardLayout;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Small Shipyard: built whole, dug into the ground, it forms, its blocks merge into its model and take its shapes
 * (a walkway its grating, a tower its lattice), its item port staying itself; a block in its pit undoes it, and
 * clearing the pit forms it again; a block of it taken away undoes it and its other blocks come back as they were;
 * and cleared all at once, as a blast or a fill clears it, it is gone in a moment.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_shipyard";
    /* The pattern's lowest corner, the pit floor's west front corner, a ground of stone under it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* Places of the dock, in its own blocks: a tower's foot, a walkway, the item port, the pit, the stair's foot. */
    private static final BlockPos TOWER = dock(-11, 1, -13);
    private static final BlockPos WALKWAY = dock(-10, 3, 0);
    private static final BlockPos PORT = dock(10, 0, 9);
    private static final BlockPos PIT = dock(-5, -2, -9);
    /* Over half the block: a deck lies at its block's top. */
    private static final double DECK = 0.5;
    /* The pattern's size, less a block each way: from its lowest corner to its highest. */
    private static final BlockPos SIZE = new BlockPos(22, 14, 26);
    /*
     * How long clearing the whole formed dock in one tick may take, in milliseconds: far over what it takes, a few
     * hundred, and far under the minutes it took while each loss worked the struck model out again.
     */
    private static final long CLEARED_WITHIN = 10_000;
    private static final long NANOS_A_MILLI = 1_000_000;
    /* Corridors enough to make a ship longer than the pit, which flies all the same. */
    private static final int LONG_SHIP = 6;

    private ShipyardGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_formsWhenBuiltWholeAndItsBlocksTakeTheDocksShapes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.succeedWhen(() -> {
            helper.assertTrue(formed(level, terminal), "the dock forms whole");
            helper.assertBlockPresent(FormedParts.BLOCK.get(), TOWER);
            helper.assertBlockPresent(FormedParts.BLOCK.get(), WALKWAY);
            helper.assertBlockPresent(HangarBlocks.ITEM_PORT.get(), PORT);
            final BlockPos walkway = helper.absolutePos(WALKWAY);
            final VoxelShape deck = level.getBlockState(walkway).getCollisionShape(level, walkway);
            helper.assertTrue(!deck.isEmpty() && deck.min(Direction.Axis.Y) > DECK,
                    "a walkway is its grating at its block's top: " + deck.bounds());
            final BlockPos tower = helper.absolutePos(TOWER);
            final VoxelShape lattice = level.getBlockState(tower).getCollisionShape(level, tower);
            helper.assertTrue(!lattice.isEmpty() && !Block.isShapeFullBlock(lattice),
                    "a tower is its lattice, not a whole block");
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_aBlockInItsPitUndoesItAndClearingThePitFormsItAgain(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> helper.setBlock(PIT, Blocks.DIRT))
                .thenWaitUntil(() -> helper.assertTrue(!formed(level, terminal), "a block in the pit undoes it"))
                .thenExecute(() -> helper.setBlock(PIT, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the pit cleared, it forms again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_aBlockTakenAwayUndoesItAndItsBlocksComeBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> helper.destroyBlock(TOWER))
                .thenWaitUntil(() -> {
                    helper.assertTrue(!formed(level, terminal), "a tower block taken away undoes it");
                    helper.assertBlockPresent(ShipyardBlocks.WALKWAY.get(), WALKWAY);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_clearedAllAtOnceIsClearedAtOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    // Every block of the formed dock taken in one tick, as a blast or a fill takes them: each lost
                    // block marks the ones beside it, which must not work the struck model out again each time.
                    final long start = System.nanoTime();
                    for (final BlockPos at : BlockPos.betweenClosed(helper.absolutePos(CORNER),
                            helper.absolutePos(CORNER.offset(SIZE)))) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    final long millis = (System.nanoTime() - start) / NANOS_A_MILLI;
                    helper.assertTrue(millis < CLEARED_WITHIN, "the dock cleared in " + millis + " ms");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_keepsItsDesignSavesItByNameAndSendsOnlyAReadyShipThatFits(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final ShipDesign ready = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
                part("quarters"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(ready.withHung(List.of()));
                    helper.assertTrue(!dock.confirm() && dock.plan().isEmpty(),
                            "a ship with no engines, wings or gear is not sent");
                    dock.design(ready);
                    helper.assertTrue(dock.design().equals(ready), "it keeps the design on its screen");
                    helper.assertTrue(dock.save() && dock.plans().size() == 1 && dock.save()
                            && dock.plans().size() == 1, "saved twice by its name, it is one plan");
                    final List<ResourceLocation> longer = new ArrayList<>(ready.spine());
                    for (int k = 0; k < LONG_SHIP; k++) {
                        longer.add(1, part("corridor"));
                    }
                    final ShipDesign tooLong = ready.withSpine(longer, List.of());
                    helper.assertTrue(tooLong.problems(ShipParts.all(level)).isEmpty()
                            && !ShipyardTerminalBlockEntity.ready(level, tooLong), "a ship longer than the pit"
                            + " flies but does not fit the dock");
                    helper.assertTrue(dock.confirm() && dock.plan().orElseThrow().equals(ready),
                            "the ready ship that fits is sent to the dock");
                    final ShipDesign flipped = ready.withSpine(ready.spine(), List.of(1));
                    helper.assertTrue(ShipyardLayout.laid(ShipParts.all(level), ShipKits.all(level), flipped)
                            .cells().size() == ShipyardLayout.laid(ShipParts.all(level), ShipKits.all(level), ready)
                            .cells().size(), "a section flipped left for right lays as many blocks");
                })
                .thenSucceed();
    }

    private static boolean formed(final ServerLevel level, final BlockPos terminal) {
        return level.getBlockEntity(terminal) instanceof ShipyardTerminalBlockEntity dock && dock.formed();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* A place of the dock in its own blocks, across from the bay's middle, up from the ground, along from the front. */
    private static BlockPos dock(final int x, final int y, final int z) {
        return CORNER.offset(x + 11, y + 3, z + 13);
    }
}
