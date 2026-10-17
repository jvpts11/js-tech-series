/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;

/**
 * How things are pulled where a law of gravity holds, each strength in g, the overworld's pull at the ground: down at a
 * share of the usual pull (none for weightless), along a direction, or towards a point, as a small body or a micro
 * black hole pulls, harder the nearer.
 *
 * <pre>{@code
 * { "type": "scaled", "g": 0 }                                             // weightless
 * { "type": "towards", "x": 0, "y": 1, "z": 0, "g": 1 }                    // falling up
 * { "type": "centre", "x": 0.5, "y": 70, "z": 0.5, "g": 1, "radius": 6 }   // a planetoid: 1 g at its surface
 * }</pre>
 */
public sealed interface Pull permits Pull.Scaled, Pull.Towards, Pull.Centre {

    /** The strongest pull a law may hold, in g. */
    double MOST = 20.0;
    Codec<Pull> CODEC = StableCodecs.byName(Kind.class).dispatch("type", Pull::kind, Kind::codec);

    /** What kind of pull it is. */
    Kind kind();

    /**
     * Down, at {@code g} of the usual pull.
     *
     * @param g the share of the usual pull: 0 for weightless, 0.38 for Mars
     */
    record Scaled(double g) implements Pull {

        static final MapCodec<Scaled> MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.doubleRange(0, MOST).fieldOf("g").forGetter(Scaled::g)).apply(instance, Scaled::new));

        @Override
        public Kind kind() {
            return Kind.SCALED;
        }
    }

    /**
     * Along a direction, at {@code g}.
     *
     * @param x the direction across
     * @param y the direction up
     * @param z the direction along
     * @param g its strength
     */
    record Towards(double x, double y, double z, double g) implements Pull {

        static final MapCodec<Towards> MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Towards::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Towards::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Towards::z),
                Codec.doubleRange(0, MOST).optionalFieldOf("g", 1.0).forGetter(Towards::g))
                .apply(instance, Towards::new));

        @Override
        public Kind kind() {
            return Kind.TOWARDS;
        }
    }

    /**
     * Towards a point, at {@code g} at {@code radius} from it, falling with the square of the distance beyond and
     * rising within, to four times as hard at most.
     *
     * @param x      the point across
     * @param y      the point up
     * @param z      the point along
     * @param g      its strength at {@code radius}
     * @param radius where it pulls at {@code g}, in blocks
     */
    record Centre(double x, double y, double z, double g, double radius) implements Pull {

        static final MapCodec<Centre> MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Centre::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Centre::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Centre::z),
                Codec.doubleRange(0, MOST).optionalFieldOf("g", 1.0).forGetter(Centre::g),
                Codec.doubleRange(0.1, 1_000).optionalFieldOf("radius", 1.0).forGetter(Centre::radius))
                .apply(instance, Centre::new));

        @Override
        public Kind kind() {
            return Kind.CENTRE;
        }
    }

    /** The kinds of pull. */
    enum Kind implements IStableName {
        SCALED("scaled", Scaled.MAP),
        TOWARDS("towards", Towards.MAP),
        CENTRE("centre", Centre.MAP);

        private final String serializedName;
        private final MapCodec<? extends Pull> codec;

        Kind(final String serializedName, final MapCodec<? extends Pull> codec) {
            this.serializedName = serializedName;
            this.codec = codec;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        MapCodec<? extends Pull> codec() {
            return this.codec;
        }
    }
}
