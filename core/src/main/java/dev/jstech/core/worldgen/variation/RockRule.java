/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.variation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;

/**
 * Where geology puts one variant of a rock: how the rock lies open, between which depths under the top of the ground,
 * on ground at least how steep, and between which heights. Every part of it must hold. Pure but for its codec.
 *
 * <pre>{@code
 * {"exposure": "side", "min_slope": 2}               // the faces of scarps and the walls of canyons
 * {"exposure": "top", "max_depth": 0}                 // the tops of lava flows
 * {"min_depth": 0, "max_depth": 3}                    // the upper layers of every exposure
 * {"exposure": "buried", "min_depth": 4}              // the rock below ground
 * }</pre>
 *
 * @param exposure how it lies open
 * @param minDepth the least depth under the top of the ground, in blocks
 * @param maxDepth the most
 * @param minSlope the least steepness of the ground round it ({@link RockPlace#slope})
 * @param minY     the lowest height
 * @param maxY     the highest
 */
public record RockRule(Exposure exposure, int minDepth, int maxDepth, int minSlope, int minY, int maxY) {

    /** Anywhere at all. */
    public static final RockRule ANYWHERE = new RockRule(Exposure.ANY, 0, Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
            Integer.MAX_VALUE);
    public static final Codec<RockRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Exposure.CODEC.optionalFieldOf("exposure", Exposure.ANY).forGetter(RockRule::exposure),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("min_depth", 0).forGetter(RockRule::minDepth),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("max_depth", Integer.MAX_VALUE)
                    .forGetter(RockRule::maxDepth),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("min_slope", 0).forGetter(RockRule::minSlope),
            Codec.INT.optionalFieldOf("min_y", Integer.MIN_VALUE).forGetter(RockRule::minY),
            Codec.INT.optionalFieldOf("max_y", Integer.MAX_VALUE).forGetter(RockRule::maxY))
            .apply(instance, RockRule::new));

    /** Whether a block of the rock lying at {@code place} is where this variant goes. */
    public boolean fits(final RockPlace place) {
        return this.exposure.fits(place) && place.depth() >= this.minDepth && place.depth() <= this.maxDepth
                && place.slope() >= this.minSlope && place.y() >= this.minY && place.y() <= this.maxY;
    }

    /** The same, lying open as {@code how}. */
    public RockRule exposed(final Exposure how) {
        return new RockRule(how, this.minDepth, this.maxDepth, this.minSlope, this.minY, this.maxY);
    }

    /** The same, between {@code least} and {@code most} blocks under the top of the ground. */
    public RockRule depth(final int least, final int most) {
        return new RockRule(this.exposure, least, most, this.minSlope, this.minY, this.maxY);
    }

    /** The same, on ground at least {@code least} steep. */
    public RockRule slope(final int least) {
        return new RockRule(this.exposure, this.minDepth, this.maxDepth, least, this.minY, this.maxY);
    }

    /** The same, between the heights {@code lowest} and {@code highest}. */
    public RockRule heights(final int lowest, final int highest) {
        return new RockRule(this.exposure, this.minDepth, this.maxDepth, this.minSlope, lowest, highest);
    }

    /** How a block of rock lies open. */
    public enum Exposure implements IStableName {
        /** However it lies. */
        ANY("any"),
        /** Open to the sky: the top of the ground. */
        TOP("top"),
        /** Open on a side: a face of a scarp, a wall, a cut. */
        SIDE("side"),
        /** Open to nothing: buried in rock. */
        BURIED("buried");

        public static final Codec<Exposure> CODEC = StableCodecs.byName(Exposure.class);

        private final String serializedName;

        Exposure(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** Whether a block lying at {@code place} lies open this way. */
        public boolean fits(final RockPlace place) {
            return switch (this) {
                case ANY -> true;
                case TOP -> place.openAbove();
                case SIDE -> place.openSide();
                case BURIED -> place.buried();
            };
        }
    }
}
