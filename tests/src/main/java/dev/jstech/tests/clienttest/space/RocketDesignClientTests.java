/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.space.client.hangar.RocketDesignScreen;
import dev.jstech.space.gui.RocketDesignLayout;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * The Rocket Design on a player's game, in a hangar of stone lit from its roof: its terminal used, the screen opens on
 * the hangar seen in 3D, the camera off the player; the rocket on the screen is drawn standing in the bay, where the
 * bay's far side showed before; its sign is lit red while it lacks its capsule's crew, then green. The capsule dragged
 * from its bin to the rocket's top node snaps there, and the rocket, ready, is sent to the bay; closed, the view is the
 * player's again.
 */
public final class RocketDesignClientTests {

    /* The bay, from (-2, 2, 4) to (12, 17, 18) of the test's place, the player standing on its floor. */
    private static final int X0 = -2;
    private static final int X1 = 12;
    private static final int Z0 = 4;
    private static final int Z1 = 18;
    private static final int Y0 = 2;
    private static final int TOP = 17;
    private static final int MIDDLE_X = 5;
    private static final BlockPos TERMINAL = new BlockPos(MIDDLE_X, 3, Z1 + 1);
    private static final BlockPos EYE = new BlockPos(MIDDLE_X, 2, Z1 - 1);
    private static final int ARRIVE = 400;
    private static final int OPEN = 200;
    private static final int SETTLE = 20;
    private static final int SENT = 200;
    private static final int PATCH = 2;
    private static final int CHANNEL = 0xFF;
    private static final int APART = 45;
    private static final int LIT = 40;
    private static final int LAMPS = 3;

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
            ctx.server().execute(() -> clear(ctx, ctx.serverLevel()));
        });
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, EYE, Direction.SOUTH)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.bay().isPresent(), ARRIVE,
                        "the formed hangar to reach the player")
                .thenRightClick(SETTLE, TERMINAL)
                .thenAwaitScreen(RocketDesignScreen.class, OPEN)
                .then(SETTLE, () -> screen(ctx).edit(new RocketDesign("TEST", new RocketStack(List.of()))))
                .then(SETTLE, () -> {
                    seen[0] = mean(ctx, middle(ctx, 4));
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
                    final Vec2 top = screen.view().orElseThrow().project(pad(ctx).add(0, 5, 0)).orElseThrow();
                    screen.mouseReleased(top.x, top.y, InputConstants.MOUSE_BUTTON_LEFT);
                })
                .then(SETTLE, () -> {
                    final RocketDesign design = screen(ctx).design();
                    ctx.assertTrue(design.stack().sections().size() == 3 && design.stack().sections().get(2).part()
                            .equals(capsule), "the capsule dragged to the top node snaps there: " + design);
                    seen[2] = mean(ctx, middle(ctx, 4));
                    seen[3] = mean(ctx, frame(ctx, RocketDesignLayout.INNER_X0 + 4,
                            RocketDesignLayout.STATUS_Y0 + 11));
                })
                .thenScreenshot(0, "rocket-design")
                .then(0, () -> {
                    ctx.assertTrue(apart(seen[0], seen[2]) > APART, "the rocket is drawn standing in the bay, where "
                            + "its far side showed: " + str(seen[0]) + " then " + str(seen[2]));
                    ctx.assertTrue(seen[1][0] - seen[1][1] > LIT, "an empty design's sign is lit red: "
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

    /* The middle of the rocket's place on its bay's floor, in the world. */
    private static Vec3 pad(final ClientTestContext ctx) {
        final HangarBay bay = ((HangarTerminalBlockEntity) ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))).bay()
                .orElseThrow();
        return Vec3.atBottomCenterOf(bay.pad());
    }

    /* Where the rocket's axis is {@code up} blocks over its foot, on the window. */
    private static int[] middle(final ClientTestContext ctx, final double up) {
        final Vec2 at = screen(ctx).view().orElseThrow().project(pad(ctx).add(0, up, 0)).orElseThrow();
        return new int[] {Math.round(at.x), Math.round(at.y)};
    }

    /* Where the frame's pixel {@code x, y} is on the window. */
    private static int[] frame(final ClientTestContext ctx, final int x, final int y) {
        final RocketDesignScreen screen = screen(ctx);
        final float scale = screen.getXSize() / (float) RocketDesignLayout.WIDTH;
        return new int[] {Math.round(screen.getGuiLeft() + (x + 0.5F) * scale),
            Math.round(screen.getGuiTop() + (y + 0.5F) * scale)};
    }

    /* The hangar of stone round the bay, lamps in its roof, its terminal in the south wall, its crane, its door. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < X0 || x > X1 || z < Z0 || z > Z1 || y < Y0 || y > TOP;
                    // Lamps in the roof and along the walls low down, so the tall bay is lit to its floor.
                    final boolean lamp = shell && (y > TOP && (x - X0) % LAMPS == 1 && (z - Z0) % LAMPS == 1
                            || y == Y0 + LAMPS && Math.floorMod(x + z, LAMPS) == 0);
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), lamp ? Blocks.GLOWSTONE
                            .defaultBlockState() : shell ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockState rail = HangarBlocks.CRANE_RAIL.get().defaultBlockState();
        for (int x = X0; x <= X1; x++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP, Z0)), rail);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP, Z1)), rail);
        }
        for (int z = Z0 + 1; z < Z1; z++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(X1 - 1, TOP, z)), HangarBlocks.CRANE_BRIDGE.get()
                    .defaultBlockState());
        }
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X1 - 1, TOP - 1, Z0 + 2)), HangarBlocks.CRANE_HOIST.get()
                .defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X0 - 1, Y0, Z0 + 5)), HangarBlocks.BAY_DOOR.get()
                .defaultBlockState());
        ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).look();
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), y == Y0 - 1 ? Blocks.GRASS_BLOCK
                            .defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
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
