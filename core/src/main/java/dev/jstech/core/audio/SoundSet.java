/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A sound picked by context: rules in order, the first whose conditions all hold giving the sound, so the most
 * particular come first and a rule with no condition, last, is the fallback. A set with no rule that holds plays
 * nothing, which is how a cue stays silent where its sound would make no sense.
 *
 * @param rules the rules, in the order they are tried
 */
public record SoundSet(List<Rule> rules) {

    /** A set that plays nothing whatever the context. */
    public static final SoundSet SILENT = new SoundSet(List.of());

    public SoundSet {
        rules = List.copyOf(rules);
    }

    /** A set that always plays that sound, by its id. */
    public static SoundSet always(final String sound) {
        return new SoundSet(List.of(new Rule(Map.of(), sound)));
    }

    /** The id of the sound for that context, or null when no rule holds. */
    @Nullable
    public String pick(final SoundContext context) {
        for (final Rule rule : rules) {
            if (rule.matches(context)) {
                return rule.sound();
            }
        }
        return null;
    }

    /**
     * One choice: a sound, and what the context must say for it to be the one.
     *
     * @param when  the value each named dimension must have; none means it always holds
     * @param sound the sound's id ({@code namespace:path}): one the series declared, or any other the game knows
     */
    public record Rule(Map<String, String> when, String sound) {

        public Rule {
            when = Map.copyOf(when);
            if (sound.isEmpty()) {
                throw new IllegalArgumentException("a rule names the sound it plays");
            }
        }

        /** Whether the context says what this rule needs, dimension by dimension. */
        public boolean matches(final SoundContext context) {
            for (final Map.Entry<String, String> needed : when.entrySet()) {
                if (!needed.getValue().equals(context.get(needed.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
