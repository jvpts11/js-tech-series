/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * What a blast does to the world, as the server sets it for each mod's explosions: everything a real one does, all but
 * breaking blocks, or nothing but its show.
 */
public enum BlastConsequence implements StringRepresentable {

    /** It breaks blocks, hurts and throws what it reaches, sets fires and leaves its crater. */
    FULL,
    /** It hurts and throws what it reaches, and breaks, burns and marks no block. */
    SPARE_BLOCKS,
    /** It only flashes, sounds and smokes. */
    HARMLESS;

    /** Each one as the settings write it: {@code full}, {@code spare_blocks}, {@code harmless}. */
    public static final Codec<BlastConsequence> CODEC = StringRepresentable.fromEnum(BlastConsequence::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Whether it breaks blocks. */
    public boolean breaksBlocks() {
        return this == FULL;
    }

    /** Whether it hurts and throws what it reaches. */
    public boolean hurts() {
        return this != HARMLESS;
    }

    /** The milder of this and {@code other}. */
    public BlastConsequence atMost(final BlastConsequence other) {
        if (this == HARMLESS || other == HARMLESS) {
            return HARMLESS;
        }
        return this == SPARE_BLOCKS || other == SPARE_BLOCKS ? SPARE_BLOCKS : FULL;
    }
}
