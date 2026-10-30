/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.multiblock.PlanHologram;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.GameText;
import dev.jstech.space.client.hangar.RocketHolograms;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The hologram of a rocket being built on a player's game: before a design is sent, the bay's green far wall is seen;
 * sent, the rocket's blocks stand where they go as ghosts, so the wall is seen through them changed; the layer to build
 * next is the engine's lowest, and the line across the top of the screen names the rocket and its engine; the engine
 * built and formed, the line names the tank; the whole rocket built, the hologram is gone.
 */
public final class RocketHologramClientTests {

    /* The bay, from (-2, 2, 4) to (12, 17, 18) of the test's place; the rocket on (5, 2, 11). */
    private static final int X0 = -2;
    private static final int X1 = 12;
    private static final int Z0 = 4;
    private static final int Z1 = 18;
    private static final int Y0 = 2;
    private static final int TOP = 17;
    private static final int MIDDLE_X = 5;
    private static final BlockPos TERMINAL = new BlockPos(MIDDLE_X, 3, Z1 + 1);
    private static final BlockPos EYE = new BlockPos(MIDDLE_X, 2, Z1 - 1);
    private static final Vec3 ROCKET = new Vec3(MIDDLE_X + 0.5, Y0 + 3.5, 11.5);
    private static final String NAME = "HOLO";
    private static final int ARRIVE = 400;
    private static final int SHOW = 200;
    private static final int SETTLE = 20;
    private static final int PATCH = 2;
    private static final int OFF_CROSSHAIR = 6;
    private static final int CHANNEL = 0xFF;
    private static final int APART = 30;
    private static final int LAMPS = 3;
    private static final int PARTS = 3;

    private RocketHologramClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void hologram_showsWhatIsMissingTheNextLayerAndWhatIsBeingBuilt(final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, EYE, Direction.NORTH)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.bay().isPresent(), ARRIVE,
                        "the formed hangar to reach the player")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(BlockPos.ZERO)).add(ROCKET)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenServer(0, level -> send(ctx, level))
                .thenWaitUntil(() -> terminal(ctx).planAt().isPresent(), SHOW, "the plan sent to reach the player")
                .then(SETTLE, () -> ctx.assertTrue(HangarTerminalBlockEntity.loadedOnClient().contains(terminal(ctx))
                        && !RocketHolograms.shown().isEmpty(), "the player's game knows the terminal and shows its "
                        + "rocket's hologram: " + HangarTerminalBlockEntity.loadedOnClient().size() + " terminals, "
                        + RocketHolograms.shown().size() + " holograms"))
                .thenWaitUntil(() -> PlanHologram.banner().isPresent(), SHOW, "the hologram and its line to show")
                .then(SETTLE, () -> {
                    seen[1] = mean(ctx);
                    final String line = GameText.resolve(PlanHologram.banner().orElseThrow());
                    ctx.assertTrue(line.contains(NAME) && line.contains(GameText.resolve(
                            SkeletonRocketParts.SEA_LEVEL_ENGINE)) && line.contains(GameText.resolve(
                            RocketHolograms.LAYER.with(1, 2))), "the line names the rocket, its engine and the layer "
                            + "being built: " + line);
                    final PlanHologram.Shown shown = RocketHolograms.shown().getFirst();
                    final int lowest = shown.places().stream().mapToInt(place -> place.at().getY()).min()
                            .orElseThrow();
                    ctx.assertTrue(shown.places().stream().anyMatch(place -> place.look() == PlanHologram.Look.NEXT)
                            && shown.places().stream().filter(place -> place.look() == PlanHologram.Look.NEXT)
                            .allMatch(place -> place.at().getY() == lowest) && shown.parts().size() == PARTS,
                            "the next layer is the engine's lowest, and each of the three parts has its box");
                })
                .thenScreenshot(0, "rocket-hologram")
                .then(0, () -> ctx.assertTrue(apart(seen[0], seen[1]) > APART, "the rocket's ghosts stand before the"
                        + " far wall: " + str(seen[0]) + " then " + str(seen[1])))
                .thenServer(0, level -> lay(ctx, level, 0))
                .thenWaitUntil(() -> PlanHologram.banner().map(GameText::resolve).filter(line -> line.contains(
                        GameText.resolve(SkeletonRocketParts.TANK))).isPresent(), SHOW,
                        "the line to name the tank once the engine is formed")
                .thenServer(0, level -> {
                    lay(ctx, level, 1);
                    lay(ctx, level, 2);
                })
                .thenWaitUntil(() -> RocketHolograms.shown().isEmpty() && PlanHologram.banner().isEmpty(), SHOW,
                        "the hologram to go once the rocket is whole");
    }

    /* The hangar, lamps in its roof and along its walls, its far wall green; formed, its plan not yet sent. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < X0 || x > X1 || z < Z0 || z > Z1 || y < Y0 || y > TOP;
                    final boolean lamp = shell && (y > TOP && (x - X0) % LAMPS == 1 && (z - Z0) % LAMPS == 1
                            || y == Y0 + LAMPS && Math.floorMod(x + z, LAMPS) == 0);
                    final BlockState wall = z < Z0 && y >= Y0 && y <= TOP ? Blocks.LIME_CONCRETE.defaultBlockState()
                            : Blocks.STONE.defaultBlockState();
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), lamp ? Blocks.GLOWSTONE
                            .defaultBlockState() : shell ? wall : Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockState rail = HangarBlocks.CRANE_RAIL.get().defaultBlockState();
        for (int x = X0; x <= X1; x++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP - 1, Z0)), rail);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP - 1, Z1)), rail);
        }
        for (int z = Z0; z <= Z1; z++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(X1 - 1, TOP, z)), HangarBlocks.CRANE_BRIDGE.get()
                    .defaultBlockState());
        }
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X1 - 1, TOP - 1, Z0 + 2)), HangarBlocks.CRANE_HOIST.get()
                .defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X0 - 1, Y0, Z0 + 5)), HangarBlocks.BAY_DOOR.get()
                .defaultBlockState());
        ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).look();
    }

    /* The design sent to the bay: an engine, a tank and a capsule. */
    private static void send(final ClientTestContext ctx, final ServerLevel level) {
        final HangarTerminalBlockEntity terminal = (HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL));
        terminal.design(new RocketDesign(NAME, RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 2),
                part("capsule_3", 1))));
        terminal.confirm();
    }

    /* Every block of the part {@code part} of the plan, each the block wanted there. */
    private static void lay(final ClientTestContext ctx, final ServerLevel level, final int part) {
        final PlannedBuild plan = ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).build()
                .orElseThrow();
        for (final PlanSpot spot : plan.plan().spots()) {
            if (spot.part() == part) {
                level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
            }
        }
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

    /* The terminal on the player's game. */
    private static HangarTerminalBlockEntity terminal(final ClientTestContext ctx) {
        return (HangarTerminalBlockEntity) ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* The mean colour of a patch just under the crosshair, which whitens its own middle. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + OFF_CROSSHAIR;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
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
