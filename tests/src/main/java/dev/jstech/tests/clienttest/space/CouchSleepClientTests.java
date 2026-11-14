/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.space.client.cruise.CouchSleepClient;
import dev.jstech.space.cruise.CouchSleep;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sleeping in a crew seat on a player's game: sitting in a capsule's seat in orbit, its key pressed, the player's view
 * darkens as in a bed, the line under it saying which key wakes them; pressed again, they wake and the view clears.
 */
public final class CouchSleepClientTests {

    private static final BlockPos EYE = new BlockPos(0, 2, 0);
    private static final BlockPos ROCKET = new BlockPos(6, 2, 6);
    private static final int TRAVEL = 1200;
    private static final int SETTLE = 10;
    /* As long as the view takes to darken as in a bed, and a little; as long as it takes to clear. */
    private static final int ASLEEP = 110;
    private static final int WAKING = 15;
    /*
     * How bright a channel of the view may be asleep, whatever lies behind: the bed's dark over the brightest white;
     * how far from the view's middle it is looked at, clear of the crosshair.
     */
    private static final int DARK = 64;
    private static final int OFF_MIDDLE = 24;
    private static final int CHANNEL = 0xFF;
    /* How far up from the screen's foot the wake line stands, its height, and the least of its points lit. */
    private static final int LINE_UP = 40;
    private static final int LINE_TALL = 9;
    private static final int LINE_WIDE = 120;
    private static final int LIT = 200;
    private static final int LEAST_LIT = 20;

