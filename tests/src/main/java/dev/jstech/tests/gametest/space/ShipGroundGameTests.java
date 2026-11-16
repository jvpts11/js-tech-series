/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.space.ship.ShipGround;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a ship meets of the land is met by the whole of each of its blocks, not their middles only: a slab half a block
 * thick, between the middles of two of its blocks one over the other, goes into them, and stops it, however it is
 * turned; standing on the slab, it only rests there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipGroundGameTests {

    private static final String ARENA = "empty";
    /* Two blocks one over the other, the bottom one here; the slab in the place of the top one. */
    private static final BlockPos BOTTOM = new BlockPos(2, 2, 2);
    private static final BlockPos SLAB = BOTTOM.above();
    /* How far up the two blocks are taken, so their middles are a quarter of a block either side of the slab. */
    private static final double UP = 0.25;
    /* How far up they stand on the slab: the slab's top, half a block over its place's bottom. */
    private static final double ON_THE_SLAB = 1.5;
    private static final double TURNED = Math.toRadians(30);

    private ShipGroundGameTests() {
    }

    @GameTest(template = ARENA)
    public static void meet_aSlabBetweenTheMiddlesOfTwoBlocksStopsItAndStandingOnItItRests(
            final GameTestHelper helper) {
        helper.setBlock(BOTTOM, Blocks.STONE);
        helper.setBlock(SLAB, Blocks.STONE);
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(BOTTOM),
                helper.absolutePos(SLAB));
        helper.assertTrue(made.ok(), "the two blocks make a structure");
        final MovingStructure blocks = made.structure();
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(blocks.ready(), "its blocks come into it"))
                .thenExecute(() -> {
                    final StructurePose from = blocks.pose();
                    blocks.teleport(from.movedTo(from.x(), from.y() + UP, from.z()));
                    helper.assertFalse(ShipGround.meet(helper.getLevel(), blocks, blocks.frame()).blocked(),
                            "with nothing where it stands, nothing stops it");
                    helper.setBlock(SLAB, Blocks.STONE_SLAB);
                    helper.assertTrue(ShipGround.meet(helper.getLevel(), blocks, blocks.frame()).blocked(),
                            "a slab between the middles of two of its blocks goes into them and stops it");
                    blocks.teleport(StructurePose.turned(from.x(), from.y() + UP, from.z(), 0, 1, 0, TURNED));
                    helper.assertTrue(ShipGround.meet(helper.getLevel(), blocks, blocks.frame()).blocked(),
                            "turned, the slab still goes into them");
                    blocks.teleport(from.movedTo(from.x(), from.y() + ON_THE_SLAB, from.z()));
                    final ShipGround.Contact standing = ShipGround.meet(helper.getLevel(), blocks, blocks.frame());
                    helper.assertTrue(!standing.blocked() && standing.touching(), "standing on the slab, it rests"
                            + " there: " + standing);
                    MovingStructures.takeAway(blocks);
                }).thenSucceed();
    }
}
