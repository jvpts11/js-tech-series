/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherPieceBlock;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

/**
 * A mobile launcher built by its hangar's plan on a player's game: once its last blocks are down it takes its formed
 * shape, as the approved launcher is drawn: the tower the red truss round its shaft, seen red where the player looks at
 * it; the deck one piece round its opening, the hazard band's stripes round it; and the whole of it seen from high in
 * the bay, to be put beside the approved pictures.
 */
public final class LauncherClientTests {

    /* The bay, from (-10, 2, 4) to (14, 40, 28) of the test's place, its walls and roof all lamps. */
    private static final int X0 = -10;
    private static final int X1 = 14;
    private static final int Z0 = 4;
    private static final int Z1 = 28;
    private static final int Y0 = 2;
    private static final int TOP = 40;
    private static final int MIDDLE = 2;
    private static final BlockPos TERMINAL = new BlockPos(MIDDLE, 3, Z1 + 1);
    private static final int ARRIVE = 400;
    private static final int FORM = 200;
    private static final int SETTLE = 20;
    private static final int AWAY = 9;
    private static final int WINDOW = 20;
    private static final int CROSS = 5;
    private static final int CHANNEL = 0xFF;
    private static final int REDDER = 40;
    private static final double ENOUGH_RED = 0.2;
    /*
     * On the deck: where the player stands across from the rocket's foot, and where the hazard band round the opening
     * is looked at, in blocks to the tower's left; high in the bay's corner, how far out, along and up.
     */
    private static final double ON_DECK = 3.0;
    private static final double BAND = 1.75;
    private static final double HIGH_OUT = 10.0;
    private static final double HIGH_ALONG = -4.0;
    private static final double HIGH_UP = 5.0;
    private static final int BAND_WINDOW = 8;
    private static final int YELLOWER = 70;
    private static final int DARK = 60;
    private static final double ENOUGH_STRIPES = 0.08;

