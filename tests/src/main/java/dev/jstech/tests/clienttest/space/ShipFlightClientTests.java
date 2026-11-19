/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipSounds;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.StandingWatch;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A small ship let go from its dock and flown, on a player's game: sat in its pilot seat, the player sends it on with
 * the key they walk forward with, the world going by through its canopy; getting up from the seat while it flies on,
 * they are drawn on the seat and then where they stand, in one step, never left behind by the ship flying on under
 * them; standing still, they are carried on steady with the deck, and the block they aim at across it, outlined, stays
 * the one drawn under the crosshair; and they walk aft through its airlock, as anyone aboard walks about in flight.
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
    /* How hard a crew member at the helm holds the throttle open while the player is up and about. */
    private static final float CRUISE = 1F;
    /* How high the ship is taken up before it flies, in blocks. */
    private static final double ALOFT = 30;
    /* The way a player faces to walk south, aft along a ship flying north, and to look east, across it. */
    private static final float AFT = 0F;
    private static final float ACROSS = -90F;
    /*
     * How long the player is watched every frame, in ticks; how far from the seat, or from where they got up, they may
     * be drawn getting up, in blocks; and how far their place on the deck may wander as drawn, standing still: the
     * ship goes over a block a tick.
     */
    private static final int STOOD = 20;
    private static final double GOT_UP = 0.3;
    private static final double STEADY = 0.02;
    /* How long the pilot holds left, in ticks, and how far their view turns a tick at least, in degrees. */
    private static final int TURNING = 15;
    private static final float TURN_STEP = 0.5F;
    /* How little a ship turns in a tick and still stands still, in radians; how upright it stands level. */
    private static final double STILL = 1.0e-4;
    private static final double UPRIGHT = 0.9999;

    private ShipFlightClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void flight_thePilotFliesTheShipAndTheCrewWalksInsideInFlight(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] last = new StructurePose[1];
        final float[] turn = new float[1];
        final BlockPos[] seat = new BlockPos[1];
        final Vec3[] from = new Vec3[2];
        final boolean[] cruising = new boolean[1];
        final StandingWatch[] standing = new StandingWatch[1];
        final ShipDesign design = new ShipDesign("JS-03", List.of(part("orbiter_cockpit"), part("airlock"),
                part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"),
                part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"),
                part("wheels")));
        // A crew member at the helm, holding the throttle open while the player is up and about.
        final Consumer<ServerTickEvent.Post> helm = event -> {
            if (cruising[0] && ship[0] != null) {
                ship[0].controls().pilot(ctx.serverLevel().getGameTime(), Map.of(
                        StructureControls.ControlAxis.FORWARD, CRUISE), new Vec3(0, 0, -1), new Vec3(0, 1, 0));
            }
        };
        NeoForge.EVENT_BUS.addListener(helm);
        // The engines' loops heard on each tick of the turn.
        final Set<String> engines = new HashSet<>();
        final Consumer<ClientTickEvent.Post> heardInTurn = event -> SoundDirector.playing().stream()
                .filter(id -> id.contains("engine")).forEach(engines::add);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(helm);
            NeoForge.EVENT_BUS.unregister(heardInTurn);
            if (standing[0] != null) {
                standing[0].stop();
            }
            ctx.mc().options.keyUp.setDown(false);
            ctx.mc().options.keyLeft.setDown(false);
            ctx.mc().options.keyRight.setDown(false);
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
                // Hovering, slow, held left: its engines' loop goes on through the turn, never asked anew.
                .then(SETTLE, () -> {
                    engines.clear();
                    ctx.mc().options.keyLeft.setDown(true);
                    NeoForge.EVENT_BUS.addListener(heardInTurn);
                })
                // And back as far, so its nose points where the flight below begins.
                .then(TURNING * 2, () -> {
                    ctx.mc().options.keyLeft.setDown(false);
                    ctx.mc().options.keyRight.setDown(true);
                })
                .then(TURNING * 2, () -> {
                    ctx.mc().options.keyRight.setDown(false);
                    NeoForge.EVENT_BUS.unregister(heardInTurn);
                    ctx.assertTrue(engines.size() == 1 && engines.iterator().next().contains("|around|"),
                            "turning slowly as it hovers, its engines are one loop, heard all round: " + engines);
                })
                .thenWaitUntilServer(level -> settled(ship[0], last), ARRIVE, "let go, the hovering ship stops "
                        + "turning", level -> ship[0].pose().toString())
                .then(SETTLE, () -> {
                    engines.clear();
                    from[0] = ctx.server().submit(() -> where(ship[0])).join();
                    ctx.mc().options.keyUp.setDown(true);
                })
                .thenWaitUntilServer(level -> from[0].z - ship[0].pose().z() > FLOWN, ARRIVE,
                        "held forward, the ship to fly north along its nose", level -> ship[0].pose().toString())
                .thenScreenshot(0, "ship-flight-helm")
                // At the helm, inside it, its engines are heard all round, from no place a turn would swing round.
                .then(0, () -> {
                    SoundDirector.updateNow();
                    ctx.assertTrue(SoundDirector.playing().stream().anyMatch(id -> id.endsWith("|around|"
                            + ShipSounds.ENGINE_THRUST_INSIDE.id()) || id.endsWith("|around|"
                            + ShipSounds.ENGINE_IDLE_INSIDE.id())), "at the helm its engines are heard all round: "
                            + SoundDirector.playing());
                })
                // Held left, the ship turns and the pilot's view turns with it, frame by frame within each tick as
                // the ship is drawn, never a step at once at the tick.
                .then(0, () -> {
                    ctx.mc().options.keyUp.setDown(false);
                    ctx.mc().options.keyLeft.setDown(true);
                    NeoForge.EVENT_BUS.addListener(heardInTurn);
                })
                .then(TURNING, () -> {
                    final float step = Mth.wrapDegrees(ctx.player().getYRot() - ctx.player().yRotO);
                    ctx.mc().options.keyLeft.setDown(false);
                    NeoForge.EVENT_BUS.unregister(heardInTurn);
                    ctx.assertTrue(Math.abs(step) > TURN_STEP, "turning, the pilot's view turns between its last "
                            + "tick and this one, drawn smoothly: " + step + " degrees");
                    // Its engines' loop goes on through the turn: one asked anew would start again from its start.
                    ctx.assertTrue(engines.size() == 1 && engines.iterator().next().contains("|around|"),
                            "through the turn its engines are one loop, heard all round: " + engines);
                })
                // Let go, its turn eases away and it rolls level before anyone stands up in it.
                .thenWaitUntilServer(level -> settled(ship[0], last), ARRIVE, "let go, the ship stops turning and"
                        + " comes level", level -> ship[0].pose().toString())
                // A crew member takes the helm and holds the throttle open; the player gets up as it flies on, and is
                // drawn first on the seat and then where they stand beside it, in one step, never a frame between.
                .then(0, () -> {
                    ctx.mc().options.keyUp.setDown(false);
                    cruising[0] = true;
                    standing[0] = StandingWatch.of(ship[0], ctx);
                })
                .thenServer(0, level -> ctx.serverPlayer().stopRiding())
                .thenWaitUntil(() -> ctx.player().getVehicle() == null, ARRIVE, "the player to get up in flight")
                .then(STOOD, () -> {
                    standing[0].stop();
                    standing[0].assertNoFrameBetween(GOT_UP);
                    standing[0] = StandingWatch.of(ship[0], ctx);
                })
                // Standing still, they are carried on with the deck, drawn steady on it frame after frame.
                .then(STOOD, () -> {
                    standing[0].stop();
                    standing[0].assertSteady(STEADY);
                    turn[0] = ctx.server().submit(() -> heading(ship[0])).join();
                    ctx.player().setYRot(ACROSS + turn[0]);
                    ctx.player().yRotO = ACROSS + turn[0];
                    ctx.player().setXRot(0);
                    ctx.player().xRotO = 0;
                    standing[0] = StandingWatch.of(ship[0], ctx);
                })
                // Looking across at its side, the block they aim at, outlined, is the same frame after frame.
                .then(STOOD, () -> {
                    standing[0].stop();
                    standing[0].assertAimHeld();
                })
                .thenScreenshot(0, "ship-flight-aim")
                .then(0, () -> {
                    ctx.player().setYRot(AFT + turn[0]);
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
                            "the player walked aft inside the ship, on its floor: from " + from[0] + " to " + now
                            + ", the ship turned " + turn[0] + " degrees");
                    ctx.assertTrue(from[1].distanceTo(where(ship[0])) > 1, "while it flew on: from " + from[1]
                            + " to " + ship[0].pose());
                    ctx.assertTrue(ship[0].worldBox().inflate(0.5).contains(ctx.serverPlayer().position()),
                            "carried with it, inside its box");
                });
    }

    /* Whether {@code ship} has turned no more since {@code last} held its pose, a tick ago, and stands level. */
    private static boolean settled(final MovingStructure ship, final StructurePose[] last) {
        final StructurePose now = ship.pose();
        final boolean still = last[0] != null && now.angleTo(last[0]) < STILL;
        last[0] = now;
        // Its up, as far as it stands up in the world's: one less twice the squares of its tilt.
        return still && 1 - 2 * (now.qx() * now.qx() + now.qz() * now.qz()) > UPRIGHT;
    }

    /*
     * How far {@code ship} has turned from the way its yard faces, as a player's view turns, in degrees: so a view
     * set aft or across in its yard is set aft or across in the ship, wherever it heads.
     */
    private static float heading(final MovingStructure ship) {
        final Vec3 here = where(ship);
        final Vec3 at = MovingStructures.toYard(ship, here);
        // Its yard's south in the world: how far a step east and a step south in the world go south in its yard.
        final double east = MovingStructures.toYard(ship, here.add(1, 0, 0)).z - at.z;
        final double south = MovingStructures.toYard(ship, here.add(0, 0, 1)).z - at.z;
        return (float) Math.toDegrees(Math.atan2(-east, south));
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
