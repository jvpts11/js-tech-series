/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.multiblock.PlanHologram;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.GameText;
import dev.jstech.space.client.hangar.RocketHolograms;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The hologram of a rocket being built on a player's game: before a design is sent, a green screen beyond the rocket's
 * place is seen; sent, and its mobile launcher laid by hand, the rocket's blocks stand where they go as ghosts, so the
 * screen is seen through them changed; the layer to build next is the engine's lowest, and the line across the top of
 * the screen names the rocket and its engine; the engine built and formed, the line names the tank; the whole rocket
 * built, the hologram is gone.
 */
public final class RocketHologramClientTests {

    /* The hangar's terminal, its door to the north; the bay's middle at (5, 2, 11) of the test's place. */
    private static final BlockPos TERMINAL = new BlockPos(22, 2, 9);
    private static final Orientation TURN = Orientation.NORTH;
    /* A perch toward the door, over the launcher's deck, the rocket looked at from it, its middle on the launcher. */
    private static final BlockPos PERCH = new BlockPos(5, 10, 2);
    private static final Vec3 ROCKET = new Vec3(5.5, 11.5, 11.5);
    /* A green screen in the bay beyond the rocket and its tower, and lamps round the rocket. */
    private static final int SCREEN_Z = 23;
    private static final int SCREEN_X0 = 1;
    private static final int SCREEN_X1 = 9;
    private static final int SCREEN_Y0 = 4;
    private static final int SCREEN_Y1 = 20;
    private static final List<BlockPos> LAMPS = List.of(new BlockPos(1, 12, 9), new BlockPos(9, 12, 9),
            new BlockPos(1, 12, 14), new BlockPos(9, 12, 14), new BlockPos(3, 14, 22), new BlockPos(7, 14, 22));
    private static final String NAME = "HOLO";
    private static final int ARRIVE = 400;
    private static final int SHOW = 200;
    private static final int SETTLE = 20;
    private static final int PATCH = 2;
    private static final int OFF_CROSSHAIR = 6;
    private static final int CHANNEL = 0xFF;
    private static final int APART = 30;
    /* The rocket's three parts, the first of its plan's; its launcher's after them. */
    private static final int PARTS = 3;

    private RocketHologramClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void hologram_showsWhatIsMissingTheNextLayerAndWhatIsBeingBuilt(final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), ctx.abs(TERMINAL),
                TURN)));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, PERCH, Direction.SOUTH)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.bay().isPresent(), ARRIVE,
                        "the formed hangar to reach the player")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(BlockPos.ZERO)).add(ROCKET)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenServer(0, level -> {
                    send(ctx, level);
                    lay(ctx, level, part -> part >= PARTS);
                })
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
                            RocketHolograms.LAYER.with(1, 2))), "its launcher laid, the line names the rocket, its "
                            + "engine and the layer being built: " + line);
                    final PlanHologram.Shown shown = RocketHolograms.shown().getFirst();
                    final int lowest = shown.places().stream().mapToInt(place -> place.at().getY()).min()
                            .orElseThrow();
                    ctx.assertTrue(shown.places().stream().anyMatch(place -> place.look() == PlanHologram.Look.NEXT)
                            && shown.places().stream().filter(place -> place.look() == PlanHologram.Look.NEXT)
                            .allMatch(place -> place.at().getY() == lowest)
                            && shown.parts().size() == terminal(ctx).missingParts().size(),
                            "the next layer is the engine's lowest, and each part of the plan has its box");
                })
                .thenScreenshot(0, "rocket-hologram")
                .then(0, () -> ctx.assertTrue(apart(seen[0], seen[1]) > APART, "the rocket's ghosts stand before the"
                        + " screen: " + str(seen[0]) + " then " + str(seen[1])))
                .thenServer(0, level -> lay(ctx, level, part -> part == 0))
                .thenWaitUntil(() -> PlanHologram.banner().map(GameText::resolve).filter(line -> line.contains(
                        GameText.resolve(SkeletonRocketParts.TANK))).isPresent(), SHOW,
                        "the line to name the tank once the engine is formed")
                .thenServer(0, level -> lay(ctx, level, part -> part > 0 && part < PARTS))
                .thenWaitUntil(() -> RocketHolograms.shown().isEmpty() && PlanHologram.banner().isEmpty(), SHOW,
                        "the hologram to go once the rocket is whole");
    }

    /* The hangar built whole, a green screen in its bay beyond the rocket's place, lamps round it, a perch. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        TestHangars.build(level, ctx.abs(TERMINAL), TURN);
        for (int x = SCREEN_X0; x <= SCREEN_X1; x++) {
            for (int y = SCREEN_Y0; y <= SCREEN_Y1; y++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, SCREEN_Z)), Blocks.LIME_CONCRETE
                        .defaultBlockState());
            }
        }
        for (final BlockPos lamp : LAMPS) {
            level.setBlockAndUpdate(ctx.abs(lamp), Blocks.GLOWSTONE.defaultBlockState());
        }
        level.setBlockAndUpdate(ctx.abs(PERCH.below()), Blocks.BARRIER.defaultBlockState());
    }

    /* The design sent to the bay: an engine, a tank and a capsule. */
    private static void send(final ClientTestContext ctx, final ServerLevel level) {
        final HangarTerminalBlockEntity terminal = (HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL));
        terminal.design(new RocketDesign(NAME, RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 2),
                part("capsule_3", 1))));
        terminal.confirm();
    }

    /* Every block of the parts of the plan {@code parts} takes, each the block wanted there. */
    private static void lay(final ClientTestContext ctx, final ServerLevel level, final IntPredicate parts) {
        final PlannedBuild plan = ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).build()
                .orElseThrow();
        for (final PlanSpot spot : plan.plan().spots()) {
            if (parts.test(spot.part())) {
                level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
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
