/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a spotlight's or a beam's light goes: its rays through its cone ({@link LightMath#cone}), turned the way the
 * lamp faces, each going on through air, water and what light passes (glass) until it meets what light does not; the
 * last open place before that is lit, as bright as the light still is there ({@link LightMath#along}). The server
 * lights those places; the player's game draws the beam the same way, so both agree.
 */
public final class Beams {

    /* How far out of its block a ray starts, so it leaves the lamp's own block. */
    private static final double START = 0.6;
    /* The rings of rays a spotlight's cone is cast with, a beam's narrow cone, and how far a ray steps. */
    private static final int SPOT_RINGS = 4;
    private static final int BEAM_RINGS = 1;
    private static final double BEAM_ANGLE = 6;
    private static final double STEP = 0.5;

    private Beams() {
    }

    /**
     * The places a lamp of {@code kind} at {@code from} facing {@code facing} lights while it shines at
     * {@code intensity}: a spotlight's cone or a beam's narrow one, as the server lays them and the player's game
     * draws them.
     */
    public static Map<BlockPos, BeamLights.Lit> lamp(final BlockGetter level, final BlockPos from,
                                                    final Direction facing, final LightKind kind,
                                                    final int intensity) {
        final boolean beam = kind.shape() == LightKind.Shape.BEAM;
        return cast(level, from, facing, beam ? BEAM_ANGLE : kind.angle(), beam ? BEAM_RINGS : SPOT_RINGS,
                kind.reach(), intensity, kind.colour(), STEP);
    }

    /** The rays of a lamp of {@code kind}, for what draws its light's shaft. */
    public static List<Ray> lampRays(final BlockGetter level, final BlockPos from, final Direction facing,
                                     final LightKind kind) {
        final boolean beam = kind.shape() == LightKind.Shape.BEAM;
        return rays(level, from, facing, beam ? BEAM_ANGLE : kind.angle(), beam ? BEAM_RINGS : SPOT_RINGS,
                kind.reach(), STEP);
    }

    /**
     * The places a light at {@code from} facing {@code facing} lights, with a cone of {@code angle} degrees in
     * {@code rings} rings, reaching {@code reach} blocks, its rays stepping {@code step} blocks at a time.
     */
    public static Map<BlockPos, BeamLights.Lit> cast(final BlockGetter level, final BlockPos from,
                                                    final Direction facing, final double angle, final int rings,
                                                    final int reach, final int intensity, final int colour,
                                                    final double step) {
        final Map<BlockPos, BeamLights.Lit> lit = new HashMap<>();
        for (final Ray ray : rays(level, from, facing, angle, rings, reach, step)) {
            if (ray.hit() == null) {
                continue;
            }
            final int brightness = LightMath.along(intensity, ray.length(), reach);
            if (brightness > 0) {
                lit.merge(ray.hit(), new BeamLights.Lit(brightness, colour), (a, b) -> a.level() >= b.level() ? a : b);
            }
        }
        return lit;
    }

    /** The rays of such a light, each from its start to where it ends, and the place it lights if it meets one. */
    public static List<Ray> rays(final BlockGetter level, final BlockPos from, final Direction facing,
                                 final double angle, final int rings, final int reach, final double step) {
        Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 side = Vec3.atLowerCornerOf((facing.getAxis() == Direction.Axis.Y ? Direction.NORTH
                : Direction.UP).getNormal()).cross(forward).normalize();
        Vec3 up = forward.cross(side).normalize();
        Vec3 origin = Vec3.atCenterOf(from).add(forward.scale(START));
        // A lamp aboard a moving structure shines on the world from where it is, the way its structure turns it.
        final IStructureBody aboard = level instanceof Level world && YardLayout.isYardBlock(from.getX())
                ? MovingStructures.at(world, from) : null;
        if (aboard != null) {
            origin = MovingStructures.toWorld(aboard, origin);
            forward = turned(aboard, forward);
            side = turned(aboard, side);
            up = turned(aboard, up);
        }
        final List<Ray> out = new ArrayList<>();
        for (final double[] local : LightMath.cone(angle, rings)) {
            final Vec3 way = side.scale(local[0]).add(up.scale(local[1])).add(forward.scale(local[2]));
            BlockPos last = null;
            BlockPos hit = null;
            double travelled = 0;
            while (travelled <= reach) {
                final BlockPos at = BlockPos.containing(origin.add(way.scale(travelled)));
                if (!at.equals(from)) {
                    final BlockState state = level.getBlockState(at);
                    if (LightBlock.fits(state)) {
                        last = at;
                    } else if (state.getLightBlock(level, at) > 0) {
                        // What light does not pass stops the ray; glass and the like it shines through.
                        hit = last;
                        break;
                    }
                }
                travelled += step;
            }
            out.add(new Ray(origin, origin.add(way.scale(Math.min(travelled, reach))), hit,
                    Math.min(travelled, reach)));
        }
        return out;
    }

    /**
     * Whether the lamp at {@code pos} is aboard a moving structure that moved this tick: its beam sweeps the world as
     * it goes, and is cast again often.
     */
    public static boolean goingAboard(final Level level, final BlockPos pos) {
        if (!YardLayout.isYardBlock(pos.getX())) {
            return false;
        }
        final IStructureBody body = MovingStructures.at(level, pos);
        return body != null && !body.frame().pose().equals(body.previousFrame().pose());
    }

    /* The world's way of the structure's way {@code way}, as {@code body} is turned now. */
    private static Vec3 turned(final IStructureBody body, final Vec3 way) {
        final StructureFrame frame = body.frame();
        return new Vec3(frame.turnX(way.x, way.y, way.z), frame.turnY(way.x, way.y, way.z),
                frame.turnZ(way.x, way.y, way.z));
    }

    /**
     * A ray of light.
     *
     * @param start  where it starts
     * @param end    where it ends: what it meets, or its reach
     * @param hit    the place it lights before what it meets; none where it meets nothing within its reach
     * @param length how far it goes, in blocks
     */
    public record Ray(Vec3 start, Vec3 end, @Nullable BlockPos hit, double length) {
    }
}
