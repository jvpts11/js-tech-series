/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.damage.CellMark;
import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What happens on the way between worlds, a second at a time, unless the server turns it off: a micrometeoroid holes
 * the hull and the air of the room behind the hole rushes out till it is patched; a solar storm is told some seconds
 * ahead and passes, the crew sheltering behind the water and the cargo; a part burns through and loses a block, its
 * work failing till the block is put back.
 */
public final class CruiseEvents {

    /* How many seconds pass between each kind of event, on the mean. */
    private static final int MICROMETEOROID_EVERY = 150;
    private static final int STORM_EVERY = 600;
    private static final int FAILURE_EVERY = 900;
    /* A micrometeoroid's hole and a burn-through, in blocks. */
    private static final double GRAIN = 0.15;
    private static final double BURN_THROUGH = 0.8;
    /* How long a storm is told ahead and lasts, in ticks. */
    private static final int STORM_AHEAD = 400;
    private static final int STORM_LASTS = 600;
    private static final int TICKS_A_SECOND = 20;
    /* The share of a holed room's air that rushes out each second through each hole. */
    private static final double LEAK = 0.05;
    /* How far round a rocket's box a body is aboard it, in blocks. */
    private static final double ABOARD = 3;
    /* How many of its blocks a grain is tried on, till one shows it a face from outside. */
    private static final int TRIES = 16;

    private CruiseEvents() {
    }

    /** A second of {@code body}'s cruise: perhaps an event, by the chances of each. */
    public static void second(final ServerLevel level, final MovingStructure body,
                              final FlightComputerBlockEntity computer) {
        final RandomSource random = level.random;
        if (random.nextInt(MICROMETEOROID_EVERY) == 0) {
            micrometeoroid(level, body, computer);
        }
        if (random.nextInt(STORM_EVERY) == 0 && !SolarStorms.passing(level)) {
            storm(level, body);
        }
        if (random.nextInt(FAILURE_EVERY) == 0) {
            failure(level, body, computer);
        }
    }

    /** A micrometeoroid holes the hull of {@code body} at one of its formed blocks; whether it struck one. */
    public static boolean micrometeoroid(final ServerLevel level, final MovingStructure body,
                                         final FlightComputerBlockEntity computer) {
        final List<BlockPos> hull = computer.formedPlaces(level);
        for (int tries = 0; tries < TRIES && !hull.isEmpty(); tries++) {
            final BlockPos struck = hull.get(level.random.nextInt(hull.size()));
            final Vec3 out = outward(computer, struck);
            // Where a grain coming from outside meets the block's own shape: its hull there.
            final Vec3 middle = Vec3.atCenterOf(struck);
            final BlockHitResult met = level.getBlockState(struck).getCollisionShape(level, struck)
                    .clip(middle.add(out.scale(2)), middle, struck);
            if (met != null && FormedDamage.strike(level, struck, DamageStamps.MICROMETEOROID, met.getLocation(),
                    out, GRAIN, level.random.nextLong()) > 0) {
                tell(level, body, CruiseTexts.HOLE.of(CruiseTexts.HOLED_NAME.text(), CruiseTexts.HOLED.text()));
                return true;
            }
        }
        return false;
    }

    /** A solar storm is told to {@code body}'s crew, and passes through its space a little later. */
    public static void storm(final ServerLevel level, final MovingStructure body) {
        tell(level, body, CruiseTexts.STORM.of(CruiseTexts.STORM_NAME.text(),
                CruiseTexts.STORM_COMING.with(STORM_AHEAD / TICKS_A_SECOND)));
        SolarStorms.coming(level, STORM_AHEAD, STORM_LASTS);
    }

    /** A part of {@code body} burns through and loses a block; whether one was lost. */
    public static boolean failure(final ServerLevel level, final MovingStructure body,
                                  final FlightComputerBlockEntity computer) {
        final List<BlockPos> hull = computer.formedPlaces(level);
        if (hull.isEmpty()) {
            return false;
        }
        final BlockPos lost = hull.get(level.random.nextInt(hull.size()));
        FormedDamage.blame(level, lost, DamageStamps.BURN, BURN_THROUGH);
        level.setBlockAndUpdate(lost, Blocks.AIR.defaultBlockState());
        tell(level, body, CruiseTexts.BREAKDOWN.of(CruiseTexts.FAILURE_NAME.text(), CruiseTexts.FAILURE.text()));
        return true;
    }

    /**
     * The air of the rooms behind {@code body}'s hull holes rushes out, a share for each hole a second: the
     * micrometeoroids' holes not patched yet, of the blocks {@code places}.
     */
    public static void leak(final ServerLevel level, final List<BlockPos> places) {
        for (final BlockPos pos : places) {
            if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
                continue;
            }
            for (final CellMark kept : part.damage()) {
                if (kept.mark().stamp().equals(DamageStamps.MICROMETEOROID) && kept.mark().lost().isEmpty()) {
                    final Optional<Room> room = SealedRooms.roomBeside(level, pos);
                    room.ifPresent(holed -> {
                        for (final Map.Entry<ResourceLocation, Double> gas : holed.air().moles().entrySet()) {
                            holed.take(gas.getKey(), gas.getValue() * LEAK);
                        }
                    });
                }
            }
        }
    }

    /** Tells everyone aboard {@code body} {@code notification}. */
    public static void tell(final ServerLevel level, final MovingStructure body, final Notification notification) {
        for (final ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                body.worldBox().inflate(ABOARD))) {
            Notifications.send(player, notification);
        }
    }

    /* Out of the rocket at {@code pos}: away from the line up its middle, level. */
    private static Vec3 outward(final FlightComputerBlockEntity computer, final BlockPos pos) {
        final BlockPos foot = computer.getBlockPos().offset(computer.foot().orElse(BlockPos.ZERO));
        final Vec3 out = new Vec3(pos.getX() + 0.5 - (foot.getX() + 0.5), 0, pos.getZ() + 0.5 - (foot.getZ() + 0.5));
        return out.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : out.normalize();
    }
}
