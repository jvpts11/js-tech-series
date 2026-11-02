/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.client.hangar.RocketDesignScreen;
import dev.jstech.space.gui.RocketDesignLayout;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * The Rocket Design on a player's game, in a Rocket Hangar built whole: its terminal used in the control room, the
 * screen opens on the hangar's bay seen in 3D from the control room's side, the camera off the player; the rocket on
 * the screen is drawn standing on its launcher in the bay, its tank where a green screen beyond its engine showed
 * before; its sign is lit red while it is not ready, then green. The capsule dragged from its bin to the rocket's top
 * node snaps there, and the rocket, ready, is sent to the bay; closed, the view is the player's again.
 */
public final class RocketDesignClientTests {

    /* The hangar's terminal, its door to the north; the bay's middle at (5, 2, 11) of the test's place. */
    private static final BlockPos TERMINAL = new BlockPos(22, 2, 9);
    private static final Orientation TURN = Orientation.NORTH;
    /* Where the player stands in the control room, behind the terminal's chair, facing the terminal. */
    private static final BlockPos EYE = new BlockPos(24, 2, 9);
    /* A green screen in the bay beyond the rocket's place, west of it, and lamps round the rocket. */
    private static final int SCREEN_X = -3;
    private static final int SCREEN_Z0 = 5;
    private static final int SCREEN_Z1 = 17;
    private static final int SCREEN_Y0 = 2;
    private static final int SCREEN_Y1 = 30;
    private static final List<BlockPos> LAMPS = List.of(new BlockPos(1, 12, 6), new BlockPos(9, 12, 6),
            new BlockPos(1, 12, 16), new BlockPos(9, 12, 16), new BlockPos(-2, 12, 8), new BlockPos(-2, 12, 14));
    private static final int ARRIVE = 400;
    private static final int OPEN = 200;
    private static final int SETTLE = 20;
    private static final int SENT = 200;
    private static final int PATCH = 2;
    private static final int CHANNEL = 0xFF;
    private static final int APART = 45;
    private static final int LIT = 40;
    /* How far up the rocket's axis, from its foot, its colour is read: its engine's and its tank's, under the lamps. */
    private static final int UP = 4;

