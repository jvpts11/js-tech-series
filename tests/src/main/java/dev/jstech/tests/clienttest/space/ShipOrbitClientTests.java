/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.dimension.TravelScreens;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A small ship climbing to space on a player's game, the player its pilot at its helm: pitched up and flying on, it
 * climbs over the line where the Overworld becomes its space and crosses into it with no loading screen at all, the
 * player still sitting at its helm on the other side, the ship drawn round them there.
 */
public final class ShipOrbitClientTests {

    private static final ResourceKey<Level> SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* How far under the line the ship waits, in blocks; how far its nose is pitched up, in degrees; its cruise. */
    private static final int UNDER_THE_LINE = 40;
    private static final double CLIMBING_PITCH = 60;
    private static final double CRUISE = 40;
    private static final ShipDesign SHIP = new ShipDesign("JS-06", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
            part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipOrbitClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void orbit_itClimbsIntoSpaceWithNoLoadingScreenItsPilotAtTheHelm(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final int[] plot = new int[1];
        final boolean[] screen = new boolean[1];
        final int[] passages = new int[1];
        // From its launch on, the least air its pilot had and the most cold they took, each tick on the server.
        final boolean[] watching = new boolean[1];
        final int[] breath = {Integer.MAX_VALUE, 0};
        final Consumer<ServerTickEvent.Post> watch = event -> {
            if (watching[0]) {
                breath[0] = Math.min(breath[0], ctx.serverPlayer().getAirSupply());
                breath[1] = Math.max(breath[1], ctx.serverPlayer().getTicksFrozen());
            }
        };
        NeoForge.EVENT_BUS.addListener(watch);
        ctx.afterTest(() -> NeoForge.EVENT_BUS.unregister(watch));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().stopRiding();
            if (ship[0] != null) {
                for (final ServerLevel level : List.of(ctx.server().overworld(), space(ctx))) {
                    final MovingStructure found = MovingStructures.byId(level, ship[0].id());
                    if (found != null) {
                        MovingStructures.takeAway(found);
                    }
                }
            }
            land(ctx);
            if (built[0] != null) {
                clear(ctx, built[0]);
            }
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
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
                            plan[0].where(first(plan[0], ShipKitBlocks.PILOT_SEAT.getId())))));
                    // Taken up under the line to space, hovering there while the player takes its helm.
                    final StructurePose now = ship[0].pose();
                    ship[0].teleport(now.movedTo(now.x(), SpaceClimbs.ceiling(level) - UNDER_THE_LINE, now.z()));
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                    ctx.assertTrue(MovingStructures.sit(ctx.serverPlayer(), seat), "the player sits at its helm");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity
                        && drawn(ship[0].plot()), ARRIVE, "the player to sit at the helm of the ship drawn round them")
                .thenWaitUntilServer(level -> SealedRooms.roomBeside(level, ((ShipFlight) ship[0].motion())
                        .consoleIn(ship[0]).orElseThrow()).isPresent(), ARRIVE, "its cockpit to be a sealed room",
                        level -> "no room by its console")
                .then(SETTLE, () -> passages[0] = TravelScreens.seamlessPassages())
                .thenServer(0, level -> {
                    // Its nose pitched up and its throttle open at its cruise: it climbs on into space.
                    watching[0] = true;
                    ship[0].teleport(ship[0].pose().turnedBy(1, 0, 0, Math.toRadians(CLIMBING_PITCH)));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(new ShipFlightModel.State(
                            CRUISE, 0, 1, 1, 0, 0)));
                })
                .thenWaitUntil(() -> arrived(ctx, screen), ARRIVE, "the ship and its pilot to cross into space")
                .thenServer(0, level -> plot[0] = MovingStructures.byId(space(ctx), ship[0].id()).plot())
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity && drawn(plot[0]),
                        ARRIVE, "the player to sit at its helm still, the ship drawn round them in space")
                .then(SETTLE, () -> ctx.assertTrue(!screen[0] && TravelScreens.seamlessPassages() > passages[0],
                        "the passage opened no loading screen: a screen seen " + screen[0] + ", seamless passages "
                                + passages[0] + " to " + TravelScreens.seamlessPassages()))
                .thenServer(0, level -> ctx.assertTrue(breath[0] == ctx.serverPlayer().getMaxAirSupply()
                        && breath[1] == 0, "its pilot breathed its cockpit's air all the way up and across, never short"
                        + " of it, never cold: least air " + breath[0] + ", most cold " + breath[1]))
                .thenScreenshot(0, "ship-orbit");
    }

    /* Whether the player's game is in the Solar System's space; any loading screen seen on the way is noted. */
    private static boolean arrived(final ClientTestContext ctx, final boolean[] screen) {
        if (ctx.mc().screen instanceof ReceivingLevelScreen) {
            screen[0] = true;
        }
        return ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE);
    }

    /* Whether the ship in the yard's plot {@code plot} is drawn whole on the player's game. */
    private static boolean drawn(final int plot) {
        final ClientStructure seen = ClientMovingStructures.byPlot(plot);
        return seen != null && seen.ready() && seen.meshes().built() > 0 && !seen.meshes().waiting();
    }

    /* Puts the player back on the ground of the test's place, as the next test expects them. */
    private static void land(final ClientTestContext ctx) {
        final ServerPlayer player = ctx.serverPlayer();
        final BlockPos origin = ctx.origin().above(2);
        player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
    }

    private static ServerLevel space(final ClientTestContext ctx) {
        return SystemSpaces.spaceOf(ctx.server(), StarSystems.SOLAR_SYSTEM);
    }

    /* The first place of {@code plan} that wants {@code block}. */
    private static PlanSpot first(final PlannedBuild plan, final ResourceLocation block) {
        return plan.plan().spots().stream().filter(each -> each.block().equals(block)).findFirst().orElseThrow();
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
