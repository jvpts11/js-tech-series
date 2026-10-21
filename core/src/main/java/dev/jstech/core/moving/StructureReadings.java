/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * What a sensor aboard a moving structure reads of its flight, as it speaks it in redstone, to programs and to the
 * network: its altitude, the air's pressure, its speed and its climb, its pitch and roll and its heading by the way the
 * sensor looks, and how far the ground is below it and the first block ahead of it. A sensor on the ground reads the
 * same, standing still. So an autopilot can be built of redstone.
 *
 * <pre>{@code
 * public List<Reading> readings() {   // a flight sensor
 *     return StructureReadings.of(level, worldPosition, getBlockState().getValue(FACING));
 * }
 * }</pre>
 */
@TextHolder
public final class StructureReadings {

    public static final TextKey ALTITUDE = TextKey.of("jscore.moving.reading.altitude", "Altitude");
    public static final TextKey PRESSURE = TextKey.of("jscore.moving.reading.pressure", "Air pressure");
    public static final TextKey SPEED = TextKey.of("jscore.moving.reading.speed", "Speed");
    public static final TextKey CLIMB = TextKey.of("jscore.moving.reading.climb", "Climb");
    public static final TextKey PITCH = TextKey.of("jscore.moving.reading.pitch", "Pitch");
    public static final TextKey ROLL = TextKey.of("jscore.moving.reading.roll", "Roll");
    public static final TextKey HEADING = TextKey.of("jscore.moving.reading.heading", "Heading");
    public static final TextKey GROUND = TextKey.of("jscore.moving.reading.ground", "Height above the ground");
    public static final TextKey AHEAD = TextKey.of("jscore.moving.reading.ahead", "Distance ahead");
    public static final TextKey METRES = TextKey.of("jscore.moving.reading.metres", "m");
    public static final TextKey KILOPASCALS = TextKey.of("jscore.moving.reading.kilopascals", "kPa");
    public static final TextKey METRES_A_SECOND = TextKey.of("jscore.moving.reading.metres_a_second", "m/s");
    public static final TextKey DEGREES = TextKey.of("jscore.moving.reading.degrees", "°");
    /** How far below and ahead a sensor looks, in blocks: farther is read as this far. */
    public static final double LOOKS = 64;
    /* The spans each reading is spoken over in redstone. */
    private static final double MOST_PRESSURE = 120;
    private static final double MOST_SPEED = 100;
    private static final double MOST_CLIMB = 20;
    private static final double QUARTER = 90;
    private static final double FULL_TURN = 360;
    /* How finely the ground and what is ahead are looked for, in blocks. */
    private static final double STEP = 0.25;
    private static final double TICKS = 20;

    private StructureReadings() {
    }

    /**
     * What a sensor at {@code pos} looking {@code facing} reads: aboard the structure whose yard holds {@code pos},
     * as it stands and goes now; else where it stands in the world, still.
     */
    public static List<Reading> of(final Level level, final BlockPos pos, final Direction facing) {
        final IStructureBody body = YardLayout.isYardBlock(pos.getX()) ? MovingStructures.at(level, pos) : null;
        final Vec3 middle = Vec3.atCenterOf(pos);
        final Vec3 at = body == null ? middle : MovingStructures.toWorld(body, middle);
        final Vec3 ahead = body == null ? Vec3.atLowerCornerOf(facing.getNormal()) : turned(body, facing);
        final Vec3 up = body == null ? new Vec3(0, 1, 0) : turned(body, Direction.UP);
        final Vec3 going = velocityOf(body, at);
        final Vec3 right = ahead.cross(up).lengthSqr() > 0 ? ahead.cross(up).normalize() : new Vec3(1, 0, 0);
        final BlockPos world = BlockPos.containing(at);
        return List.of(
                new Reading(ALTITUDE, at.y, level.getMinBuildHeight(), level.getMaxBuildHeight(), METRES),
                new Reading(PRESSURE, HeatClimate.pressure(level, world), 0, MOST_PRESSURE, KILOPASCALS),
                new Reading(SPEED, going.length(), 0, MOST_SPEED, METRES_A_SECOND),
                new Reading(CLIMB, going.y, -MOST_CLIMB, MOST_CLIMB, METRES_A_SECOND),
                new Reading(PITCH, Math.toDegrees(Math.asin(clamp(ahead.y))), -QUARTER, QUARTER, DEGREES),
                new Reading(ROLL, Math.toDegrees(Math.asin(clamp(-right.y))), -QUARTER, QUARTER, DEGREES),
                new Reading(HEADING, heading(ahead), 0, FULL_TURN, DEGREES),
                new Reading(GROUND, distance(level, at, new Vec3(0, -1, 0)), 0, LOOKS, METRES),
                new Reading(AHEAD, distance(level, at, ahead), 0, LOOKS, METRES));
    }

    /** How far below the world's place {@code at} the ground is, in blocks, up to {@link #LOOKS}. */
    public static double below(final Level level, final Vec3 at) {
        return distance(level, at, new Vec3(0, -1, 0));
    }

    /** What a sensor aboard reads, looking the way its block looks. */
    public static List<Reading> of(final Level level, final BlockPos pos) {
        return of(level, pos, BlockPhysics.wayOf(level.getBlockState(pos)));
    }

    /*
     * How far from {@code at} along {@code way} the first block of the world that stops what moves is, in blocks, up
     * to {@link #LOOKS}: only the world's blocks, never the structure's own. A place not loaded stops the look there.
     */
    private static double distance(final Level level, final Vec3 at, final Vec3 way) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double travelled = STEP; travelled <= LOOKS; travelled += STEP) {
            pos.set(at.x + way.x * travelled, at.y + way.y * travelled, at.z + way.z * travelled);
            if (!Loaded.at(level, pos)) {
                return travelled;
            }
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return travelled;
            }
        }
        return LOOKS;
    }

    /* Where the way {@code way} looks, the heading from north round by east, 0 to 360 degrees. */
    private static double heading(final Vec3 way) {
        if (way.x * way.x + way.z * way.z < 1.0e-9) {
            return 0;
        }
        final double degrees = Math.toDegrees(Math.atan2(way.x, -way.z));
        return degrees < 0 ? degrees + FULL_TURN : degrees;
    }

    /* How fast the structure's point at the world's place {@code at} goes, in metres a second. */
    private static Vec3 velocityOf(@Nullable final IStructureBody body, final Vec3 at) {
        if (!(body instanceof MovingStructure structure)) {
            return Vec3.ZERO;
        }
        final StructurePhysics physics = StructureMounts.rootOf(structure).physics();
        if (physics != null && physics.active()) {
            final Vector3d point = physics.body().velocityAt(new Vector3d(at.x, at.y, at.z), new Vector3d());
            return new Vec3(point.x, point.y, point.z);
        }
        final double[] moved = structure.velocity();
        return new Vec3(moved[0], moved[1], moved[2]).scale(TICKS);
    }

    private static Vec3 turned(final IStructureBody body, final Direction way) {
        final StructureFrame frame = body.frame();
        return new Vec3(frame.turnX(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnY(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnZ(way.getStepX(), way.getStepY(), way.getStepZ()));
    }

    private static double clamp(final double value) {
        return Math.max(-1, Math.min(1, value));
    }
}
