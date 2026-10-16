/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How a settlement grows: its whole layout is laid out as it generates, the size it will reach, and it stands at first
 * as its first {@code built} stages of {@code stages}, the rest built a stage every {@code days} days of the world's
 * time from when it was first seen, from its middle out.
 *
 * <pre>{@code
 * { "stages": 5, "built": 1, "days": 3 }
 * }</pre>
 *
 * @param stages how many stages it grows in
 * @param built  how many of them stand when it generates
 * @param days   how many days of the world's time each further stage takes
 */
public record Growth(int stages, int built, int days) {

    private static final int MOST_STAGES = 64;
    private static final int LONGEST = 3650;
    public static final Codec<Growth> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, MOST_STAGES).fieldOf("stages").forGetter(Growth::stages),
            Codec.intRange(1, MOST_STAGES).optionalFieldOf("built", 1).forGetter(Growth::built),
            Codec.intRange(1, LONGEST).optionalFieldOf("days", 1).forGetter(Growth::days))
            .apply(instance, Growth::new));

    /**
     * The stage, from 1, of the {@code index}th of {@code count} pieces as the layout laid them out, from its middle
     * out: the first in the first stage, the rest shared out evenly over the stages.
     */
    public int stageOf(final int index, final int count) {
        return Math.min(this.stages, 1 + (int) ((long) index * this.stages / Math.max(1, count)));
    }

    /** How many stages stand {@code days} days after it was first seen. */
    public int standing(final long daysSeen) {
        return (int) Math.min(this.stages, this.built + Math.max(0, daysSeen) / this.days);
    }
}
