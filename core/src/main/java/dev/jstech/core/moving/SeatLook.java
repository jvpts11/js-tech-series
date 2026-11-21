/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

/**
 * The look of one sitting on a seat of a moving structure, kept in the seat's own frame, the structure's: carried
 * round as the structure turns about any axis, so a pilot looking ahead along a nose pitched up still looks ahead
 * along it; turned by the mouse about the seat's up and across its right, from straight up to straight down as the
 * seat has them; and the roll of the view that keeps the seat's up at the top of it. The look stays the sitter's own,
 * in the world, so what they aim at and use is what they see.
 */
@ApiStatus.Internal
public final class SeatLook {

    /* How far a unit of the mouse turns the look, in degrees, as the game's own turn has it. */
    private static final double DEGREES_PER_STEP = 0.15;
    /* How far up or down from the seat's level the look goes, in degrees. */
    private static final double STRAIGHT = 90;
    /* How far a frame's up may lean from the world's and the seat still count as level. */
    private static final double LEVEL = 1.0e-6;

    private SeatLook() {
    }

    /** Whether {@code frame} leans or pitches its seats at all, so their sitters look in it rather than the world. */
    @ApiStatus.Internal
    public static boolean tilted(final StructureFrame frame) {
        return 1 - frame.turnY(0, 1, 0) > LEVEL;
    }

    /**
     * Turns {@code sitter}'s look as the structure turned from {@code before} to {@code now}, keeping where it looked
     * in the seat's frame; the look a tick before is left where it was, so the view between turns frame by frame.
     */
    @ApiStatus.Internal
    public static void carry(final Entity sitter, final StructureFrame before, final StructureFrame now) {
        final Vec3 local = into(before, direction(sitter.getYRot(), sitter.getXRot()));
        set(sitter, out(now, local), false);
    }

    /**
     * Turns {@code sitter}'s look by the mouse's {@code yaw} and {@code pitch}, as the game's turn takes them, about
     * the seat's up and across its right in {@code frame}, from straight up to straight down from the seat.
     */
    @ApiStatus.Internal
    public static void turn(final Entity sitter, final StructureFrame frame, final double yaw, final double pitch) {
        final Vec3 local = into(frame, direction(sitter.getYRot(), sitter.getXRot()));
        final double turnedYaw = yawOf(local) + yaw * DEGREES_PER_STEP;
        final double turnedPitch = Mth.clamp(pitchOf(local) + pitch * DEGREES_PER_STEP, -STRAIGHT, STRAIGHT);
        set(sitter, out(frame, direction(turnedYaw, turnedPitch)), true);
    }

    /**
     * How far a view facing {@code yaw} and {@code pitch} rolls, in degrees, for the seat's up in {@code frame} to
     * stand at the top of it: how far that up leans to the view's right.
     */
    @ApiStatus.Internal
    public static float roll(final StructureFrame frame, final float yaw, final float pitch) {
        final Vec3 forward = direction(yaw, pitch);
        final double radians = Math.toRadians(yaw);
        final Vec3 right = new Vec3(-Math.cos(radians), 0, -Math.sin(radians));
        final Vec3 top = right.cross(forward);
        final Vec3 up = out(frame, new Vec3(0, 1, 0));
        return (float) Math.toDegrees(Math.atan2(up.dot(right), up.dot(top)));
    }

    /*
     * Sets {@code sitter} looking along {@code look}, its yaw turned the short way from where it faced; with
     * {@code again}, its look a tick before turned as far, as the game's own turn by the mouse has it.
     */
    private static void set(final Entity sitter, final Vec3 look, final boolean again) {
        final float yawTurn = Mth.wrapDegrees((float) yawOf(look) - sitter.getYRot());
        final float pitch = (float) pitchOf(look);
        final float pitchTurn = pitch - sitter.getXRot();
        sitter.setYRot(sitter.getYRot() + yawTurn);
        sitter.setXRot(pitch);
        sitter.setYHeadRot(sitter.getYHeadRot() + yawTurn);
        if (again) {
            sitter.yRotO += yawTurn;
            sitter.xRotO += pitchTurn;
        }
    }

    /* The world's way {@code way} in the frame's own. */
    private static Vec3 into(final StructureFrame frame, final Vec3 way) {
        return new Vec3(frame.unturnX(way.x, way.y, way.z), frame.unturnY(way.x, way.y, way.z),
                frame.unturnZ(way.x, way.y, way.z));
    }

    /* The frame's way {@code way} in the world. */
    private static Vec3 out(final StructureFrame frame, final Vec3 way) {
        return new Vec3(frame.turnX(way.x, way.y, way.z), frame.turnY(way.x, way.y, way.z),
                frame.turnZ(way.x, way.y, way.z));
    }

    /* The way a look of the game's {@code yaw} and {@code pitch} points, in degrees. */
    private static Vec3 direction(final double yaw, final double pitch) {
        final double y = Math.toRadians(yaw);
        final double p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    private static double yawOf(final Vec3 way) {
        return Math.toDegrees(Math.atan2(-way.x, way.z));
    }

    private static double pitchOf(final Vec3 way) {
        return -Math.toDegrees(Math.asin(Mth.clamp(way.y / Math.max(way.length(), LEVEL), -1, 1)));
    }
}
