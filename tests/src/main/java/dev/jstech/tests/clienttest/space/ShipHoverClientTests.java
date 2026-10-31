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
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A small ship on feet rising off its dock's cradle and hovering, on a player's game, as the approved landing on feet
 * draws it: its air engines' nozzles turned down blow their jets at the ground, the dust under it is blown out, and
 * its gear is down; flown high and fast on its wings, its gear goes up, drawn stowed.
 */
public final class ShipHoverClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The hovering ship's camera, in the dock's own places: off its nose to the left, low, as the mock looks at it. */
    private static final Vec3 EYES = new Vec3(-6, 4, -15);
    private static final Vec3 AT = new Vec3(0, 3, -1.5);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* How long its pilot holds up, in ticks; how high it is then flown, in blocks, and how fast, in metres a second. */
    private static final int RISING = 50;
    private static final double HIGH = 80;
    private static final double FAST = 30;
    private static final ShipDesign SHIP = new ShipDesign("JS-05", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
            part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));

    private ShipHoverClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void hover_itsNozzlesBlowDownTheDustRisesAndItsGearGoesUpHighAndFast(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final boolean[] rising = new boolean[1];
        final boolean[] piloted = new boolean[1];
        // Its pilot at the helm: holding up a while, then nothing, the ship hovering.
        final Consumer<ServerTickEvent.Post> helm = event -> {
            if (piloted[0] && ship[0] != null) {
                ship[0].controls().pilot(ctx.serverLevel().getGameTime(), rising[0]
                        ? Map.of(StructureControls.ControlAxis.UP, 1F) : Map.of(), new Vec3(0, 0, -1),
                        new Vec3(0, 1, 0));
            }
        };
        NeoForge.EVENT_BUS.addListener(helm);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(helm);
            ctx.server().execute(() -> {
                if (ship[0] != null) {
                    MovingStructures.takeAway(ship[0]);
                }
                if (built[0] != null) {
                    clear(ctx, built[0]);
                    built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> ship[0].ready(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving");
        stand(ctx, EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, AT)))
                .thenServer(SETTLE, level -> {
                    piloted[0] = true;
                    rising[0] = true;
                })
                .thenServer(RISING, level -> rising[0] = false)
                .thenWaitUntil(() -> shown(ctx, ship[0]).lifting() && shown(ctx, ship[0]).gearDown(), ARRIVE,
                        "its console to show it hovering on its nozzles, its gear down on the player's game")
                .thenScreenshot(SETTLE, "ship-hover")
                .thenServer(0, level -> {
                    // Flown high and fast on its wings, clear of the land.
                    ship[0].teleport(ship[0].pose().movedTo(ship[0].pose().x(), ship[0].pose().y() + HIGH,
                            ship[0].pose().z()));
                    final ShipFlight flight = (ShipFlight) ship[0].motion();
                    MovingStructures.move(ship[0], flight.with(new ShipFlightModel.State(FAST, 0, FAST / 40, 1, 0,
                            0)));
                })
                .thenWaitUntil(() -> !shown(ctx, ship[0]).gearDown() && !shown(ctx, ship[0]).lifting(), ARRIVE,
                        "high and fast, its gear to go up on the player's game");
    }

    /* What {@code ship}'s console shows on the player's game. */
    private static ShipReadings shown(final ClientTestContext ctx, final MovingStructure ship) {
        final BlockPos console = ((ShipFlight) ship.motion()).console().orElseThrow();
        return ctx.mc().level.getBlockEntity(console) instanceof AirReserveEntity keeper ? keeper.flight()
                : ShipReadings.PARKED;
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float by the dock");
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