    private CouchSleepClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void couchSleep_darkensTheViewAsInABedAndClearsItAwake(final ClientTestContext ctx) {
        final MovingStructure[] rocket = new MovingStructure[1];
        final double[] orbit = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0);
        final int[] seen = new int[2];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            player.stopRiding();
            final ServerLevel space = space(ctx);
            final MovingStructure there = rocket[0] == null ? null : MovingStructures.byId(space, rocket[0].id());
            if (there != null) {
                MovingStructures.takeAway(there);
            }
            space.setChunkForced(SectionPos.blockToSectionCoord(orbit[0]), SectionPos.blockToSectionCoord(orbit[1]),
                    false);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            final BlockPos home = ctx.abs(EYE);
            player.teleportTo(ctx.serverLevel(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0F, 0F);
        }));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> rocket[0] = rocket(ctx, level))
                .thenWaitUntilServer(level -> rocket[0].ready(), TRAVEL, "the rocket to stand ready",
                        level -> "arriving " + rocket[0].arriving())
                .thenServer(0, level -> {
                    final ServerLevel space = space(ctx);
                    space.setChunkForced(SectionPos.blockToSectionCoord(orbit[0]), SectionPos.blockToSectionCoord(
                            orbit[1]), true);
                    ctx.assertTrue(MovingStructures.travel(rocket[0], space, new Vec3(orbit[0], SystemLayout.ORBIT_Y,
                            orbit[1])).ok(), "the rocket is sent into the Overworld's orbit");
                })
                .thenWaitUntilServer(level -> inOrbit(ctx, rocket[0]) != null, TRAVEL,
                        "the rocket to rest in the Overworld's orbit", level -> "departing " + rocket[0].departing())
                .thenServer(0, level -> {
                    final ServerLevel space = space(ctx);
                    final MovingStructure there = inOrbit(ctx, rocket[0]);
                    final BlockPos seat = seatOf(space, there);
                    final Vec3 at = MovingStructures.inWorld(space, Vec3.atCenterOf(seat));
                    final ServerPlayer player = ctx.serverPlayer();
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(space, at.x, at.y, at.z, 0F, 0F);
                    ctx.assertTrue(MovingStructures.sit(player, seat), "the player sits in the capsule's seat");
                })
                .thenWaitUntil(() -> ctx.player() != null && ctx.player().getVehicle() instanceof StructureSeatEntity,
                        TRAVEL, "the player's game to seat them")
                .then(SETTLE, () -> KeyActionsClient.press(CouchSleep.KEY))
                .thenWaitUntil(CouchSleepClient::asleep, TRAVEL, "the player to fall asleep in the seat")
                .then(ASLEEP, () -> {
                    seen[0] = brightest(ctx);
                    seen[1] = lineLit(ctx);
                })
                .thenScreenshot(0, "couch-sleep")
                .then(0, () -> KeyActionsClient.press(CouchSleep.KEY))
                .thenWaitUntil(() -> !CouchSleepClient.asleep(), TRAVEL, "the player to wake")
                .then(WAKING, () -> {
                    ctx.assertTrue(seen[0] <= DARK, "asleep, the view is as dark as in a bed: near its middle at most "
                            + seen[0]);
                    ctx.assertTrue(seen[1] >= LEAST_LIT, "the line under it says which key wakes them: " + seen[1]
                            + " points lit");
                    ctx.assertTrue(ctx.player().getVehicle() instanceof StructureSeatEntity,
                            "awake, they still sit in the seat");
                })
                .thenScreenshot(0, "couch-sleep-awake");
    }

    /* The brightest channel of a point near the view's middle, clear of the crosshair. */
    private static int brightest(final ClientTestContext ctx) {
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            final int argb = frame.pixel(ctx.mc().getWindow().getGuiScaledWidth() / 2 + OFF_MIDDLE,
                    ctx.mc().getWindow().getGuiScaledHeight() / 2 + OFF_MIDDLE);
            return Math.max(argb >> 16 & CHANNEL, Math.max(argb >> 8 & CHANNEL, argb & CHANNEL));
        }
    }

    /* How many points of the wake line's place, at the foot of the view, are lit white. */
    private static int lineLit(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = width / 2 - LINE_WIDE / 2; x < width / 2 + LINE_WIDE / 2; x++) {
                for (int y = height - LINE_UP; y < height - LINE_UP + LINE_TALL; y++) {
                    final int argb = frame.pixel(x, y);
                    if ((argb >> 16 & CHANNEL) > LIT && (argb >> 8 & CHANNEL) > LIT && (argb & CHANNEL) > LIT) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* An engine, a tank and a capsule, built whole at ROCKET, every part formed, and let go as one structure. */
    private static MovingStructure rocket(final ClientTestContext ctx, final ServerLevel level) {
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        final BlockPos at = ctx.abs(ROCKET);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), at);
        build.start(level, ctx.abs(EYE), () -> { });
        BlockPos computerAt = at;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                computerAt = build.where(spot);
            }
        }
        ctx.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        if (!(level.getBlockEntity(computerAt) instanceof FlightComputerBlockEntity computer)) {
            throw new IllegalStateException("the rocket holds no flight computer");
        }
        computer.carry(new RocketDesign("Couch", stack), at.subtract(computerAt), IntStream.range(0,
                stack.sections().size()).boxed().toList());
        return RocketLaunch.release(level, computerAt, null).orElseThrow();
    }

    /* The body of {@code rocket} resting in the Overworld's orbit; none while it is not there. */
    private static @Nullable MovingStructure inOrbit(final ClientTestContext ctx, final MovingStructure rocket) {
        final MovingStructure there = MovingStructures.byId(space(ctx), rocket.id());
        return there != null && there.ready() ? there : null;
    }

    /* The crew seat aboard {@code body}, a place of its yard, merged into its formed capsule. */
    private static BlockPos seatOf(final ServerLevel level, final MovingStructure body) {
        final Box box = body.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FormedPartBlockEntity part
                    && part.original().is(RocketKitBlocks.CREW_SEAT.get())) {
                return pos.immutable();
            }
        }
        throw new IllegalStateException("no crew seat aboard");
    }

    private static ServerLevel space(final ClientTestContext ctx) {
        return SystemSpaces.spaceOf(ctx.server(), StarSystems.SOLAR_SYSTEM);
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
