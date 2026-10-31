/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
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
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A small ship let go from its dock and flown, on a player's game: sat in its pilot seat, the player sends it on with
 * the key they walk forward with, the world going by through its canopy; up from the seat while it flies on, they walk
 * aft through its airlock, carried with it, as anyone aboard walks about inside in flight.
 */
public final class ShipFlightClientTests {

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
    /* How long the player walks aft, in ticks. */
    private static final int WALK = 15;
    /* How far the ship flies on with its pilot's key held, and how far its crew walks aft, at least, in blocks. */
    private static final double FLOWN = 6;
    private static final double WALKED = 2;
    /* How high over its floor layer's bottom a ship's floor is, and a little more, where a player is stood. */
    private static final double FLOOR = 0.45;
    /* How hard a crew member at the helm holds the throttle open while the player walks aft. */
    private static final float CRUISE = 1F;
    /* How high the ship is taken up before it flies, in blocks; how slow it is braked to, in metres a second. */
    private static final double ALOFT = 30;
    private static final double SLOW = 1;
    /* The way a player faces to walk south, aft along a ship flying north. */
    private static final float AFT = 0F;
    /*
     * How long the player stands still on the deck in flight, watched every frame, in ticks; and how far their place on
     * it may wander as drawn, in blocks: the ship goes nearly half a block a tick.
     */
    private static final int STOOD = 20;
    private static final double STEADY = 0.02;

