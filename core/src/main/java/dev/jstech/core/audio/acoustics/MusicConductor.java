/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Chooses, once a second, which music set plays and how loud each of its layers is: the set of highest priority
 * whose conditions hold. One started keeps playing until a set of higher priority comes, or until its own conditions
 * have failed for a few seconds in a row, its layers falling silent meanwhile, so the music does not flicker at a
 * boundary the listener walks along.
 */
public final class MusicConductor {

    private @Nullable MusicSet playing;
    private int failingSeconds;

    /** How many seconds in a row a set's conditions must fail before it stops. */
    public static final int LETS_GO_AFTER = 5;

    /** The set playing, if any. */
    public Optional<MusicSet> playing() {
        return Optional.ofNullable(this.playing);
    }

    /** A second at {@code place}, with {@code sets} to choose from: the set to play, and each layer's volume. */
    public Plan second(final List<MusicSet> sets, final ListeningPlace place) {
        final Optional<MusicSet> best = sets.stream().filter(set -> set.conditions().matches(place))
                .max(Comparator.comparingInt(MusicSet::priority));
        final MusicSet now = this.playing;
        if (now == null || best.isPresent() && best.get().priority() > now.priority()) {
            this.playing = best.orElse(null);
            this.failingSeconds = 0;
        } else if (now.conditions().matches(place)) {
            this.failingSeconds = 0;
        } else if (++this.failingSeconds >= LETS_GO_AFTER) {
            this.playing = best.orElse(null);
            this.failingSeconds = 0;
        }
        final MusicSet chosen = this.playing;
        if (chosen == null) {
            return Plan.SILENT;
        }
        final boolean holds = chosen.conditions().matches(place);
        final Map<String, Float> volumes = new LinkedHashMap<>();
        for (final MusicSet.Layer layer : chosen.layers()) {
            volumes.put(layer.sound(), holds && layer.conditions().matches(place) ? layer.volume() : 0F);
        }
        return new Plan(chosen.id(), volumes);
    }

    /**
     * What plays this second.
     *
     * @param set     the set's id, or empty for none
     * @param volumes each of its layers' volume, by sound: 0 for a layer silent now but kept in step
     */
    public record Plan(String set, Map<String, Float> volumes) {

        /** No music of a set: the game's own plays. */
        public static final Plan SILENT = new Plan("", Map.of());

        public Plan {
            volumes = Map.copyOf(volumes);
        }

        /** Whether a set plays. */
        public boolean plays() {
            return !this.set.isEmpty();
        }
    }
}