    private RocketDesignClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void design_drawsTheRocketInTheHangarAndSnapsTheCapsuleToItsTop(final ClientTestContext ctx) {
        final int[][] seen = new int[4][];
        final RocketStack.Part engine = new RocketStack.Part(SkeletonRocketParts.id("sea_level_engine_3"), 1);
        final RocketStack.Part tank = new RocketStack.Part(SkeletonRocketParts.id("tank_3"), 3);
        final RocketStack.Part capsule = new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1);
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), ctx.abs(TERMINAL), TURN));
        });
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, EYE, Direction.WEST)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.bay().isPresent(), ARRIVE,
                        "the formed hangar to reach the player")
                .thenRightClick(SETTLE, TERMINAL)
                .thenAwaitScreen(RocketDesignScreen.class, OPEN)
                // Its engine alone, already on its launcher, the view turned round it; nothing of it yet where its
                // tank goes.
                .then(SETTLE, () -> screen(ctx).edit(new RocketDesign("TEST", RocketStack.of(engine))))
                .then(SETTLE, () -> {
                    seen[0] = mean(ctx, middle(ctx, UP));
                    seen[1] = mean(ctx, frame(ctx, RocketDesignLayout.INNER_X0 + 4,
                            RocketDesignLayout.STATUS_Y0 + 11));
                })
                .then(0, () -> screen(ctx).edit(new RocketDesign("TEST", RocketStack.of(engine, tank))))
                .then(SETTLE, () -> {
                    ctx.assertTrue(ctx.mc().getCameraEntity() != ctx.mc().player,
                            "the screen turns its own view round the rocket, off the player");
                    final RocketDesignScreen screen = screen(ctx);
                    final int[] tab = frame(ctx, RocketDesignLayout.TAB_X0 + 11, RocketDesignLayout.TAB_Y0 + 11);
                    screen.mouseClicked(tab[0], tab[1], InputConstants.MOUSE_BUTTON_LEFT);
                    screen.mouseReleased(tab[0], tab[1], InputConstants.MOUSE_BUTTON_LEFT);
                    final int[] card = frame(ctx, RocketDesignLayout.cardX(0) + 20, RocketDesignLayout.cardY(0)
                            + 20);
                    screen.mouseClicked(card[0], card[1], InputConstants.MOUSE_BUTTON_LEFT);
                    final Vec2 top = screen.view().orElseThrow().project(foot(ctx).add(0, 5, 0)).orElseThrow();
                    screen.mouseReleased(top.x, top.y, InputConstants.MOUSE_BUTTON_LEFT);
                })
                .then(SETTLE, () -> {
                    final RocketDesign design = screen(ctx).design();
                    ctx.assertTrue(design.stack().sections().size() == 3 && design.stack().sections().get(2).part()
                            .equals(capsule), "the capsule dragged to the top node snaps there: " + design);
                    seen[2] = mean(ctx, middle(ctx, UP));
                    seen[3] = mean(ctx, frame(ctx, RocketDesignLayout.INNER_X0 + 4,
                            RocketDesignLayout.STATUS_Y0 + 11));
                })
                .thenScreenshot(0, "rocket-design")
                .then(0, () -> {
                    ctx.assertTrue(apart(seen[0], seen[2]) > APART, "the rocket is drawn standing in the bay, where "
                            + "its far side showed: " + str(seen[0]) + " then " + str(seen[2]));
                    ctx.assertTrue(seen[1][0] - seen[1][1] > LIT, "an engine with nothing to burn lights it red: "
                            + str(seen[1]));
                    ctx.assertTrue(seen[3][1] - seen[3][0] > LIT, "the ready rocket's sign is lit green: "
                            + str(seen[3]));
                    screen(ctx).keyPressed(InputConstants.KEY_E, 0, 0);
                })
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.plan().isPresent(), SENT,
                        "the ready rocket to be sent to the bay", level -> "the hangar's design: "
                                + ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).design())
                .then(0, () -> ctx.mc().setScreen(null))
                .thenAssert(SETTLE, () -> ctx.mc().getCameraEntity() == ctx.mc().player,
                        "closed, the view is the player's own again");
    }

    private static RocketDesignScreen screen(final ClientTestContext ctx) {
        return ctx.screen(RocketDesignScreen.class);
    }

    /* The middle of the rocket's foot, on its launcher's deck over its bay's floor, in the world. */
    private static Vec3 foot(final ClientTestContext ctx) {
        final HangarBay bay = ((HangarTerminalBlockEntity) ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))).bay()
                .orElseThrow();
        return Vec3.atBottomCenterOf(bay.pad().above(LauncherLayout.RAISE));
    }

    /* Where the rocket's axis is {@code up} blocks over its foot on its launcher, on the window. */
    private static int[] middle(final ClientTestContext ctx, final double up) {
        final Vec2 at = screen(ctx).view().orElseThrow().project(foot(ctx).add(0, up, 0)).orElseThrow();
        return new int[] {Math.round(at.x), Math.round(at.y)};
    }

    /* Where the frame's pixel {@code x, y} is on the window. */
    private static int[] frame(final ClientTestContext ctx, final int x, final int y) {
        final RocketDesignScreen screen = screen(ctx);
        final float scale = screen.getXSize() / (float) RocketDesignLayout.WIDTH;
        return new int[] {Math.round(screen.getGuiLeft() + (x + 0.5F) * scale),
            Math.round(screen.getGuiTop() + (y + 0.5F) * scale)};
    }

    /*
     * The hangar built whole, a green screen in its bay beyond the rocket's place as the design screen looks from the
     * control room, and lamps round the rocket's place so it is drawn lit.
     */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        TestHangars.build(level, ctx.abs(TERMINAL), TURN);
        for (int z = SCREEN_Z0; z <= SCREEN_Z1; z++) {
            for (int y = SCREEN_Y0; y <= SCREEN_Y1; y++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(SCREEN_X, y, z)), Blocks.LIME_CONCRETE
                        .defaultBlockState());
            }
        }
        for (final BlockPos lamp : LAMPS) {
            level.setBlockAndUpdate(ctx.abs(lamp), Blocks.GLOWSTONE.defaultBlockState());
        }
    }

    /* The mean colour of a small patch round {@code at}. */
    private static int[] mean(final ClientTestContext ctx, final int[] at) {
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
                    final int argb = frame.pixel(at[0] + dx, at[1] + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }

    private static int apart(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
