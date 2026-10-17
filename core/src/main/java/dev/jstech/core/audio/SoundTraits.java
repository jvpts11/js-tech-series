/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * How a declared sound of the world behaves on its way to the listener, beyond what it is: whether it matters enough
 * to be shown on screen with where it comes from, for a player who asked for signs, and the sound it becomes heard
 * from far away, a launch's roar turned to a low rumble.
 */
public final class SoundTraits {

    private static final Map<ResourceLocation, Traits> DECLARED = new ConcurrentHashMap<>();

    private SoundTraits() {
    }

    /** How the sound {@code id} behaves; nothing out of the ordinary for a sound that said nothing. */
    public static Traits of(final ResourceLocation id) {
        return DECLARED.getOrDefault(id, Traits.NONE);
    }

    /** Keeps how {@code id} behaves, as its declaration says; the content kit calls it as the sound registers. */
    public static void declare(final ResourceLocation id, final Traits traits) {
        DECLARED.put(id, traits);
    }

    /**
     * How one sound behaves.
     *
     * @param matters     whether it is shown on screen, with where it comes from, to a player who asked for signs
     * @param distant     the sound it plays instead when heard from far away, if any
     * @param distantFrom from how many blocks away it plays that sound instead
     */
    public record Traits(boolean matters, Optional<ResourceLocation> distant, int distantFrom) {

        /** A sound that behaves as any other. */
        public static final Traits NONE = new Traits(false, Optional.empty(), 0);

        /** The sound heard {@code blocks} away: the distant one past its distance, this one nearer. */
        public Optional<ResourceLocation> heardAt(final double blocks) {
            return blocks >= this.distantFrom ? this.distant : Optional.empty();
        }
    }
}