    private ShipFlightClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void flight_thePilotFliesTheShipAndTheCrewWalksInsideInFlight(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final BlockPos[] seat = new BlockPos[1];
        final Vec3[] airlock = new Vec3[1];
        final Vec3[] from = new Vec3[2];
        final boolean[] cruising = new boolean[1];
        final boolean[] braking = new boolean[1];
        final StandingWatch[] standing = new StandingWatch[1];
        final ShipDesign design = new ShipDesign("JS-03", List.of(part("orbiter_cockpit"), part("airlock"),
                part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"),
                part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"),
                part("wheels")));
        // A crew member at the helm, braking it, then holding the throttle open while the player is up and about.
        final Consumer<ServerTickEvent.Post> helm = event -> {
            if ((cruising[0] || braking[0]) && ship[0] != null) {
                ship[0].controls().pilot(ctx.serverLevel().getGameTime(), Map.of(braking[0]
                        ? StructureControls.ControlAxis.BACK : StructureControls.ControlAxis.FORWARD, CRUISE),
                        new Vec3(0, 0, -1), new Vec3(0, 1, 0));
            }
        };
        NeoForge.EVENT_BUS.addListener(helm);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(helm);
            if (standing[0] != null) {
                standing[0].stop();
            }
            ctx.mc().options.keyUp.setDown(false);
            ctx.server().execute(() -> {
                ctx.serverPlayer().stopRiding();
                if (ship[0] != null) {
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
                    dock.design(design);
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
                    seat[0] = BlockPos.containing(MovingStructures.toYard(ship[0], Vec3.atCenterOf(plan[0].where(
                            first(plan[0], ShipKitBlocks.PILOT_SEAT.getId())))));
                    // The airlock's middle, on the ship's centre line, a block aft of its cycler's row.
                    final BlockPos cycler = plan[0].where(first(plan[0], ShipKitBlocks.AIRLOCK_CYCLER.getId()));
                    airlock[0] = MovingStructures.toYard(ship[0], new Vec3(cycler.getX() + 2.0,
                            cycler.getY() - 1 + FLOOR, cycler.getZ() + 1.5));
                    // Taken straight up, as its taking off will, to fly clear of its dock.
                    ship[0].teleport(ship[0].pose().movedTo(ship[0].pose().x(), ship[0].pose().y() + ALOFT,
                            ship[0].pose().z()));
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                    ctx.assertTrue(MovingStructures.sit(ctx.serverPlayer(), seat[0]), "the player sits in the pilot"
                            + " seat");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity, ARRIVE,
                        "the player to sit at the helm on their game")
                .thenWaitUntil(() -> drawn(ship[0]), ARRIVE, "the ship to be drawn on the player's game")
                .then(SETTLE, () -> {
                    from[0] = ctx.server().submit(() -> where(ship[0])).join();
                    ctx.mc().options.keyUp.setDown(true);
                })
                .thenWaitUntilServer(level -> from[0].z - ship[0].pose().z() > FLOWN, ARRIVE,
                        "held forward, the ship to fly north along its nose", level -> ship[0].pose().toString())
                .thenScreenshot(0, "ship-flight-helm")
                .then(0, () -> ctx.mc().options.keyUp.setDown(false))
                // The crew member at the helm brakes the ship nearly to a stop, so the player put in its airlock
                // lands where their game has it, then opens the throttle again with them aboard.
                .then(0, () -> braking[0] = true)
                .thenWaitUntilServer(level -> ((ShipFlight) ship[0].motion()).state().speed() < SLOW, ARRIVE,
                        "the ship to slow", level -> String.valueOf(((ShipFlight) ship[0].motion()).state()))
                .thenServer(0, level -> {
                    ctx.serverPlayer().stopRiding();
                    final Vec3 stand = MovingStructures.inWorld(level, airlock[0]);
                    ctx.serverPlayer().teleportTo(level, stand.x, stand.y, stand.z, AFT, 0);
                    braking[0] = false;
                    cruising[0] = true;
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() == null, ARRIVE, "the player to be up from the seat")
                .then(SETTLE, () -> standing[0] = StandingWatch.of(ship[0], ctx))
                .then(STOOD, () -> {
                    standing[0].stop();
                    standing[0].assertSteady(STEADY);
                })
                .then(0, () -> {
                    ctx.player().setYRot(AFT);
                    ctx.player().setXRot(0);
                    from[0] = ctx.server().submit(() -> MovingStructures.toYard(ship[0], ctx.serverPlayer()
                            .position())).join();
                    from[1] = ctx.server().submit(() -> where(ship[0])).join();
                    ctx.mc().options.keyUp.setDown(true);
                })
                .thenScreenshot(WALK, "ship-flight-walking")
                .then(0, () -> ctx.mc().options.keyUp.setDown(false))
                .thenServer(SETTLE, level -> {
                    final Vec3 now = MovingStructures.toYard(ship[0], ctx.serverPlayer().position());
                    ctx.assertTrue(now.z - from[0].z > WALKED && Math.abs(now.y - from[0].y) < 0.5,
                            "the player walked aft inside the ship, on its floor: from " + from[0] + " to " + now);
                    ctx.assertTrue(from[1].z - ship[0].pose().z() > 1, "while it flew on north: from " + from[1]
                            + " to " + ship[0].pose());
                    ctx.assertTrue(ship[0].worldBox().inflate(0.5).contains(ctx.serverPlayer().position()),
                            "carried with it, inside its box");
                });
    }

    /* Where the ship's pivot is in the world. */
    private static Vec3 where(final MovingStructure ship) {
        return new Vec3(ship.pose().x(), ship.pose().y(), ship.pose().z());
    }

    /* Whether the ship has reached the player's game and been built there. */
    private static boolean drawn(final MovingStructure ship) {
        final ClientStructure seen = ship == null ? null : ClientMovingStructures.byPlot(ship.plot());
        return seen != null && seen.ready() && seen.meshes().built() > 0 && !seen.meshes().waiting();
    }

    /* The first place of {@code plan} that wants {@code block}. */
    private static PlanSpot first(final PlannedBuild plan, final ResourceLocation block) {
        return plan.plan().spots().stream().filter(each -> each.block().equals(block)).findFirst().orElseThrow();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /*
     * Where the player is drawn on the ship, every frame the player's game draws: their place as drawn, between their
     * last two ticks, in the yard of the ship as drawn then.
     */
    private static final class StandingWatch {

        private final int plot;
        private final ClientTestContext ctx;
        private final List<Vec3> onIt = new ArrayList<>();
        private final Consumer<RenderFrameEvent.Post> listener = this::frame;

        private StandingWatch(final int plotOf, final ClientTestContext test) {
            this.plot = plotOf;
            this.ctx = test;
        }

        /** Starts watching the player on {@code ship}, every frame. */
        static StandingWatch of(final MovingStructure ship, final ClientTestContext test) {
            final StandingWatch watch = new StandingWatch(ship.plot(), test);
            NeoForge.EVENT_BUS.addListener(watch.listener);
            return watch;
        }

        /** Stops watching. */
        void stop() {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        /** Asserts the player kept to one place on the ship, as drawn, within {@code most} blocks. */
        void assertSteady(final double most) {
            ctx.assertTrue(onIt.size() > 3, "the player was drawn in " + onIt.size() + " frames");
            final Vec3 first = onIt.getFirst();
            double furthest = 0;
            for (final Vec3 each : onIt) {
                furthest = Math.max(furthest, each.distanceTo(first));
            }
            ctx.assertTrue(furthest < most, "standing still on the deck in flight, the player is drawn steady on it,"
                    + " frame after frame: they wandered " + furthest + " blocks on it in " + onIt.size() + " frames");
        }

        private void frame(final RenderFrameEvent.Post event) {
            final ClientStructure seen = ClientMovingStructures.byPlot(plot);
            if (seen == null || ctx.player() == null) {
                return;
            }
            final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            final StructureFrame frame = new StructureFrame(seen.drawnAt(partial), seen.pivotX(), seen.pivotY(),
                    seen.pivotZ());
            final Vec3 at = ctx.player().getPosition(partial);
            onIt.add(new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z),
                    frame.yardZ(at.x, at.y, at.z)));
        }
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
