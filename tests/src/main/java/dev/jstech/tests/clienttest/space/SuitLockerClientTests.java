/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.Orientation;
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
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.ship.SuitLockerEntity;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
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
