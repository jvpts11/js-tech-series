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
 * hold's ramp shut across the tail, swinging down, and lying on the land, seen from behind the ship.
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

    private ShipHoldDoorsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void ramp_swingsDownFromTheTailOntoTheLandAsItOpens(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final FormedDoors.Door[] ramp = new FormedDoors.Door[1];
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
                    dock.design(RAMP_SHIP);
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
                    final PlanSpot spot = plan[0].plan().spots().stream().filter(each -> each.part() == RAMP_PART
                            && each.block().equals(ShipKitBlocks.RAMP_PLATE.getId())).findFirst().orElseThrow();
                    final BlockPos aboard = BlockPos.containing(MovingStructures.toYard(ship[0],
                            Vec3.atCenterOf(plan[0].where(spot))));
                    for (final FormedPlace place : ((IFormedPlaced) level.getBlockEntity(aboard)).places()) {
                        FormedDoors.named(level, aboard, place, ShipDoorRules.RAMP).ifPresent(door -> ramp[0] = door);
                    }
                    ctx.assertTrue(ramp[0] != null && FormedDoors.shut(level, ramp[0]), "its ramp stands shut");
                    // Behind the tail, looking at the ramp's block.
                    final Vec3 tail = MovingStructures.inWorld(level, Vec3.atCenterOf(ramp[0].uses()));
                    final Vec3 aft = MovingStructures.inWorld(level, Vec3.atCenterOf(ramp[0].uses()).add(0, 0, 1))
                            .subtract(tail).normalize();
                    final Vec3 eyes = tail.add(aft.scale(BEHIND)).add(0, OVER, 0);
                    ctx.serverPlayer().teleportTo(level, eyes.x, eyes.y - ctx.serverPlayer().getEyeHeight(), eyes.z,
                            0, 0);
                })
                .then(SETTLE, () -> {
                    final Vec3 tail = ctx.server().submit(() -> MovingStructures.inWorld(built[0],
                            Vec3.atCenterOf(ramp[0].uses()))).join();
                    ctx.lookAt(tail.subtract(0, 1, 0));
                    ctx.mc().options.hideGui = true;
                })
                .then(SETTLE, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, "ship-ramp-shut")
                .thenServer(0, level -> FormedDoors.set(level, ramp[0], false))
                .then(HALF_SWING, () -> frames[1] = frame(ctx))
                .thenScreenshot(0, "ship-ramp-swinging")
                .then(SWUNG, () -> {
                    frames[2] = frame(ctx);
                    ctx.assertTrue(changed(frames[0], frames[2]) >= MOVED, "open, the ramp is drawn away from the"
                            + " tail, down on the land: " + changed(frames[0], frames[2]) + " pixels");
                    ctx.assertTrue(changed(frames[0], frames[1]) >= MOVED && changed(frames[1], frames[2]) >= MOVED,
                            "half way, it is drawn between shut and open: " + changed(frames[0], frames[1]) + " and "
                                    + changed(frames[1], frames[2]) + " pixels");
                })
                .thenScreenshot(0, "ship-ramp-open");
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
