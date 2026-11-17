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
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.space.crew.ShipDoorRules;
import dev.jstech.space.ship.DoorMotion;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A starship's holds' doors as the player's game draws them moving, as the approved storyboard has them: the Ramp
 * hold's ramp shut across the tail, swinging down, and lying on the land, seen from behind the ship; the Bay Doors
 * hold's roof shut, its leaves standing up, and lying along the hull's flanks, seen from over it.
 */
public final class ShipHoldDoorsClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 40;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* The camera behind the tail, how far out and how high over the ramp's block, in blocks. */
    private static final double BEHIND = 7.5;
    private static final double OVER = 1.2;
    /* Half its swing, and a little more than all of it, in ticks. */
    private static final int HALF_SWING = (int) Math.round(DoorMotion.SWING * 20 / 2);
    private static final int SWUNG = (int) Math.ceil(DoorMotion.SWING * 20) + 10;
    /* How far two pixels may differ and be the same, in each channel; how many must differ for a change. */
    private static final int SAME = 6;
    private static final int MOVED = 400;
    private static final int CHANNEL = 0xFF;
    private static final ShipDesign RAMP_SHIP = new ShipDesign("JS-H1", List.of(part("orbiter_cockpit"),
            part("airlock"), part("reactor_flush"), part("ramp")), List.of(part("magnetic_nozzle"),
            part("stub_wings"), part("legs")));
    private static final int RAMP_PART = 3;
    private static final ShipDesign BAY_SHIP = new ShipDesign("JS-H2", List.of(part("orbiter_cockpit"),
            part("airlock"), part("bay"), part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"),
            part("twin_canted_fins"), part("wheels")));
    private static final int BAY_PART = 2;
    /* The camera over the hold's roof, how far across the ship from its middle and how high over it, in blocks. */
    private static final double ABOVE_ACROSS = 5.0;
    private static final double ABOVE = 6.0;
    /* A ship whose Side Door hold stands beside its swept wings, the raised one, and its part's number. */
    private static final ShipDesign SIDE_SHIP = new ShipDesign("JS-H3", List.of(part("greenhouse_cockpit"),
            part("airlock"), part("side_door"), part("reactor_flush")), List.of(part("twin_bells"),
            part("swept_wings"), part("wheels")));
    private static final int SIDE_PART = 2;
    /* The camera beside the door, out over the wing and a little up, in blocks. */
    private static final double BESIDE = 4.5;
    private static final double BESIDE_UP = 0.8;
    /*
     * The camera out past the wing's tip and aft of it, low over the land, off the door's block; and where it looks,
     * the middle of the wing's stair let down behind the wing.
     */
    private static final Vec3 BEHIND_THE_WING = new Vec3(5.0, -1.5, 6.5);
    private static final Vec3 AT_THE_STAIR = new Vec3(1.0, -1.5, 3.4);
    /* A ship whose Side Door hold stands ahead of the wings, with its own boarding stair, and its part's number. */
    private static final ShipDesign STAIR_SHIP = new ShipDesign("JS-H4", List.of(part("greenhouse_cockpit"),
            part("side_door"), part("airlock"), part("reactor_flush")), List.of(part("twin_bells"),
            part("swept_wings"), part("wheels")));
    private static final int STAIR_PART = 1;
    /* The camera out from the flank and aft, low over the land; where it looks, the stair let down from the sill. */
    private static final Vec3 OFF_THE_FLANK = new Vec3(5.0, -1.0, 2.0);
    private static final Vec3 AT_THE_SILL_STAIR = new Vec3(1.2, -2.0, -0.5);

    private ShipHoldDoorsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void ramp_swingsDownFromTheTailOntoTheLandAsItOpens(final ClientTestContext ctx) {
        swing(ctx, RAMP_SHIP, RAMP_PART, ShipKitBlocks.RAMP_PLATE.getId(), ShipDoorRules.RAMP, "ship-ramp",
                new Vec3(0, OVER, BEHIND), new Vec3(0, -1, 0));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void bayDoors_swingUpAndOverOntoTheHullsFlanksAsTheyOpen(final ClientTestContext ctx) {
        swing(ctx, BAY_SHIP, BAY_PART, ShipKitBlocks.HULL_PLATING.getId(), ShipDoorRules.BAY_DOORS, "ship-bay-doors",
                new Vec3(ABOVE_ACROSS, ABOVE, 0), Vec3.ZERO);
    }

    @ClientTest(timeoutTicks = 2400)
    public static void sideDoor_rollsUpTheFlankIntoItsDrumAsItOpens(final ClientTestContext ctx) {
        swing(ctx, SIDE_SHIP, SIDE_PART, ShipKitBlocks.RAMP_PLATE.getId(), ShipDoorRules.SIDE_DOOR,
                "ship-side-door", new Vec3(BESIDE, BESIDE_UP, 0), new Vec3(0, BESIDE_UP, 0));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void wingStair_swingsDownBehindTheWingAsTheSideDoorOpens(final ClientTestContext ctx) {
        swing(ctx, SIDE_SHIP, SIDE_PART, ShipKitBlocks.RAMP_PLATE.getId(), ShipDoorRules.SIDE_DOOR,
                "ship-wing-stair", BEHIND_THE_WING, AT_THE_STAIR);
    }

    @ClientTest(timeoutTicks = 2400)
    public static void boardingStair_unfoldsFromUnderTheSideDoorsSillAwayFromTheWings(final ClientTestContext ctx) {
        swing(ctx, STAIR_SHIP, STAIR_PART, ShipKitBlocks.RAMP_PLATE.getId(), ShipDoorRules.SIDE_DOOR,
                "ship-side-stair", OFF_THE_FLANK, AT_THE_SILL_STAIR);
    }

    /*
     * The ship of {@code design} let go on the land, its door {@code piece} found through its part numbered
     * {@code part}'s block {@code block}, seen from {@code eyes} off the door's block, across the ship, up and along it
     * aft, looking at {@code look} off it: drawn shut, half way and open, each screenshot named after {@code name}.
     * Open, it is drawn away from where it stood shut; half way, between the two.
     */
    private static void swing(final ClientTestContext ctx, final ShipDesign design, final int part,
                              final ResourceLocation block, final String piece, final String name, final Vec3 eyes,
                              final Vec3 look) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final FormedDoors.Door[] moving = new FormedDoors.Door[1];
        final int[][] frames = new int[3][];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
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
                .thenWaitUntilServer(level -> ship[0].ready() && ship[0].motion() instanceof ShipFlight flight
                        && flight.grounded(), ARRIVE, "the ship let go to stand on the land", level -> "arriving")
                .thenServer(0, level -> {
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        if (spot.part() != part || !spot.block().equals(block)) {
                            continue;
                        }
                        final BlockPos aboard = BlockPos.containing(MovingStructures.toYard(ship[0],
                                Vec3.atCenterOf(plan[0].where(spot))));
                        for (final FormedPlace place : ((IFormedPlaced) level.getBlockEntity(aboard)).places()) {
                            FormedDoors.named(level, aboard, place, piece).ifPresent(door -> moving[0] = door);
                        }
                    }
                    ctx.assertTrue(moving[0] != null && FormedDoors.shut(level, moving[0]), "its " + piece
                            + " stands shut");
                    final Vec3 there = seen(level, moving[0], eyes);
                    ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(),
                            there.z, 0, 0);
                })
                .then(SETTLE, () -> {
                    ctx.lookAt(ctx.server().submit(() -> seen(built[0], moving[0], look)).join());
                    ctx.mc().options.hideGui = true;
                })
                .then(SETTLE, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, name + "-shut")
                // Used as a player uses it, so what it lets down on the land comes down too.
                .thenServer(0, level -> ctx.assertTrue(FormedDoors.use(level, null, moving[0]), "it opens"))
                .then(HALF_SWING, () -> frames[1] = frame(ctx))
                .thenScreenshot(0, name + "-swinging")
                .then(SWUNG, () -> {
                    frames[2] = frame(ctx);
                    ctx.assertTrue(changed(frames[0], frames[2]) >= MOVED, "open, it is drawn away from where it"
                            + " stood shut: " + changed(frames[0], frames[2]) + " pixels");
                    ctx.assertTrue(changed(frames[0], frames[1]) >= MOVED && changed(frames[1], frames[2]) >= MOVED,
                            "half way, it is drawn between shut and open: " + changed(frames[0], frames[1]) + " and "
                                    + changed(frames[1], frames[2]) + " pixels");
                })
                .thenScreenshot(0, name + "-open");
    }

    /* Where {@code off} is off the block {@code door} works by, in the world: across the ship, up, along it aft. */
    private static Vec3 seen(final ServerLevel level, final FormedDoors.Door door, final Vec3 off) {
        final Vec3 at = Vec3.atCenterOf(door.uses());
        return MovingStructures.inWorld(level, at.add(off.x, 0, off.z)).add(0, off.y, 0);
    }

    /* How many pixels of {@code a} and {@code b} differ. */
    private static int changed(final int[] a, final int[] b) {
        int out = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (differ(a[i], b[i])) {
                out++;
            }
        }
        return out;
    }

    /* The whole frame, its pixels row by row. */
    private static int[] frame(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[width * height];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = frame.pixel(x, y);
                }
            }
        }
        return out;
    }

    /* Whether two pixels differ by more than {@link #SAME} in a channel. */
    private static boolean differ(final int a, final int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((a >> shift & CHANNEL) - (b >> shift & CHANNEL)) > SAME) {
                return true;
            }
        }
        return false;
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
