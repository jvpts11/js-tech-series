/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.id.IStableName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * An animation of a model: how long it is, what it does at its end, the channels that move each bone, the morph
 * targets' weights, and the events along it, sounds, particles and words to code, each at its time. Pure.
 *
 * @param name    its name
 * @param length  how long it is, in seconds
 * @param loop    what it does at its end
 * @param bones   the channels of each bone it moves, by the bone's name
 * @param weights the weight channel of each morph target it moves, by the target's name
 * @param events  what happens along it, earliest first
 */
public record ModelAnimation(String name, double length, Loop loop, Map<String, Tracks> bones,
                             Map<String, AnimationChannel> weights, List<Event> events) {

    public ModelAnimation {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(loop, "loop");
        bones = Map.copyOf(bones);
        weights = Map.copyOf(weights);
        final List<Event> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparingDouble(Event::time));
        events = List.copyOf(sorted);
    }

    /**
     * The time into it {@code elapsed} seconds after it started, as its end says: round again for one that loops,
     * its end for one that holds or plays once.
     */
    public double timeAt(final double elapsed) {
        if (this.length <= 0) {
            return 0;
        }
        return this.loop == Loop.LOOP ? elapsed % this.length : Math.min(elapsed, this.length);
    }

    /** Whether, {@code elapsed} seconds after it started, an animation that plays once is over. */
    public boolean over(final double elapsed) {
        return this.loop == Loop.ONCE && elapsed >= this.length;
    }

    /**
     * The channels that move one bone, each none where the animation leaves that of the bone alone.
     *
     * @param rotation its turn
     * @param position its place
     * @param scale    its size
     */
    public record Tracks(@Nullable AnimationChannel rotation, @Nullable AnimationChannel position,
                         @Nullable AnimationChannel scale) {
    }

    /**
     * Something that happens at a time of an animation: a sound played, a particle effect shown, or a word handed to
     * the code that drives the model.
     *
     * @param time    when, in seconds
     * @param kind    what it is
     * @param value   the sound's or the effect's name, or the word
     * @param locator the locator it happens at, or empty for the model's middle
     */
    public record Event(double time, EventKind kind, String value, String locator) {

        public Event {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(locator, "locator");
        }
    }

    /** What happens at an event. */
    public enum EventKind {
        SOUND,
        PARTICLE,
        INSTRUCTION
    }

    /** What an animation does at its end. */
    public enum Loop implements IStableName {
        /** It stops, and the bones go back to where they were. */
        ONCE("once"),
        /** It starts over. */
        LOOP("loop"),
        /** It stays on its last frame. */
        HOLD("hold_on_last_frame");

        private final String serializedName;

        Loop(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
