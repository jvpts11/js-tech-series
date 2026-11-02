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
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A system's space has no bottom: a player or a moving structure that goes far under its lowest block, where no world
 * lies under them, is neither hurt nor taken away, and stays where it went, as high up would be.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceBottomGameTests {

    private static final String ARENA = "empty";
    /* A spot of the Solar System's space over no world, far from the Overworld, the Moon and Mars. */
    private static final double X = 0;
    private static final double Z = 15_000;
    /* How deep under the space's lowest block they go, where the game would take away whatever falls. */
    private static final double DEEP = -500;
    private static final double CLOSE = 1;
    private static final BlockPos CRAFT = new BlockPos(2, 2, 2);
    private static final int WATCHED = 60;

    private SpaceBottomGameTests() {
    }

    @GameTest(template = ARENA, batch = "jsspace_bottom", timeoutTicks = 400)
    public static void bottom_letsAPlayerGoFarDownInSpaceUnharmed(final GameTestHelper helper) {
        final ServerLevel space = space(helper);
        helper.assertTrue(SystemLayout.over(X, Z) == null, "no world lies under the spot");
        final ServerPlayer player = GameTestPlayers.connected(helper, "deep_diver");
        // Creative: the void kills a player in creative mode too, while space's air and cold leave them alone.
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(space, X, DEEP, Z, 0, 0);
        helper.startSequence()
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    helper.assertTrue(player.isAlive() && player.level() == space, "the player is still there, in "
                            + player.level().dimension().location());
                    helper.assertTrue(player.getHealth() == player.getMaxHealth(), "unhurt by the void: "
                            + player.getHealth());
                    helper.assertTrue(Math.abs(player.getY() - DEEP) < CLOSE, "where they went: " + player.getY());
                    GameTestPlayers.leave(player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_bottom", timeoutTicks = 3000)
    public static void bottom_letsAMovingStructureGoFarDownInSpaceWhole(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        helper.setBlock(CRAFT, Blocks.IRON_BLOCK);
        final StructureResult made = MovingStructures.assemble(overworld, helper.absolutePos(CRAFT),
                helper.absolutePos(CRAFT));
        helper.assertTrue(made.ok(), "the craft is made");
        final MovingStructure craft = made.structure();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(craft.ready(), "the craft is ready"))
                .thenExecute(() -> helper.assertTrue(MovingStructures.travel(craft, space(helper),
                        new Vec3(X, SystemLayout.ORBIT_Y, Z)).ok(), "the craft is sent into space"))
                .thenWaitUntil(() -> {
                    final MovingStructure there = MovingStructures.byId(space(helper), craft.id());
                    helper.assertTrue(there != null && there.ready(), "the craft is in space");
                })
                .thenExecute(() -> {
                    final MovingStructure there = MovingStructures.byId(space(helper), craft.id());
                    final StructurePose at = there.pose();
                    there.teleport(at.movedTo(at.x(), DEEP, at.z()));
                })
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    final MovingStructure there = MovingStructures.byId(space(helper), craft.id());
                    helper.assertTrue(there != null && !there.entity().isRemoved() && there.blocks() == 1,
                            "far under the space's lowest block the craft is still there, whole");
                    helper.assertTrue(Math.abs(there.pose().y() - DEEP) < CLOSE, "where it went, not put back up: "
                            + there.pose().y());
                    MovingStructures.takeAway(there);
                })
                .thenSucceed();
    }

    private static ServerLevel space(final GameTestHelper helper) {
        return SystemSpaces.spaceOf(helper.getLevel().getServer(), StarSystems.SOLAR_SYSTEM);
    }
}
