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
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Goes through a list of poses one after the other, and stops at the last or starts over: a cable car between its
 * stations, a ferry's crossing, a patrol. Each leg is gone as {@link TowardMotion} goes.
 *
 * @param points   the poses to go through, in order
 * @param at       the pose it is going to now
 * @param speed    the most it moves, in blocks a second
 * @param turnRate the most it turns, in degrees a second; 0 turns it at once
 * @param loop     whether it starts over from the first after the last
 */
public record PathMotion(List<StructurePose> points, int at, double speed, double turnRate, boolean loop)
        implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "path");
    public static final MapCodec<PathMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StructureCodecs.POSE.listOf().fieldOf("points").forGetter(PathMotion::points),
            Codec.INT.fieldOf("at").forGetter(PathMotion::at),
            Codec.DOUBLE.fieldOf("speed").forGetter(PathMotion::speed),
            Codec.DOUBLE.fieldOf("turn_rate").forGetter(PathMotion::turnRate),
            Codec.BOOL.fieldOf("loop").forGetter(PathMotion::loop)
    ).apply(instance, PathMotion::new));

    /** @throws IllegalArgumentException when there is no point, no point at {@code at}, or no speed */
    public PathMotion {
        points = List.copyOf(points);
        if (points.isEmpty() || at < 0 || at >= points.size() || !(speed > 0) || turnRate < 0) {
            throw new IllegalArgumentException("a path has points, a place on them and a speed: " + points.size()
                    + ", " + at + ", " + speed);
        }
    }

    /** A path through {@code points} from the first, at {@code speed} blocks a second. */
    public static PathMotion through(final List<StructurePose> points, final double speed, final double turnRate,
                                     final boolean loop) {
        return new PathMotion(points, 0, speed, turnRate, loop);
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        final Step leg = new TowardMotion(points.get(at), speed, turnRate).step(now, context);
        if (leg.next() != null) {
            return new Step(leg.pose(), this);
        }
        final int then = at + 1;
        if (then < points.size()) {
            return new Step(leg.pose(), new PathMotion(points, then, speed, turnRate, loop));
        }
        return new Step(leg.pose(), loop ? new PathMotion(points, 0, speed, turnRate, true) : null);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
