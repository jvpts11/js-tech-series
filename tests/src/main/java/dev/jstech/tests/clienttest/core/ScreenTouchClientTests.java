/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.live.ScreenTouchClient;
import dev.jstech.tests.TestCameras;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A screen on a block's face as the player's crosshair meets it: the pixel under the crosshair found, as the screen's
 * picture reads it to light what is there, and a right click pressing the screen at that pixel on the server.
 */
public final class ScreenTouchClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    /* The test screen, two blocks south of the player: its north face looks at them. */
    private static final BlockPos SCREEN = new BlockPos(0, 3, 1);
    /* The pixel aimed at, a quarter across and three quarters down. */
    private static final int AT_X = 16;
    private static final int AT_Y = 48;
    private static final int WAIT = 60;

    private ScreenTouchClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void crosshair_findsThePixelAndAClickPressesIt(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    TestCameras.PRESSED.set(new int[] {-1, -1});
                    level.setBlockAndUpdate(ctx.abs(SCREEN), TestCameras.SCREEN.get().defaultBlockState());
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(SCREEN)).is(TestCameras.SCREEN.get()), WAIT,
                        "the screen in the world")
                .then(0, () -> ctx.lookAt(onFace(ctx, AT_X, AT_Y)))
                .thenWaitUntil(() -> ScreenTouchClient.pointed().isPresent(), WAIT, "the crosshair on the screen")
                .thenAssert(0, () -> ScreenTouchClient.pointerOn(ctx.abs(SCREEN))
                        .filter(at -> at[0] == AT_X && at[1] == AT_Y).isPresent(),
                        "the pixel under the crosshair is the one aimed at")
                .then(0, () -> KeyMapping.click(ctx.mc().options.keyUse.getKey()))
                .thenWaitUntil(() -> TestCameras.PRESSED.get()[0] == AT_X && TestCameras.PRESSED.get()[1] == AT_Y,
                        WAIT, "the server heard the press at that pixel")
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(SCREEN), Blocks.AIR.defaultBlockState()));
    }

    /* The middle of a pixel of the screen on the north face, seen from the north: its left is to the east. */
    private static Vec3 onFace(final ClientTestContext ctx, final int x, final int y) {
        final BlockPos at = ctx.abs(SCREEN);
        final double across = (x + 0.5) / TestCameras.TOUCH_PIXELS;
        final double down = (y + 0.5) / TestCameras.TOUCH_PIXELS;
        return new Vec3(at.getX() + 1 - across, at.getY() + 1 - down, at.getZ());
    }
}
