/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A rocket's launch on the player's game: built whole by its plan and let go, it climbs off where it stood as one
 * body, its formed parts still drawn by its flight computer as it goes, seen against the sky from beside it.
 */
public final class RocketLaunchClientTests {

    /* Where the rocket's foot stands, and where the block keeping its plan stands while it is built. */
    private static final BlockPos FOOT = new BlockPos(0, 2, 40);
    private static final BlockPos OWNER = new BlockPos(0, 2, 30);
    /* Where the player watches from, off its side; how far it climbs before it is looked at, and how far up. */
    private static final Vec3 BESIDE = new Vec3(-24.0, 0.0, 0.0);
    private static final int CLIMBED = 2;
    private static final double LOOK_UP = 3.5;
    private static final int ARRIVE = 400;
    private static final int SETTLE = 2;
    private static final int STILL = 40;
    private static final int CHANNEL = 0xFF;
    /* Beside the crosshair, up and down it: the pixels looked at, and how many must be the rocket, not the sky. */
    private static final int ASIDE = 4;
    private static final int UP_DOWN = 10;
    private static final int STEP = 2;
    private static final int LEAST_SEEN = 6;
    /* How much bluer than red the sky is, which the rocket's skin is not; and how dark an unlit one is drawn. */
    private static final int SKY_BLUE = 30;
    private static final int DARKEST = 60;

    private RocketLaunchClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void rocket_climbsAsOneBodyDrawnWhole(final ClientTestContext ctx) {
        final Map<BlockPos, BlockState> was = new HashMap<>();
        final MovingStructure[] body = new MovingStructure[1];
        final double[] start = new double[1];
        final Vec3[] aim = new Vec3[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (body[0] != null) {
                MovingStructures.takeAway(body[0]);
            }
            was.forEach((at, state) -> ctx.serverLevel().setBlockAndUpdate(at, state));
        }));
        final BlockPos[] computer = new BlockPos[1];
        ctx.thenServer(0, level -> stand(ctx, level, was))
                .thenServer(SETTLE, level -> computer[0] = build(ctx, level, was))
                .then(0, () -> ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(FOOT)).add(0, LOOK_UP, 0)))
                .then(STILL, () -> { })
                .thenScreenshot(0, "rocket-standing")
                .then(0, () -> seen(ctx, "standing on the ground, drawn by its flight computer"))
                .thenServer(0, level -> body[0] = RocketLaunch.release(level, computer[0], null).orElse(null))
                .thenWaitUntilServer(level -> body[0] != null && body[0].ready(), ARRIVE,
                        "the released rocket to stand ready", level -> "released: " + (body[0] != null))
                .then(STILL, () -> { })
                .thenScreenshot(0, "rocket-released")
                .then(0, () -> seen(ctx, "released, as one body where it stood"))
                .thenServer(0, level -> {
                    start[0] = body[0].pose().y();
                    RocketLaunch.lift(level, body[0]);
                })
                .thenWaitUntilServer(level -> body[0].pose().y() > start[0] + CLIMBED, ARRIVE, "the rocket to climb",
                        level -> "at " + body[0].pose().y() + " from " + start[0])
                .thenServer(0, level -> {
                    // Held where it has climbed to, to be looked at.
                    body[0].move(new TowardMotion(body[0].pose(), 1, 0));
                    aim[0] = new Vec3(body[0].pose().x(), body[0].pose().y() + LOOK_UP, body[0].pose().z());
                })
                .then(0, () -> ctx.lookAt(aim[0]))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "rocket-climbing")
                .then(0, () -> seen(ctx, "climbing, its formed parts still drawn"));
    }

    /* The player off the rocket's side, on a block no one sees, kept to put back. */
    private static void stand(final ClientTestContext ctx, final ServerLevel level,
                              final Map<BlockPos, BlockState> was) {
        final Vec3 at = Vec3.atBottomCenterOf(ctx.abs(FOOT)).add(BESIDE);
        final BlockPos under = BlockPos.containing(at).below();
        was.putIfAbsent(under, level.getBlockState(under));
        level.setBlockAndUpdate(under, Blocks.BARRIER.defaultBlockState());
        level.players().getFirst().teleportTo(level, at.x, at.y, at.z, 0.0F, 0.0F);
    }

    /*
     * Builds the rocket whole by its plan, every part formed, lets its plan go with its flight computer as its hangar
     * does, and loads its tank; keeps what stood there to put back, and gives its flight computer's place.
     */
    private static BlockPos build(final ClientTestContext ctx, final ServerLevel level,
                                  final Map<BlockPos, BlockState> was) {
        final RocketStack stack = RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("sea_level_engine_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("tank_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1));
        final BlockPos foot = ctx.abs(FOOT);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, ctx.abs(OWNER), () -> { });
        BlockPos computer = foot;
        for (final PlanSpot spot : build.plan().spots()) {
            was.putIfAbsent(build.where(spot), level.getBlockState(build.where(spot)));
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                computer = build.where(spot);
            }
        }
        build.stop(false);
        if (level.getBlockEntity(computer) instanceof FlightComputerBlockEntity rocket) {
            rocket.carry(new RocketDesign("WATCHED", stack), foot.subtract(computer), List.of(0, 1, 2));
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 5_800.0)));
        }
        return computer;
    }

    /* The rocket {@code what} is seen at the crosshair: lit pixels of it up and down beside it, not the sky. */
    private static void seen(final ClientTestContext ctx, final String what) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + ASIDE;
        final int middle = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int seen = 0;
        final StringBuilder colours = new StringBuilder();
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = middle - UP_DOWN; y <= middle + UP_DOWN; y += STEP) {
                final int argb = frame.pixel(x, y);
                final int red = argb >> 16 & CHANNEL;
                final int blue = argb & CHANNEL;
                if (blue - red < SKY_BLUE && Math.max(red, Math.max(argb >> 8 & CHANNEL, blue)) > DARKEST) {
                    seen++;
                }
                colours.append(' ').append(Integer.toHexString(argb & 0xFFFFFF));
            }
        }
        ctx.assertTrue(seen >= LEAST_SEEN, "the rocket " + what + ", lit: " + seen + " of its pixels seen;"
                + colours);
    }
}
