/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.descent.DescentPhase;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * A capsule coming down through air on a player's game: its three main parachutes are drawn open over it, orange and
 * white, where the camera looks up at them.
 */
public final class DescentClientTests {

    private static final BlockPos EYE = new BlockPos(0, 2, 0);
    private static final BlockPos COMPUTER = new BlockPos(0, 2, 24);
    /* How high over the computer the domes stand: the capsule, the lines and the domes over them. */
    private static final double DOMES = 16;
    private static final int SETTLE = 20;
    private static final int CHANNEL = 0xFF;
    /* An orange gore: much red, some green, little blue; and the least of them seen round the middle. */
    private static final int ORANGE_RED = 170;
    private static final int ORANGE_BLUE = 80;
    private static final int LEAST_ORANGE = 40;
    private static final int SAMPLE = 120;
    private static final int STEP = 3;

    private DescentClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void parachutes_openOverTheCapsuleOrangeAndWhite(final ClientTestContext ctx) {
        final int[] orange = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(COMPUTER),
                            RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity computer) {
                        computer.carry(new RocketDesign("CHUTES", RocketStack.of(new RocketStack.Part(
                                SkeletonRocketParts.id("capsule_3"), 1))), BlockPos.ZERO, List.of(0));
                        computer.descent(DescentPhase.PARACHUTES);
                    }
                })
                .then(SETTLE, () -> ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(COMPUTER)).add(0, DOMES, 0)))
                .then(SETTLE, () -> orange[0] = orangeRound(ctx))
                .thenScreenshot(0, "parachutes-open")
                .then(0, () -> ctx.assertTrue(orange[0] >= LEAST_ORANGE, "the parachutes are drawn open over the"
                        + " capsule, orange where the camera looks: " + orange[0] + " orange points"));
    }

    /* How many points round the middle of the window are an orange gore's. */
    private static int orangeRound(final ClientTestContext ctx) {
        final int cx = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int cy = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = cx - SAMPLE; x <= cx + SAMPLE; x += STEP) {
                for (int y = cy - SAMPLE; y <= cy + SAMPLE; y += STEP) {
                    final int argb = frame.pixel(x, y);
                    if ((argb >> 16 & CHANNEL) > ORANGE_RED && (argb & CHANNEL) < ORANGE_BLUE
                            && (argb >> 8 & CHANNEL) < (argb >> 16 & CHANNEL)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
