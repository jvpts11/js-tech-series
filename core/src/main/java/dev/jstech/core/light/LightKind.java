/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Objects;

/**
 * A kind of light, as data: its colour, how bright it is, how far it reaches, its shape (a point, a spotlight's cone,
 * a beam), how it flickers, what turns it on, and whether it can break. A lamp ({@link LampBlock}) is of one kind; a
 * data pack changes a kind by writing its file under {@code data/<namespace>/jstech/lights/}.
 *
 * <pre>{@code
 * { "colour": "#FF2010", "intensity": 15, "flicker": "pulse", "control": "redstone" }
 * { "colour": "#FFF6E8", "intensity": 15, "shape": "spot", "reach": 48, "angle": 30, "control": "power", "watts": 200 }
 * }</pre>
 *
 * @param colour    its colour, {@code 0xRRGGBB}
 * @param intensity how bright it is where it is, from 1 to 15, as the game's light levels
 * @param reach     how far a spotlight or a beam reaches, in blocks, past the game's fifteen
 * @param shape     a point, a spotlight's cone or a beam
 * @param angle     a spotlight's cone, in degrees across; 0 for a light with none, a point's or a beam's
 * @param flicker   how its brightness moves in time
 * @param control   what turns it on
 * @param watts     what it draws while on, for one turned on by power
 * @param breaks    whether it can break, going dark until mended
 */
public record LightKind(int colour, int intensity, int reach, Shape shape, double angle, Flicker flicker,
                       Control control, double watts, boolean breaks) {

    private static final int MOST_REACH = 256;
    private static final double MOST_ANGLE = 179;
    private static final double DEFAULT_ANGLE = 40;
    /** A plain white lamp, always on. */
    public static final LightKind PLAIN = new LightKind(LightMath.WHITE, LightMath.BRIGHTEST, LightMath.BRIGHTEST,
            Shape.POINT, 0, Flicker.STEADY, Control.ALWAYS, 0, false);
    /** As a file writes it, every field but the intensity left out for its plain value. */
    public static final Codec<LightKind> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LightColours.COLOUR.optionalFieldOf("colour", LightMath.WHITE).forGetter(LightKind::colour),
            Codec.intRange(1, LightMath.BRIGHTEST).optionalFieldOf("intensity", LightMath.BRIGHTEST)
                    .forGetter(LightKind::intensity),
            Codec.intRange(1, MOST_REACH).optionalFieldOf("reach", LightMath.BRIGHTEST).forGetter(LightKind::reach),
            StableCodecs.byName(Shape.class).optionalFieldOf("shape", Shape.POINT).forGetter(LightKind::shape),
            Codec.doubleRange(0, MOST_ANGLE).optionalFieldOf("angle", DEFAULT_ANGLE).forGetter(LightKind::angle),
            flickers().optionalFieldOf("flicker", Flicker.STEADY).forGetter(LightKind::flicker),
            StableCodecs.byName(Control.class).optionalFieldOf("control", Control.ALWAYS)
                    .forGetter(LightKind::control),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("watts", 0.0).forGetter(LightKind::watts),
            Codec.BOOL.optionalFieldOf("breaks", false).forGetter(LightKind::breaks))
            .apply(instance, LightKind::new));

    public LightKind {
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(flicker, "flicker");
        Objects.requireNonNull(control, "control");
    }

    /** Its level now, from its intensity and its flicker at {@code seconds}, for the light {@code seed} starts. */
    public int levelAt(final double seconds, final long seed) {
        if (this.flicker.steady()) {
            return this.intensity;
        }
        return (int) Math.clamp(Math.round(this.intensity * this.flicker.brightness(seconds, seed)), 0,
                LightMath.BRIGHTEST);
    }

    /* A flicker as a file writes it: one of the named ones by its name, or its own letters, rate and looping. */
    private static Codec<Flicker> flickers() {
        final Codec<Flicker> own = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("pattern").forGetter(Flicker::pattern),
                Codec.DOUBLE.optionalFieldOf("rate", 10.0).forGetter(Flicker::rate),
                Codec.BOOL.optionalFieldOf("loops", true).forGetter(Flicker::loops))
                .apply(instance, Flicker::new));
        return Codec.either(Codec.STRING, own).xmap(either -> either.map(name -> Flicker.NAMED.getOrDefault(name,
                Flicker.STEADY), flicker -> flicker), Either::right);
    }

    /** The shapes of light. */
    public enum Shape implements IStableName {
        /** Light all round, as a lamp's. */
        POINT("point"),
        /** A cone the way the lamp faces, as a spotlight's. */
        SPOT("spot"),
        /** A narrow beam the way it faces, as a searchlight's. */
        BEAM("beam");

        private final String stableName;

        Shape(final String stableName) {
            this.stableName = stableName;
        }

        @Override
        public String serializedName() {
            return this.stableName;
        }
    }

    /** What turns a light on. */
    public enum Control implements IStableName {
        /** It is always on. */
        ALWAYS("always"),
        /** Redstone turns it on. */
        REDSTONE("redstone"),
        /** Power turns it on: it draws its watts while it is on. */
        POWER("power");

        private final String stableName;

        Control(final String stableName) {
            this.stableName = stableName;
        }

        @Override
        public String serializedName() {
            return this.stableName;
        }
    }
}
