/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetector;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetectorLayout;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetectorBlockEntity;
import dev.jstech.core.jstechseries.atmosphere.client.AtmosphereDetectorScreen;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.RoomView;
import dev.jstech.core.room.RoomViews;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * The sealed rooms kit on a player's game, to look at: a stone hut with a window, a copper grate in a wall and a hole
 * in its roof, its air a little over the place's and falling; its Atmosphere Detector's screen, the gauge, the air,
 * the room drawn with its leaks and the lamps; and, the key pressed, the room in the world, its outline seen through
 * its walls, its leaks marked and what its air reads beside the crosshair.
 */
public final class RoomClientTests {

    private static final int FLOOR = 2;
    private static final BlockPos STAND = new BlockPos(0, FLOOR, -3);
    private static final BlockPos LOW = new BlockPos(-2, FLOOR, 1);
    private static final BlockPos HIGH = new BlockPos(2, FLOOR + 3, 5);
    private static final BlockPos DETECTOR = new BlockPos(0, FLOOR + 1, 2);
    private static final BlockPos GRATE = new BlockPos(2, FLOOR + 1, 4);
    private static final BlockPos ROOF_HOLE = new BlockPos(0, FLOOR + 3, 4);
    private static final BlockPos INSIDE = new BlockPos(0, FLOOR + 1, 3);
    private static final int WAIT = 200;
    private static final int READ = 45;
    private static final int SETTLE = 10;

    private RoomClientTests() {
    }

    @ClientTest(timeoutTicks = 1800)
    public static void detector_showsItsRoomOnItsScreenAndInTheWorld(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerLevel level = ctx.serverLevel();
            SealedRooms.untrack(level, ctx.abs(INSIDE));
            hut(ctx, level, Blocks.AIR, Blocks.AIR);
        }));
        ctx.thenServer(0, level -> {
            hut(ctx, level, Blocks.STONE, Blocks.AIR);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-1, FLOOR + 2, 1)), Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(1, FLOOR + 2, 1)), Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(GRATE), Blocks.COPPER_GRATE.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(DETECTOR), AtmosphereDetector.BLOCK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            SealedRooms.track(level, ctx.abs(INSIDE));
        })
                .thenWaitUntil(() -> ctx.server().submit(() -> SealedRooms.roomAt(ctx.serverLevel(),
                        ctx.abs(INSIDE)).isPresent()).join(), WAIT, "the hut to be found as a room")
                .thenServer(0, level -> {
                    SealedRooms.roomAt(level, ctx.abs(INSIDE)).ifPresent(room -> room.add(AirMix.NITROGEN,
                            GasMath.moles(40, room.gasVolume(), GasMath.TEMPERED)));
                    level.setBlockAndUpdate(ctx.abs(ROOF_HOLE), Blocks.AIR.defaultBlockState());
                })
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenAwaitLandDrawn(WAIT)
                .then(READ, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(DETECTOR))))
                .thenRightClick(SETTLE, DETECTOR)
                .thenAwaitScreen(AtmosphereDetectorScreen.class, WAIT)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(DETECTOR))
                        instanceof AtmosphereDetectorBlockEntity detector && detector.readings().room(), WAIT,
                        "the detector to read its room")
                .thenScreenshot(SETTLE, "detector_screen")
                .then(0, () -> ctx.clickGui(AtmosphereDetectorLayout.SHOW_X + 4, AtmosphereDetectorLayout.SHOW_Y + 4))
                .thenWaitUntil(() -> RoomViews.seen(ctx.mc().level.getGameTime()).source()
                        == RoomView.Source.DETECTOR, WAIT, "the room to be shown in the world")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(INSIDE))))
                .thenAssert(SETTLE, () -> {
                    final RoomView view = RoomViews.seen(ctx.mc().level.getGameTime());
                    return view.shown() && view.leaks().size() >= 2 && !view.outline().isEmpty();
                }, "the room shows its outline and its two leaks, the grate and the hole")
                .thenScreenshot(0, "room_in_the_world");
    }

    /* The hut: a box of {@code wall} from LOW to HIGH, its inside of {@code inside}. */
    private static void hut(final ClientTestContext ctx, final ServerLevel level, final Block wall,
                            final Block inside) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final boolean shell = x == LOW.getX() || x == HIGH.getX() || y == LOW.getY() || y == HIGH.getY()
                            || z == LOW.getZ() || z == HIGH.getZ();
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), (shell ? wall : inside)
                            .defaultBlockState());
                }
            }
        }
    }
}
