/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A ship built whole in a Small Shipyard and let go, on a player's game: from then on a moving structure, its blocks
 * gone from the bay into it, it is drawn as before, every part in its model, now from its flight console aboard. Got
 * up from its helm and sent away by a command, the player is where they were sent.
 */
public final class ShipReleaseClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The half-built ship's wide camera, in the dock's own places. */
    private static final Vec3 EYES = new Vec3(-7.4, 15.6, -12.4);
    private static final Vec3 AT = new Vec3(0, 3.0, -0.5);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    private static final ShipDesign HELM_SHIP = new ShipDesign("JS-08", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipReleaseClientTests() {
    }

    @ClientTest(timeoutTicks = 2000)
    public static void release_theShipLetGoIsStillDrawnFromItsConsole(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final ShipDesign design = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
                part("bay"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (built[0] != null) {
                clear(ctx, built[0]);
                built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
            }
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(design);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> !ship[0].arriving(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving");
        stand(ctx, EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, AT)))
                .thenScreenshot(SETTLE, "ship-released")
                .then(0, () -> ctx.assertTrue(ctx.mc().level.getBlockEntity(terminal[0])
                        instanceof ShipyardTerminalBlockEntity dock && dock.plan().isEmpty(), "the dock has let"
                        + " its ship go"));
    }

    /*
     * Got up from the helm of a ship let go and standing in its dock, then sent away by a command, the player is where
     * the command sent them, on the server and on their game, and stays there: the server no longer holds them where
     * they got up.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void helm_gotUpAndSentAwayThePlayerIsWhereTheyWereSent(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final Vec3[] away = new Vec3[1];
        final KeyMapping crouch = ctx.mc().options.keyShift;
        ctx.afterTest(() -> {
            crouch.setDown(false);
            ctx.server().execute(() -> {
                ctx.serverPlayer().stopRiding();
                if (ship[0] != null && !ship[0].entity().isRemoved()) {
                    MovingStructures.takeAway(ship[0]);
                }
                if (built[0] != null) {
                    clear(ctx, built[0]);
                }
                ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(HELM_SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> ship[0].ready(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving")
                .thenServer(0, level -> {
                    final BlockPos seat = BlockPos.containing(MovingStructures.toYard(ship[0], Vec3.atCenterOf(
                            plan[0].where(plan[0].plan().spots().stream().filter(each -> each.block().equals(
                                    ShipKitBlocks.PILOT_SEAT.getId())).findFirst().orElseThrow()))));
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                    ctx.assertTrue(MovingStructures.sit(ctx.serverPlayer(), seat), "the player sits at its helm");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity, ARRIVE,
                        "the player to sit at its helm on their game")
                .then(SETTLE, () -> crouch.setDown(true))
                .thenWaitUntil(() -> !(ctx.player().getVehicle() instanceof StructureSeatEntity), ARRIVE,
                        "the player to get up from the helm, crouching")
                .then(0, () -> crouch.setDown(false))
                .thenServer(SETTLE, level -> {
                    away[0] = Vec3.atBottomCenterOf(ctx.origin().above(2));
                    ctx.serverPlayer().teleportTo(level, away[0].x, away[0].y, away[0].z, 0, 0);
                })
                .thenServer(SETTLE, level -> {
                    final ServerPlayer player = ctx.serverPlayer();
                    ctx.assertTrue(player.position().distanceTo(away[0]) < 1, "sent away, the player is where they"
                            + " were sent on the server, " + away[0] + ", not held where they got up: "
                            + player.position());
                })
                .then(0, () -> ctx.assertTrue(ctx.player().position().distanceTo(away[0]) < 1, "and on their game: "
                        + ctx.player().position()));
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float over the dock");
    }

    /* The world's point of the dock's place {@code at}: across from the bay's middle, up from the ground, along. */
    private static Vec3 place(final ClientTestContext ctx, final Vec3 at) {
        return Vec3.atLowerCornerOf(ctx.abs(CORNER.offset(11, 3, 13))).add(at);
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int y = Y_BOTTOM; y <= Y_TOP; y++) {
                    final Block block = y == Y_BOTTOM ? Blocks.BEDROCK : y < Y_GRASS ? Blocks.DIRT
                            : y == Y_GRASS ? Blocks.GRASS_BLOCK : Blocks.AIR;
                    level.setBlock(ctx.abs(new BlockPos(x, y, z)), block.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
