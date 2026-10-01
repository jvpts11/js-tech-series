/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * One step a saved thing takes from one version of its layout to the next: a value renamed, moved, split, written in
 * another unit, or a whole old file read into the shape of today. Something several versions behind takes every step
 * from its version on, in order, before it is read, so what a world holds survives the layout changing under it.
 *
 * <p>A step is given what was saved as it was saved, in the version before the step, and gives back the same thing in
 * the version after it. It may change what it is given and give that back.
 */
@FunctionalInterface
public interface ISaveUpgrade {

    /** What was saved, in the version before this step, rewritten into the version after it. */
    Tag upgrade(Tag before);

    /**
     * A step over a compound: a block entity's tag, or a value saved as one. What is not a compound is given back
     * untouched, since a step written for a compound has nothing to say about anything else.
     */
    static ISaveUpgrade compound(final UnaryOperator<CompoundTag> step) {
        Objects.requireNonNull(step, "step");
        return before -> before instanceof CompoundTag tag ? step.apply(tag) : before;
    }

    /** The entry under {@code from}, if there is one, moved to {@code to}. */
    static ISaveUpgrade rename(final String from, final String to) {
        return compound(tag -> {
            final Tag value = tag.get(from);
            if (value != null) {
                tag.remove(from);
                tag.put(to, value);
            }
            return tag;
        });
    }

    /** The entry under {@code key} taken out, for something that is saved no more. */
    static ISaveUpgrade remove(final String key) {
        return compound(tag -> {
            tag.remove(key);
            return tag;
        });
    }
}
