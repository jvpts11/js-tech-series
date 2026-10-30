/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.moving.IStructureBody;
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
import dev.jstech.space.rocket.StageFlights;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket's stage let go on the player's game: its first stage, come loose as a body of its own, is drawn whole by
 * its own blocks, lit, where no flight computer draws it any more.
 */
public final class StagingClientTests {

    /* Where the rocket's foot stands, the block keeping its plan while it is built, and where the player watches. */
    private static final BlockPos FOOT = new BlockPos(0, 2, 60);
    private static final BlockPos OWNER = new BlockPos(0, 2, 50);
    private static final Vec3 BESIDE = new Vec3(-20.0, 0.0, 0.0);
    /* Next to nothing in the first stage's tank, none in the second's: it is let go just over the pad. */
    private static final double FIRST = 20;
    private static final double LOOK_UP = 2.0;
    private static final int ARRIVE = 600;
    private static final int STILL = 20;
    private static final double LOOK_ACROSS = 64;
    private static final double LOOK_HIGH = 256;
    private static final int CHANNEL = 0xFF;
    /* Beside the crosshair, up and down it: the pixels looked at, and how many must be the stage, lit, not the sky. */
    private static final int ASIDE = 3;
    private static final int UP_DOWN = 8;
    private static final int STEP = 2;
    private static final int LEAST_SEEN = 5;
    private static final int SKY_BLUE = 30;
    private static final int DARKEST = 60;

    private StagingClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void stage_isDrawnByItsOwnBlocksOnceLetGo(final ClientTestContext ctx) {
        final Map<BlockPos, BlockState> was = new HashMap<>();
        final MovingStructure[] body = new MovingStructure[1];
        final MovingStructure[] stage = new MovingStructure[1];
        final Vec3[] aim = new Vec3[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            for (final MovingStructure each : new MovingStructure[] {body[0], stage[0]}) {
                if (each != null && MovingStructures.byId(ctx.serverLevel(), each.id()) != null) {
                    MovingStructures.takeAway(each);
                }
            }
            was.forEach((at, state) -> ctx.serverLevel().setBlockAndUpdate(at, state));
        }));
        ctx.thenServer(0, level -> stand(ctx, level, was))
                .thenServer(2, level -> {
                    final BlockPos computer = build(ctx, level, was);
                    body[0] = RocketLaunch.release(level, computer, null).orElse(null);
                })
                .thenWaitUntilServer(level -> body[0] != null && body[0].ready(), ARRIVE,
                        "the released rocket to stand ready", level -> "released: " + (body[0] != null))
                .thenServer(0, level -> RocketLaunch.lift(level, body[0]))
                .thenWaitUntilServer(level -> (stage[0] = stageOf(level, body[0])) != null && stage[0].ready(), ARRIVE,
                        "its first stage to come loose", level -> "rocket at " + body[0].pose())
                .thenServer(0, level -> {
                    // Both held where they are, to be looked at.
                    MovingStructures.takeAway(body[0]);
                    stage[0].move(new TowardMotion(stage[0].pose(), 1, 0));
                    aim[0] = new Vec3(stage[0].pose().x(), stage[0].pose().y() + LOOK_UP, stage[0].pose().z());
                })
                .then(0, () -> ctx.lookAt(aim[0]))
                .then(STILL, () -> { })
                .thenScreenshot(0, "stage-let-go")
                .then(0, () -> seen(ctx));
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

    /* Builds a rocket of two stages whole by its plan, lets its plan go with its computer; gives the computer. */
    private static BlockPos build(final ClientTestContext ctx, final ServerLevel level,
                                  final Map<BlockPos, BlockState> was) {
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("separation_ring_3"),
                part("vacuum_engine_3"), part("tank_3"), part("capsule_3"));
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
            rocket.carry(new RocketDesign("LET GO", stack), foot.subtract(computer), List.of(0, 1, 2, 3, 4, 5));
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, FIRST)));
        }
        return computer;
    }

    /* The stage of {@code body} let go, near it; none yet. */
    private static @Nullable MovingStructure stageOf(final ServerLevel level, final MovingStructure body) {
        final Vec3 at = new Vec3(body.pose().x(), body.pose().y(), body.pose().z());
        for (final IStructureBody near : MovingStructures.near(level, new AABB(at, at).inflate(LOOK_ACROSS, LOOK_HIGH,
                LOOK_ACROSS))) {
            if (near instanceof MovingStructure piece && !piece.id().equals(body.id())
                    && StageFlights.get(level.getServer(), piece.id()).isPresent()) {
                return piece;
            }
        }
        return null;
    }

    /* The stage is seen at the crosshair: lit pixels of it up and down beside it, not the sky. */
    private static void seen(final ClientTestContext ctx) {
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
        ctx.assertTrue(seen >= LEAST_SEEN, "the stage let go is drawn by its own blocks, lit: " + seen
                + " of its pixels seen;" + colours);
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
