/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.client.ship.ShipLockers;
import dev.jstech.space.client.ship.SuitLockerScreen;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.life.SuitSupplies;
import dev.jstech.space.ship.ShipFixtureBlock;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.ship.SuitLockerEntity;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A ship's suit locker on a player's game, as the approved one draws it: the suit it holds stands on its rack, piece by
 * piece, the game's own suit a little smaller than worn, where nothing stood while it was empty; and used, it opens its
 * own screen, the locker open with a place for each thing.
 */
public final class SuitLockerClientTests {

    /* The locker, as the airlock's locker block, a block over the land: its suit stands a little under it. */
    private static final BlockPos LOCKER = new BlockPos(2, 3, 4);
    /* The locker's block of the airlock's model. */
    private static final int[] CELL = {1, 1, 1};
    /* How far in front of the suit the player stands, how high their eyes are and how high they look at it. */
    private static final double AHEAD = 2.2;
    private static final double EYES = 1.0;
    private static final double MIDDLE = 0.85;
    private static final double HALF = 0.5;
    private static final ResourceLocation AIRLOCK = SmallShipParts.id("small_airlock");
    private static final int ARRIVE = 200;
    private static final int SETTLE = 10;
    /* How far two pixels may differ and be the same, in a channel; how many must differ for the suit to stand there. */
    private static final int SAME = 8;
    private static final int STANDS = 1500;
    private static final int CHANNEL = 0xFF;
    /*
     * A dock built where the shipyard's client tests build it, its ship laid by hand; the eye and the point looked at
     * across the airlock, in the airlock model's space, as the approved locker was drawn; its four bottles.
     */
    private static final BlockPos DOCK = new BlockPos(-11, -1, 6);
    private static final Vec3 ABOARD_EYE = new Vec3(-0.6, 2.0, 0.8);
    private static final Vec3 ABOARD_AT = new Vec3(1.15, 1.4, 0.75);
    private static final int BOTTLES = 4;
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;

