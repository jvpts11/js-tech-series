/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.Nullable;

/**
 * Runs on the rails of the world, any mod's that are the game's kind of rail, as a train does: the structure's pivot
 * follows the track a little above it, straight, up and down the slopes and round the curves in a quarter circle,
 * turned along the way it goes, and stops where the track ends. The track is read as it goes, so a rail laid ahead of
 * a moving train is taken.
 *
 * @param rail  the rail the pivot is over now
 * @param from  the side of that rail it came in by
 * @param along how far through that rail it is, from 0 to 1
 * @param speed how fast it runs, in blocks a second
 * @param lift  how high its pivot stands above the track
 */
public record RailsMotion(BlockPos rail, Direction from, double along, double speed, double lift)
        implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "rails");
    public static final MapCodec<RailsMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf("rail").forGetter(RailsMotion::rail),
            Direction.CODEC.fieldOf("from").forGetter(RailsMotion::from),
            Codec.DOUBLE.fieldOf("along").forGetter(RailsMotion::along),
            Codec.DOUBLE.fieldOf("speed").forGetter(RailsMotion::speed),
            Codec.DOUBLE.fieldOf("lift").forGetter(RailsMotion::lift)
    ).apply(instance, RailsMotion::new));
    /* Ticks in a second; where a rail's track lies above its block's floor; how far down a rail is looked for. */
    private static final double TICKS = 20;
    private static final double TRACK = 1.0 / 16;
    private static final int LOOK_DOWN = 4;
    /* How fast the structure turns to follow the track, in radians a tick at most. */
    private static final double FOLLOW = Math.toRadians(18);
    /* A quarter circle of half a block, the length of the track round a curve. */
    private static final double CURVE = Math.PI / 4;

    /** @throws IllegalArgumentException when the speed is not above zero, or {@code from} is not level */
    public RailsMotion {
        if (!(speed > 0) || from.getAxis() == Direction.Axis.Y) {
            throw new IllegalArgumentException("a train runs at a speed, coming in by a side: " + speed + ", "
                    + from);
        }
    }

    /**
     * The rule that sets a structure running on the rail under its pivot, going {@code heading}; null when there is no
     * rail under it, or the rail does not run that way.
     */
    public static @Nullable RailsMotion onRails(final Level level, final StructurePose pose, final Direction heading,
                                                final double speed) {
        final BlockPos below = BlockPos.containing(pose.x(), pose.y(), pose.z());
        for (int down = 0; down <= LOOK_DOWN; down++) {
            final BlockPos at = below.below(down);
            final RailShape shape = shapeAt(level, at);
            if (shape != null) {
                final Direction from = heading.getOpposite();
                if (!runs(shape, from)) {
                    return null;
                }
                final RailsMotion motion = new RailsMotion(at, from, 0.5, speed, 0);
                return new RailsMotion(at, from, 0.5, speed, pose.y() - motion.track(shape)[1]);
            }
        }
        return null;
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        final Level level = context.world();
        RailShape shape = level == null ? null : shapeAt(level, rail);
        if (shape == null || !runs(shape, from)) {
            return new Step(now, null);
        }
        BlockPos at = rail;
        Direction in = from;
        double through = along;
        double left = speed / TICKS;
        boolean ended = false;
        while (left > 0) {
            final Direction out = exit(shape, in);
            final double length = length(shape);
            final double room = (1 - through) * length;
            if (left < room) {
                through += left / length;
                left = 0;
                continue;
            }
            left -= room;
            final BlockPos ahead = next(level, at, shape, out);
            final RailShape aheadShape = ahead == null ? null : shapeAt(level, ahead);
            if (aheadShape == null || !runs(aheadShape, out.getOpposite())) {
                through = 1;
                ended = true;
                break;
            }
            at = ahead;
            in = out.getOpposite();
            shape = aheadShape;
            through = 0;
        }
        final RailsMotion on = new RailsMotion(at, in, through, speed, lift);
        final double[] point = on.track(shape);
        final StructurePose aimed = MotionMath.facing(now, point[3], point[4], point[5]);
        final StructurePose turned = MotionMath.towards(now, aimed, 0, FOLLOW);
        final StructurePose next = turned.movedTo(point[0], point[1] + lift, point[2]);
        return new Step(next, ended ? null : on);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public boolean readsWorld() {
        return true;
    }

    /*
     * Where on the track it is and which way the track runs there, as {x, y, z, dx, dy, dz}: a straight rail from the
     * middle of the side it came in by to the middle of the side it goes out by, a slope rising a block on the way, a
     * curve round the corner between the two sides.
     */
    private double[] track(final RailShape shape) {
        final Direction out = exit(shape, from);
        final double cx = rail.getX() + 0.5;
        final double cz = rail.getZ() + 0.5;
        final double floor = rail.getY() + TRACK;
        final double inY = floor + (rises(shape, from) ? 1 : 0);
        final double outY = floor + (rises(shape, out) ? 1 : 0);
        final double ex = cx + from.getStepX() * 0.5;
        final double ez = cz + from.getStepZ() * 0.5;
        final double xx = cx + out.getStepX() * 0.5;
        final double xz = cz + out.getStepZ() * 0.5;
        if (from.getAxis() == out.getAxis()) {
            return new double[] {ex + (xx - ex) * along, inY + (outY - inY) * along, ez + (xz - ez) * along,
                    xx - ex, outY - inY, xz - ez};
        }
        final double cornerX = cx + (from.getStepX() + out.getStepX()) * 0.5;
        final double cornerZ = cz + (from.getStepZ() + out.getStepZ()) * 0.5;
        final double ux = (ex - cornerX) * 2;
        final double uz = (ez - cornerZ) * 2;
        final double vx = (xx - cornerX) * 2;
        final double vz = (xz - cornerZ) * 2;
        final double angle = along * Math.PI / 2;
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        return new double[] {cornerX + 0.5 * (cos * ux + sin * vx), floor, cornerZ + 0.5 * (cos * uz + sin * vz),
                -sin * ux + cos * vx, 0, -sin * uz + cos * vz};
    }

    /* The rail's shape at {@code pos}, or null for no rail of the game's kind. */
    private static @Nullable RailShape shapeAt(final Level level, final BlockPos pos) {
        if (!Loaded.at(level, pos)) {
            return null;
        }
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock railBlock)) {
            return null;
        }
        return railBlock.getRailDirection(state, level, pos, null);
    }

    /* The next rail along {@code out}: level with this one, a block up at a slope's top, or a block down. */
    private static @Nullable BlockPos next(final Level level, final BlockPos at, final RailShape shape,
                                           final Direction out) {
        final BlockPos ahead = at.relative(out);
        if (rises(shape, out)) {
            return ahead.above();
        }
        if (shapeAt(level, ahead) != null) {
            return ahead;
        }
        final BlockPos lower = ahead.below();
        return shapeAt(level, lower) != null ? lower : null;
    }

    /* Whether a rail of {@code shape} has a side {@code side}. */
    private static boolean runs(final RailShape shape, final Direction side) {
        final Direction[] sides = sides(shape);
        return sides[0] == side || sides[1] == side;
    }

    /* The other side of a rail of {@code shape} from {@code in}. */
    private static Direction exit(final RailShape shape, final Direction in) {
        final Direction[] sides = sides(shape);
        return sides[0] == in ? sides[1] : sides[0];
    }

    /* Whether the track of a slope rises toward {@code side}. */
    private static boolean rises(final RailShape shape, final Direction side) {
        return switch (shape) {
            case ASCENDING_EAST -> side == Direction.EAST;
            case ASCENDING_WEST -> side == Direction.WEST;
            case ASCENDING_NORTH -> side == Direction.NORTH;
            case ASCENDING_SOUTH -> side == Direction.SOUTH;
            default -> false;
        };
    }

    /* How long the track through a rail of {@code shape} is. */
    private static double length(final RailShape shape) {
        return switch (shape) {
            case ASCENDING_EAST, ASCENDING_WEST, ASCENDING_NORTH, ASCENDING_SOUTH -> Math.sqrt(2);
            case NORTH_SOUTH, EAST_WEST -> 1;
            default -> CURVE;
        };
    }

    /* The two sides a rail of {@code shape} joins. */
    private static Direction[] sides(final RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Direction[] {Direction.NORTH, Direction.SOUTH};
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Direction[] {Direction.EAST, Direction.WEST};
            case SOUTH_EAST -> new Direction[] {Direction.SOUTH, Direction.EAST};
            case SOUTH_WEST -> new Direction[] {Direction.SOUTH, Direction.WEST};
            case NORTH_WEST -> new Direction[] {Direction.NORTH, Direction.WEST};
            case NORTH_EAST -> new Direction[] {Direction.NORTH, Direction.EAST};
        };
    }
}