    private LauncherClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void launcher_isDrawnAsTheApprovedOneOnceItForms(final ClientTestContext ctx) {
        final BlockPos[] tower = new BlockPos[1];
        final BlockPos[] eye = new BlockPos[1];
        final Vec3[] deck = new Vec3[4];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenServer(0, level -> {
            build(ctx, level);
            lay(ctx, level, tower, eye, deck);
        })
                .thenWaitUntil(() -> eye[0] != null, SETTLE, "the launcher to be laid")
                .thenServer(0, level -> {
                    // On a block no one sees, level with the deck, so the deck is not in the way.
                    level.setBlockAndUpdate(eye[0].below(), Blocks.BARRIER.defaultBlockState());
                    level.players().getFirst().teleportTo(level, eye[0].getX() + 0.5, eye[0].getY(),
                            eye[0].getZ() + 0.5, 0.0F, 0.0F);
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(tower[0]).getBlock() instanceof LauncherPieceBlock
                        && ctx.mc().level.getBlockState(tower[0]).getValue(LauncherPieceBlock.FORMED), ARRIVE + FORM,
                        "the formed tower to reach the player")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(tower[0])))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-formed")
                .then(0, () -> {
                    final double red = red(ctx);
                    ctx.assertTrue(red >= ENOUGH_RED, "the formed tower's truss is seen red round the crosshair: "
                            + Math.round(red * 100) + "% of it");
                })
                // Standing on the deck, looking down at the hazard band round the opening.
                .thenServer(0, level -> level.players().getFirst().teleportTo(level, deck[0].x(), deck[0].y(),
                        deck[0].z(), 0.0F, 0.0F))
                .then(SETTLE, () -> ctx.lookAt(deck[1]))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-deck")
                .then(0, () -> {
                    final double[] band = band(ctx);
                    ctx.assertTrue(band[0] >= ENOUGH_STRIPES && band[1] >= ENOUGH_STRIPES, "the deck is drawn"
                            + " with the hazard band's yellow and black stripes round the opening: "
                            + Math.round(band[0] * 100) + "% yellow, " + Math.round(band[1] * 100) + "% black");
                })
                // From high in the bay's corner, the whole launcher as the approved pictures show it.
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(BlockPos.containing(deck[2]).below(), Blocks.BARRIER.defaultBlockState());
                    level.players().getFirst().teleportTo(level, deck[2].x(), deck[2].y(), deck[2].z(), 0.0F, 0.0F);
                })
                .then(SETTLE, () -> ctx.lookAt(deck[1].add(0, 2, 0)))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-overview")
                // And up at the crane on the tower's top, its boom over the rocket.
                .then(SETTLE, () -> ctx.lookAt(deck[3]))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-crane");
    }

    /* The hangar, its walls and roof all lamps, its terminal in the south wall looking north, its crane, its door. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < X0 || x > X1 || z < Z0 || z > Z1 || y < Y0 || y > TOP;
                    // Walls and roof of lamps, so the tower stands in light all the way up.
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), !shell ? Blocks.AIR.defaultBlockState()
                            : y >= Y0 ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.STONE.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int x = X0; x <= X1; x++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP, Z0)), HangarBlocks.CRANE_RAIL.get()
                    .defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP, Z1)), HangarBlocks.CRANE_RAIL.get()
                    .defaultBlockState());
        }
        for (int z = Z0 + 1; z < Z1; z++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(MIDDLE, TOP, z)), HangarBlocks.CRANE_BRIDGE.get()
                    .defaultBlockState());
        }
        level.setBlockAndUpdate(ctx.abs(new BlockPos(MIDDLE, TOP - 1, (Z0 + Z1) / 2)), HangarBlocks.CRANE_HOIST.get()
                .defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X0 - 1, Y0, (Z0 + Z1) / 2)), HangarBlocks.BAY_DOOR.get()
                .defaultBlockState());
        ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).look();
    }

    /*
     * Its plan sent, an engine, a tank and a capsule on a launcher, and every block of it laid at once; the player to
     * stand a few blocks off the tower's side and look at its middle; then on the deck beside the opening, looking at
     * the hazard band round it; then high in the bay's corner over the deck's left, looking at it whole.
     */
    private static void lay(final ClientTestContext ctx, final ServerLevel level, final BlockPos[] tower,
                            final BlockPos[] eye, final Vec3[] deck) {
        final HangarTerminalBlockEntity terminal = (HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL));
        terminal.design(new RocketDesign("LAUNCHER", RocketStack.of(part("sea_level_engine_3", 1),
                part("tank_3", 2), part("capsule_3", 1))));
        terminal.confirm();
        final Direction side = terminal.launcherSide().orElseThrow();
        final PlannedBuild plan = terminal.build().orElseThrow();
        // The rocket too, so its hologram is gone and the tower is seen as it is.
        for (final PlanSpot spot : plan.plan().spots()) {
            level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
        }
        final int middle = plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks
                .TOWER_FRAME.getId())).mapToInt(spot -> spot.at().getY()).max().orElseThrow() / 2;
        // The frame of the tower's side across the rocket's line, halfway up, seen from that side.
        final Direction across = side.getClockWise();
        final PlanSpot seen = plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks
                .TOWER_FRAME.getId()) && spot.at().getY() == middle).max(Comparator.comparingInt(spot ->
                spot.at().getX() * across.getStepX() + spot.at().getZ() * across.getStepZ())).orElseThrow();
        tower[0] = plan.where(seen);
        final Vec3 foot = Vec3.atBottomCenterOf(plan.origin());
        final Vec3 left = new Vec3(-across.getStepX(), 0, -across.getStepZ());
        final Vec3 along = new Vec3(side.getStepX(), 0, side.getStepZ());
        deck[0] = foot.add(left.scale(ON_DECK));
        deck[1] = foot.add(left.scale(BAND));
        deck[2] = foot.add(left.scale(HIGH_OUT)).add(along.scale(HIGH_ALONG)).add(0, HIGH_UP, 0);
        final int boom = plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks.TOWER_FRAME
                .getId())).mapToInt(spot -> spot.at().getY()).max().orElseThrow();
        deck[3] = foot.add(along.scale(HIGH_ALONG / 2)).add(0, boom, 0);
        eye[0] = tower[0].relative(across, AWAY).atY(plan.origin().getY());
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        if (level.getBlockEntity(ctx.abs(TERMINAL)) instanceof HangarTerminalBlockEntity terminal) {
            terminal.undo();
        }
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), y == Y0 - 1 ? Blocks.GRASS_BLOCK
                            .defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /*
     * How much of a window round the crosshair, its own white cross left out, is the hazard band's yellow, and how
     * much its black.
     */
    private static double[] band(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int yellow = 0;
        int black = 0;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -WINDOW; dx <= WINDOW; dx++) {
                for (int dy = -BAND_WINDOW; dy <= BAND_WINDOW; dy++) {
                    if (Math.abs(dx) <= CROSS && Math.abs(dy) <= CROSS) {
                        continue;
                    }
                    final int argb = frame.pixel(x + dx, y + dy);
                    final int r = argb >> 16 & CHANNEL;
                    final int g = argb >> 8 & CHANNEL;
                    final int b = argb & CHANNEL;
                    if (r > b + YELLOWER && g > b + YELLOWER / 2) {
                        yellow++;
                    } else if (r < DARK && g < DARK && b < DARK) {
                        black++;
                    }
                    count++;
                }
            }
        }
        return new double[] {(double) yellow / count, (double) black / count};
    }

    /*
     * How much of a window round the crosshair, its own white cross left out, is red: red well over green and blue, as
     * the truss's members are between the shaft's dark showing through them.
     */
    private static double red(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int red = 0;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -WINDOW; dx <= WINDOW; dx++) {
                for (int dy = -WINDOW; dy <= WINDOW; dy++) {
                    if (Math.abs(dx) <= CROSS && Math.abs(dy) <= CROSS) {
                        continue;
                    }
                    final int argb = frame.pixel(x + dx, y + dy);
                    final int r = argb >> 16 & CHANNEL;
                    if (r > (argb >> 8 & CHANNEL) + REDDER && r > (argb & CHANNEL) + REDDER) {
                        red++;
                    }
                    count++;
                }
            }
        }
        return (double) red / count;
    }
}