    private SuitLockerClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void locker_standsItsSuitOnTheRackAndOpensItsOwnScreen(final ClientTestContext ctx) {
        final FormedPlace[] place = new FormedPlace[1];
        final Vec3[] view = new Vec3[2];
        final int[][] frames = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().closeContainer();
            ctx.serverLevel().removeBlock(ctx.abs(LOCKER), false);
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        ctx.thenServer(0, level -> {
            final ShipKit kit = ShipKits.all(level).get(AIRLOCK);
            place[0] = new FormedPlace(ShipPartModels.shape(AIRLOCK, kit, kit.pieces().getFirst()),
                    FormedShapes.ORIENTATIONS.indexOf(Orientation.NORTH), CELL[0], CELL[1], CELL[2]);
            final BlockPos at = ctx.abs(LOCKER);
            level.setBlockAndUpdate(at, ShipKitBlocks.SUIT_LOCKER.get().defaultBlockState());
            ((ShipFixtureBlock) ShipKitBlocks.SUIT_LOCKER.get()).partFormed(level, at, true, place[0]);
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).then(0, () -> {
            // Where the rack stands and which way the suit faces, as the locker's renderer reads them.
            final ShipLockers.Rig rig = ShipLockers.rigOf(place[0].model()).orElseThrow();
            final Vector3f rack = rig.rack();
            final double[] at = new double[3];
            CellSpace.toWorld(new double[] {rack.x() - CELL[0], rack.y() - CELL[1], rack.z() - CELL[2]},
                    place[0].turn(), at);
            final double[] ahead = new double[3];
            CellSpace.toWorld(new double[] {HALF + rig.facing().x(), HALF + rig.facing().y(),
                HALF + rig.facing().z()}, place[0].turn(), ahead);
            final Vec3 feet = Vec3.atLowerCornerOf(ctx.abs(LOCKER)).add(at[0], at[1], at[2]);
            view[0] = feet.add((ahead[0] - HALF) * AHEAD, EYES, (ahead[2] - HALF) * AHEAD);
            view[1] = feet.add(0, MIDDLE, 0);
        }).thenServer(0, level -> ctx.serverPlayer().teleportTo(level, view[0].x,
                        view[0].y - ctx.serverPlayer().getEyeHeight(), view[0].z, 0, 0))
                .thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(view[0]) < 0.1, ARRIVE,
                        "the player to stand in front of the locker's rack")
                .then(2, () -> ctx.lookAt(view[1]))
                .then(SETTLE, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, "suit-locker-empty")
                .thenServer(0, level -> {
                    final SuitLockerEntity locker = (SuitLockerEntity) level.getBlockEntity(ctx.abs(LOCKER));
                    locker.setItem(0, new ItemStack(LunarSuit.HELMET.get()));
                    locker.setItem(1, new ItemStack(LunarSuit.SUIT.get()));
                    locker.setItem(2, new ItemStack(LunarSuit.TROUSERS.get()));
                    locker.setItem(3, new ItemStack(LunarSuit.BOOTS.get()));
                    locker.setItem(4, new ItemStack(SuitSupplies.OXYGEN_BOTTLE.get()));
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(LOCKER)) instanceof SuitLockerEntity locker
                        && locker.holds(SuitLockerEntity.Place.BOOTS), ARRIVE, "the player's game to be told the suit")
                .then(SETTLE, () -> {
                    frames[1] = frame(ctx);
                    final int changed = changed(frames[0], frames[1]);
                    ctx.assertTrue(changed >= STANDS, "the suit stands on the rack where nothing stood: " + changed
                            + " pixels");
                })
                .thenScreenshot(0, "suit-locker-suit")
                .thenServer(0, level -> ctx.serverPlayer().openMenu((SuitLockerEntity) level.getBlockEntity(
                        ctx.abs(LOCKER))))
                .thenWaitUntil(() -> ctx.mc().screen instanceof SuitLockerScreen, ARRIVE,
                        "the locker's own screen to open")
                .thenScreenshot(SETTLE, "suit-locker-screen");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void locker_aboardHoldsTheSuitAndBottlesAsTheApprovedLocker(final ClientTestContext ctx) {
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final BlockPos[] locker = new BlockPos[1];
        final ServerLevel[] built = new ServerLevel[1];
        final int[][] frames = new int[2][];
        final ShipDesign design = new ShipDesign("JS-07", List.of(part("orbiter_cockpit"), part("airlock"),
                part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"), part("wheels")));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().getAbilities().flying = false;
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            if (built[0] != null) {
                clear(ctx, built[0]);
            }
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(DOCK));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE * 3, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(design);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                        if (spot.block().equals(ShipKitBlocks.SUIT_LOCKER.getId())) {
                            locker[0] = plan[0].where(spot);
                        }
                    }
                    ctx.assertTrue(locker[0] != null, "the airlock has its suit locker");
                    // Standing across the airlock from the locker, as the approved locker was drawn.
                    final Vec3 eye = inWorld(plan[0], "small_airlock", ABOARD_EYE);
                    final Vec3 along = inWorld(plan[0], "small_airlock", ABOARD_AT).subtract(eye);
                    ctx.serverPlayer().setGameMode(GameType.CREATIVE);
                    ctx.serverPlayer().getAbilities().flying = true;
                    ctx.serverPlayer().onUpdateAbilities();
                    ctx.serverPlayer().teleportTo(level, eye.x, eye.y - ctx.serverPlayer().getEyeHeight(), eye.z,
                            (float) Math.toDegrees(Math.atan2(-along.x, along.z)),
                            (float) Math.toDegrees(-Math.atan2(along.y, along.horizontalDistance())));
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(locker[0]) instanceof SuitLockerEntity, ARRIVE,
                        "the locker to reach the player's game")
                .then(SETTLE * 4, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, "suit-locker-aboard-empty")
                .thenServer(0, level -> {
                    final SuitLockerEntity held = (SuitLockerEntity) level.getBlockEntity(locker[0]);
                    held.setItem(0, new ItemStack(LunarSuit.HELMET.get()));
                    held.setItem(1, new ItemStack(LunarSuit.SUIT.get()));
                    held.setItem(2, new ItemStack(LunarSuit.TROUSERS.get()));
                    held.setItem(3, new ItemStack(LunarSuit.BOOTS.get()));
                    for (int bottle = 0; bottle < BOTTLES; bottle++) {
                        held.setItem(4 + bottle, new ItemStack(SuitSupplies.OXYGEN_BOTTLE.get()));
                    }
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(locker[0]) instanceof SuitLockerEntity held
                        && held.holds(SuitLockerEntity.Place.BOOTS), ARRIVE, "the player's game to be told the suit")
                .then(SETTLE, () -> {
                    frames[1] = frame(ctx);
                    final int changed = changed(frames[0], frames[1]);
                    ctx.assertTrue(changed >= STANDS, "the suit stands in the locker aboard: " + changed + " pixels");
                })
                .thenScreenshot(0, "suit-locker-aboard-full");
    }

    /* Where the point {@code at} of the model {@code model} is in the world, as the plan lays it. */
    private static Vec3 inWorld(final PlannedBuild plan, final String model, final Vec3 at) {
        for (final PlanSpot spot : plan.plan().spots()) {
            if (spot.place().isEmpty() || !spot.place().get().model().getPath().endsWith("/" + model)) {
                continue;
            }
            final FormedPlace place = spot.place().get();
            final double[] turned = new double[3];
            CellSpace.toWorld(new double[] {at.x - place.x(), at.y - place.y(), at.z - place.z()}, place.turn(),
                    turned);
            final BlockPos block = plan.where(spot);
            return new Vec3(block.getX() + turned[0], block.getY() + turned[1], block.getZ() + turned[2]);
        }
        throw new IllegalStateException("no block of " + model + " in the plan");
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

    /* The whole frame, its pixels row by row. */
    private static int[] frame(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[width * height];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = frame.pixel(x, y);
                }
            }
        }
        return out;
    }

    /* How many pixels differ between two frames by more than {@link #SAME} in a channel. */
    private static int changed(final int[] a, final int[] b) {
        int out = 0;
        for (int k = 0; k < a.length; k++) {
            for (int shift = 0; shift <= 16; shift += 8) {
                if (Math.abs((a[k] >> shift & CHANNEL) - (b[k] >> shift & CHANNEL)) > SAME) {
                    out++;
                    break;
                }
            }
        }
        return out;
    }
}
