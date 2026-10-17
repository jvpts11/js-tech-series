/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.region.Box;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Laws of their own for a place, a dimension or a body, each optional, the usual ones holding where none is said: how
 * things are pulled ({@link Pull}), how fast time runs, what light does, how space joins up, and how matter behaves.
 *
 * <pre>{@code
 * { "gravity": { "type": "scaled", "g": 0 }, "time": 0.5,
 *   "light": { "tint": "#C0D0FF", "shift": -0.4, "lens": 0.3 },
 *   "space": { "kind": "wrap" }, "matter": { "fluids_rise": true, "explosion_speed": 4 } }
 * }</pre>
 *
 * @param gravity how things are pulled
 * @param time    how fast time runs, as a share of the usual: a half for half as fast, 2 for twice
 * @param light   what light does
 * @param space   how space joins up
 * @param matter  how matter behaves
 */
public record Laws(Optional<Pull> gravity, Optional<Double> time, Optional<Light> light, Optional<Space> space,
                   Optional<Matter> matter) {

    /** The slowest and the fastest time may run. */
    public static final double SLOWEST = 0.0;
    public static final double FASTEST = 10.0;
    /** No laws of their own: the usual ones hold. */
    public static final Laws NONE = new Laws(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    public static final Codec<Box> BOX = Codec.INT.listOf(6, 6).xmap(
            corners -> Box.between(corners.get(0), corners.get(1), corners.get(2), corners.get(3), corners.get(4),
                    corners.get(5)),
            box -> List.of(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
    public static final Codec<Laws> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Pull.CODEC.optionalFieldOf("gravity").forGetter(Laws::gravity),
            Codec.doubleRange(SLOWEST, FASTEST).optionalFieldOf("time").forGetter(Laws::time),
            Light.CODEC.optionalFieldOf("light").forGetter(Laws::light),
            Space.CODEC.optionalFieldOf("space").forGetter(Laws::space),
            Matter.CODEC.optionalFieldOf("matter").forGetter(Laws::matter))
            .apply(instance, Laws::new));

    /** Whether it holds no law of its own. */
    public boolean none() {
        return this.gravity.isEmpty() && this.time.isEmpty() && this.light.isEmpty() && this.space.isEmpty()
                && this.matter.isEmpty();
    }

    /** These laws with every law {@code over} holds in their place. */
    public Laws under(final Laws over) {
        return new Laws(over.gravity.or(() -> this.gravity), over.time.or(() -> this.time),
                over.light.or(() -> this.light), over.space.or(() -> this.space), over.matter.or(() -> this.matter));
    }

    /** How fast time runs under these laws: 1 for the usual. */
    public double timeRate() {
        return this.time.orElse(1.0);
    }

    /**
     * What light does: its colour tinted, shifted towards red or blue as by the Doppler effect, bent as round a mass,
     * shimmering as in a mirage, and a darkness or a glow with no source. Only what is seen.
     *
     * @param tint   the colour everything is seen through
     * @param shift  how far colours shift, towards red above 0, towards blue below, -1 to 1
     * @param lens   how hard the view bends round the area's middle, 0 to 1
     * @param mirage how hard the view shimmers, 0 to 1
     * @param dark   how dark it is with nothing to make it so, 0 to 1
     * @param glow   how bright it is with nothing to light it, 0 to 1
     */
    public record Light(int tint, double shift, double lens, double mirage, double dark, double glow) {

        public static final Codec<Light> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LightColours.COLOUR.optionalFieldOf("tint", 0xFFFFFF).forGetter(Light::tint),
                Codec.doubleRange(-1, 1).optionalFieldOf("shift", 0.0).forGetter(Light::shift),
                Codec.doubleRange(0, 1).optionalFieldOf("lens", 0.0).forGetter(Light::lens),
                Codec.doubleRange(0, 1).optionalFieldOf("mirage", 0.0).forGetter(Light::mirage),
                Codec.doubleRange(0, 1).optionalFieldOf("dark", 0.0).forGetter(Light::dark),
                Codec.doubleRange(0, 1).optionalFieldOf("glow", 0.0).forGetter(Light::glow))
                .apply(instance, Light::new));
    }

    /**
     * How space joins up at an area's edge.
     *
     * @param kind      what crossing the edge does
     * @param dimension for one that leads elsewhere, the dimension it leads to; its own when none is said
     * @param target    for one that leads elsewhere, the box it leads to: a crossing at a place of the area comes out
     *                  at the same place of the box, scaled to its size, so a small box may lead to a large one
     * @param seamless  for one that leads elsewhere in its own dimension, whether the far side shows through its faces,
     *                  a portal with no edge
     */
    public record Space(Crossing kind, Optional<ResourceKey<Level>> dimension, Optional<Box> target,
                        boolean seamless) {

        public static final Codec<Space> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(Crossing.class).fieldOf("kind").forGetter(Space::kind),
                ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(Space::dimension),
                BOX.optionalFieldOf("target").forGetter(Space::target),
                Codec.BOOL.optionalFieldOf("seamless", false).forGetter(Space::seamless))
                .apply(instance, Space::new));
    }

    /** What crossing an area's edge does. */
    public enum Crossing implements IStableName {
        /** Leaving through one face comes back in through the opposite one: a space that wraps round. */
        WRAP("wrap"),
        /** Coming in leads to the same place of the target box: a shortcut, or a door bigger inside. */
        ENTER("enter"),
        /** Going out leads to just outside the target box: the way back out of a space bigger inside. */
        LEAVE("leave");

        private final String serializedName;

        Crossing(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * How matter behaves.
     *
     * @param fluidsRise     whether liquids rise instead of falling
     * @param explosionSpeed how fast an explosion goes out, in blocks a second; 0 for at once, as usual
     */
    public record Matter(boolean fluidsRise, double explosionSpeed) {

        public static final Codec<Matter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("fluids_rise", false).forGetter(Matter::fluidsRise),
                Codec.doubleRange(0, 1_000).optionalFieldOf("explosion_speed", 0.0)
                        .forGetter(Matter::explosionSpeed))
                .apply(instance, Matter::new));
    }
}
