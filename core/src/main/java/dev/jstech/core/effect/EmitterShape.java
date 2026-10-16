/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Where an emitter's particles start, in its own frame, its way out along Y: from a point, within a sphere or a box,
 * across a disc facing its way, from a cone's tip, along a line, or over the faces of one of the Core's models.
 *
 * <pre>{@code
 * {"type": "disc", "radius": 0.4}   {"type": "cone", "angle": 30}   {"type": "mesh", "model": "mymod:chimney"}
 * }</pre>
 *
 * @param type   which
 * @param radius a sphere's or a disc's radius, in blocks
 * @param angle  a cone's angle across, in degrees: its particles leave within it
 * @param size   a box's or a line's size, in blocks: a box's sides are all this, a line is this long
 * @param model  a mesh's model, one of the Core's declared models
 */
public record EmitterShape(Type type, double radius, double angle, double size, String model) {

    private static final double MOST = 256;
    private static final double MOST_ANGLE = 180;
    /** A point. */
    public static final EmitterShape POINT = new EmitterShape(Type.POINT, 0, 0, 0, "");
    public static final Codec<EmitterShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Type.class).optionalFieldOf("type", Type.POINT).forGetter(EmitterShape::type),
            Codec.doubleRange(0, MOST).optionalFieldOf("radius", 0.0).forGetter(EmitterShape::radius),
            Codec.doubleRange(0, MOST_ANGLE).optionalFieldOf("angle", 0.0).forGetter(EmitterShape::angle),
            Codec.doubleRange(0, MOST).optionalFieldOf("size", 0.0).forGetter(EmitterShape::size),
            Codec.STRING.optionalFieldOf("model", "").forGetter(EmitterShape::model))
            .apply(instance, EmitterShape::new));

    public EmitterShape {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(model, "model");
    }

    /**
     * Picks where a particle starts, in the emitter's frame, into {@code at}, and the way it leaves into {@code way}
     * (a unit vector): along Y but for a cone's spread, a sphere's outward. A mesh's points are its model's, picked
     * by what draws it, so here it starts at the middle.
     */
    public void sample(final RandomGenerator random, final double[] at, final double[] way) {
        at[0] = 0;
        at[1] = 0;
        at[2] = 0;
        way[0] = 0;
        way[1] = 1;
        way[2] = 0;
        switch (this.type) {
            case POINT, MESH -> {
            }
            case SPHERE -> ShapeSampling.sphere(random, this.radius, at, way);
            case BOX -> ShapeSampling.box(random, this.size, at);
            case DISC -> ShapeSampling.disc(random, this.radius, at);
            case CONE -> ShapeSampling.cone(random, this.angle, way);
            case LINE -> ShapeSampling.line(random, this.size, at);
        }
    }

    /** The shapes. */
    public enum Type implements IStableName {
        POINT("point"),
        SPHERE("sphere"),
        BOX("box"),
        DISC("disc"),
        CONE("cone"),
        LINE("line"),
        MESH("mesh");

        private final String serializedName;

        Type(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
