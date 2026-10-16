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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;

/**
 * An effect, as its file says: its emitters, how far from it a player still sees it, and where on a block it plays
 * when it is played at one. The file is {@code assets/<namespace>/effects/<path>.json}, the effect
 * {@code <namespace>:<path>}; a resource pack writes over it.
 *
 * <pre>{@code
 * { "anchor": {"face": "up"}, "emitters": [ { "name": "steam",
 *     "emission": {"shape": {"type": "disc", "radius": 0.4}, "rate": 40, "life": [3, 5], "speed": 1.2},
 *     "look": {"sprite": "minecraft:big_smoke", "frames": 12, "size": [0.4, 2],
 *              "colour": ["#F0F4F8E0", "#F0F4F800"]},
 *     "motion": {"buoyancy": 1.5, "drag": 1.2, "expands": true} } ] }
 * }</pre>
 *
 * @param emitters its emitters
 * @param reach    how far a player still sees it, in blocks
 * @param anchor   where on a block it plays, if it says
 */
public record EffectDefinition(List<Emitter> emitters, int reach, Optional<Anchor> anchor) {

    private static final int DEFAULT_REACH = 64;
    private static final int MOST_REACH = 512;
    /** An effect of nothing. */
    public static final EffectDefinition NOTHING = new EffectDefinition(List.of(), DEFAULT_REACH, Optional.empty());
    public static final Codec<EffectDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Emitter.CODEC.listOf().optionalFieldOf("emitters", List.of()).forGetter(EffectDefinition::emitters),
            Codec.intRange(1, MOST_REACH).optionalFieldOf("reach", DEFAULT_REACH).forGetter(EffectDefinition::reach),
            Anchor.CODEC.optionalFieldOf("anchor").forGetter(EffectDefinition::anchor))
            .apply(instance, EffectDefinition::new));

    public EffectDefinition {
        emitters = List.copyOf(emitters);
        Objects.requireNonNull(anchor, "anchor");
    }

    /** Its emitter named {@code name}, if it has one. */
    public Optional<Emitter> emitter(final String name) {
        return this.emitters.stream().filter(each -> each.name().equals(name)).findFirst();
    }

    /** It, with its emitter at {@code index} as {@code changed}. */
    public EffectDefinition withEmitter(final int index, final Emitter changed) {
        final List<Emitter> all = new ArrayList<>(this.emitters);
        all.set(index, changed);
        return new EffectDefinition(all, this.reach, this.anchor);
    }

    /** It, playing at {@code changed} on a block. */
    public EffectDefinition withAnchor(final Anchor changed) {
        return new EffectDefinition(this.emitters, this.reach, Optional.of(changed));
    }

    /**
     * Where on a block an effect plays: the middle of one of its faces, facing out of it, or a locator of the model
     * the block is drawn with; nudged by an offset.
     *
     * @param face    the face, if it plays at one
     * @param locator the locator of the block's model, if it plays at one; empty for none
     * @param offset  how far from there, in blocks, x y z
     */
    public record Anchor(Optional<Direction> face, String locator, List<Double> offset) {

        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.optionalFieldOf("face").forGetter(Anchor::face),
                Codec.STRING.optionalFieldOf("locator", "").forGetter(Anchor::locator),
                Codec.DOUBLE.listOf().optionalFieldOf("offset", List.of()).forGetter(Anchor::offset))
                .apply(instance, Anchor::new));

        public Anchor {
            Objects.requireNonNull(face, "face");
            Objects.requireNonNull(locator, "locator");
            offset = List.copyOf(offset);
        }

        /** The middle of {@code direction}'s face. */
        public static Anchor at(final Direction direction) {
            return new Anchor(Optional.of(direction), "", List.of());
        }

        /** The locator {@code name} of the block's model. */
        public static Anchor locator(final String name) {
            return new Anchor(Optional.empty(), name, List.of());
        }

        /** Its offset's {@code axis} (0 x, 1 y, 2 z), 0 where it says none. */
        public double offset(final int axis) {
            return axis < this.offset.size() ? this.offset.get(axis) : 0;
        }
    }
}
