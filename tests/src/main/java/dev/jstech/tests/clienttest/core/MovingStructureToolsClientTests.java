/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.client.moving.ForceDiagramScreen;
import dev.jstech.core.client.moving.StructureForcesClient;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.jstechseries.tools.PhysicsTool;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureForcesPayload;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.tests.TestPhysics;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

/**
 * Seeing and moving moving structures as a player does: the diagram of forces opens on a raft held up by its
 * levitators and draws it with the forces on it; the physics tool grabs the raft, its panel shown and its beam drawn to
 * the point it holds, and the raft rises towards where it is held; a block's tooltip tells its physics while the tool
 * is held; and the overlay shows the forces of the structures near. Screenshots show each.
 */
public final class MovingStructureToolsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int WAIT = 400;
    private static final int CLEAR = 12_000;
    /* The scene: from here to there, cleared before and after. */
    private static final BlockPos SCENE_FROM = new BlockPos(-6, 2, -16);
    private static final BlockPos SCENE_TO = new BlockPos(6, 14, -4);

    private MovingStructureToolsClientTests() {
    }

    @ClientTest(timeoutTicks = 1600)
    public static void forces_theDiagramDrawsARaftAndTheForcesOnIt(final ClientTestContext ctx) {
        final MovingStructure[] raft = new MovingStructure[1];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> raft[0] = raft(ctx, level))
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> drawn(raft[0]), WAIT, "the raft to reach the player's game")
                .then(0, () -> ctx.mc().setScreen(new ForceDiagramScreen(seen(raft[0]))))
                .thenWaitUntil(() -> told(raft[0]) != null, WAIT, "the server to tell the raft's forces")
                .then(10, () -> {
                    ctx.assertTrue(ctx.mc().screen instanceof ForceDiagramScreen, "the diagram stays open");
                    final StructureForcesPayload told = told(raft[0]);
                    ctx.assertTrue(told.mass() > 0, "it is told the raft's mass: " + told.mass());
                    ctx.assertTrue(told.of(ForceSum.ForceKind.GRAVITY).y < 0, "and its weight: " + told.forces());
                })
                .thenScreenshot(0, "forces-diagram")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenWaitUntil(() -> !StructureForcesClient.looked().contains(seen(raft[0]).entity().getId()), WAIT,
                        "closed, the diagram to stop looking at the raft");
    }

    @ClientTest(timeoutTicks = 1600)
    public static void tool_grabsARaftWithItsPanelAndBeamShown(final ClientTestContext ctx) {
        final MovingStructure[] raft = new MovingStructure[1];
        final double[] start = new double[1];
        ctx.afterTest(() -> {
            ctx.mc().options.keyUse.setDown(false);
            ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> raft[0] = raft(ctx, level))
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenGive(0, new ItemStack(PhysicsTool.ITEM.get()))
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> drawn(raft[0]), WAIT, "the raft to reach the player's game")
                .then(0, () -> {
                    ctx.selectHotbar(0);
                    start[0] = raft[0].worldBox().minY;
                    ctx.lookAt(seen(raft[0]).worldBox().getCenter());
                })
                .thenScreenshot(5, "physics-tool-panel")
                .then(0, () -> {
                    final PhysicsTool.Aim here = PhysicsTool.aim(ctx.player());
                    final String there = ctx.server().submit(() -> {
                        final PhysicsTool.Aim aim = PhysicsTool.aim(ctx.serverPlayer());
                        return (aim == null ? "nothing" : aim.at().toString()) + " from "
                                + ctx.serverPlayer().getEyePosition() + " looking " + ctx.serverPlayer().getLookAngle();
                    }).join();
                    ctx.assertTrue(here != null, "the player's game aims at the raft");
                    ctx.assertTrue(!there.startsWith("nothing"), "and so does the server: " + there + "; the player's"
                            + " game " + here.at() + " from " + ctx.player().getEyePosition() + " looking "
                            + ctx.player().getLookAngle());
                    ctx.mc().options.keyUse.setDown(true);
                })
                .thenWaitUntil(() -> PhysicsTool.grabbed(ctx.player()) != null && ctx.player().isUsingItem(), WAIT,
                        "the tool to grab the raft")
                .then(0, () -> ctx.lookAt(ctx.player().getEyePosition().add(ctx.player().getLookAngle())
                        .add(0, 0.5, 0)))
                .thenScreenshot(15, "physics-tool-holding")
                .then(0, () -> {
                    final double now = ctx.server().submit(() -> raft[0].worldBox().minY).join();
                    final boolean held = ctx.server().submit(() -> {
                        final StructurePhysics physics = raft[0].physics();
                        return physics != null && physics.heldFromOutside();
                    }).join();
                    ctx.assertTrue(held, "the server holds the raft from outside");
                    ctx.assertTrue(now > start[0] + 0.3, "and lifts it where the player looks: " + now + " from "
                            + start[0]);
                    ctx.mc().options.keyUse.setDown(false);
                })
                .thenWaitUntil(() -> PhysicsTool.grabbed(ctx.player()) == null, WAIT, "let go, it holds nothing")
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT))
                .thenScreenshot(5, "physics-tool-third-person")
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON));
    }

    @ClientTest(timeoutTicks = 400)
    public static void tooltip_tellsABlocksPhysicsWhileTheToolIsHeld(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenGive(0, new ItemStack(PhysicsTool.ITEM.get()))
                .then(2, () -> ctx.selectHotbar(0))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(PhysicsTool.ITEM.get()), WAIT,
                        "the tool to be in the hand")
                .then(0, () -> {
                    final List<Component> lines = new ItemStack(Items.STONE).getTooltipLines(
                            Item.TooltipContext.of(ctx.mc().level), ctx.player(), TooltipFlag.NORMAL);
                    final String said = lines.stream().map(Component::getString).toList().toString();
                    final String mass = "Mass: " + UnitFormatter.forCurrentLocale().decimal(2.7) + " t";
                    ctx.assertTrue(said.contains("In a moving structure:") && said.contains(mass),
                            "stone's tooltip tells its mass, " + mass + ": " + said);
                });
    }

    @ClientTest(timeoutTicks = 1600)
    public static void overlay_showsTheForcesOfTheStructuresNear(final ClientTestContext ctx) {
        final MovingStructure[] raft = new MovingStructure[1];
        final boolean[] on = {false};
        ctx.afterTest(() -> {
            if (on[0]) {
                KeyActionsClient.press(CoreKeys.SHOW_STRUCTURE_DEBUG);
            }
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> raft[0] = raft(ctx, level))
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> drawn(raft[0]), WAIT, "the raft to reach the player's game")
                .then(0, () -> {
                    ctx.lookAt(seen(raft[0]).worldBox().getCenter());
                    KeyActionsClient.press(CoreKeys.SHOW_STRUCTURE_DEBUG);
                    on[0] = true;
                })
                .thenWaitUntil(() -> told(raft[0]) != null, WAIT, "the overlay to be told the raft's forces")
                .thenScreenshot(10, "physics-overlay")
                .then(0, () -> {
                    KeyActionsClient.press(CoreKeys.SHOW_STRUCTURE_DEBUG);
                    on[0] = false;
                })
                .thenWaitUntil(() -> !StructureForcesClient.looked().contains(seen(raft[0]).entity().getId()), WAIT,
                        "turned off, the overlay to stop looking");
    }

    /* A raft of planks held up by a levitator at each end, in front of where the player stands. */
    private static MovingStructure raft(final ClientTestContext ctx, final ServerLevel level) {
        clear(ctx);
        level.setWeatherParameters(CLEAR, 0, false, false);
        for (int x = -2; x <= 2; x++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 5, -9)), Math.abs(x) == 2
                    ? TestPhysics.HOVER.get().defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
        }
        final MovingStructure made = MovingStructures.assemble(level, ctx.abs(new BlockPos(-2, 5, -9)),
                ctx.abs(new BlockPos(2, 5, -9))).structure();
        made.move(PhysicsMotion.AT_REST);
        return made;
    }

    /* The raft as the player's game has it. */
    private static ClientStructure seen(final MovingStructure structure) {
        return ClientMovingStructures.byPlot(structure.plot());
    }

    /* The forces last told of the raft, or null. */
    private static StructureForcesPayload told(final MovingStructure structure) {
        final ClientStructure seen = seen(structure);
        return seen == null ? null : StructureForcesClient.of(seen.entity().getId());
    }

    /* Whether the structure has reached the player's game and been built. */
    private static boolean drawn(final MovingStructure structure) {
        final ClientStructure seen = structure == null ? null : seen(structure);
        return seen != null && seen.ready() && seen.meshes().built() > 0 && !seen.meshes().waiting();
    }

    /* Takes every structure away and clears the scene. */
    private static void clear(final ClientTestContext ctx) {
        final ServerLevel level = ctx.serverLevel();
        for (final IStructureBody left : List.copyOf(LevelMovingStructures.of(level).all())) {
            if (left instanceof MovingStructure structure) {
                MovingStructures.takeAway(structure);
            }
        }
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(SCENE_FROM), ctx.abs(SCENE_TO))) {
            if (!level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            }
        }
    }
}
