/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import dev.jstech.core.id.IStableName;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * How blocks sound by what they are made of, as resource packs say: what a step on each, a blow, its breaking, its
 * placing and a fall onto it play in place of the game's, and how much of a sound it drinks and lets through. An entry
 * is for one block, by its id, or for a tag of blocks, written {@code #namespace:path}; a block's own entry wins over
 * its tags', and a tag's over a later tag's, field by field, so a pack can give wool a softer step and keep the rest.
 * A later pack's entry for the same block or tag replaces what an earlier one said, field by field too.
 */
public final class MaterialSounds {

    private final Map<String, Material> entries = new LinkedHashMap<>();

    /** Adds what a pack says of {@code key}, a block's id or a tag, over what was said of it before. */
    public void put(final String key, final Material material) {
        this.entries.merge(key, material, (before, after) -> after.over(before));
    }

    /** What {@code block}, which is in {@code tags}, sounds like: its own entry, then its tags', field by field. */
    public Material of(final String block, final Collection<String> tags) {
        Material found = this.entries.getOrDefault(block, Material.NOTHING);
        for (final Map.Entry<String, Material> entry : this.entries.entrySet()) {
            if (entry.getKey().startsWith("#") && tags.contains(entry.getKey())) {
                found = found.over(entry.getValue());
            }
        }
        return found;
    }

    /** Whether any pack said anything. */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** Forgets everything, before the packs are read again. */
    public void clear() {
        this.entries.clear();
    }

    /** What makes a block sound, by the name a pack's file writes it with. */
    public enum Kind implements IStableName {
        STEP("step"),
        HIT("hit"),
        BREAK("break"),
        PLACE("place"),
        FALL("fall");

        private final String name;

        Kind(final String name) {
            this.name = name;
        }

        @Override
        public String serializedName() {
            return this.name;
        }
    }

    /**
     * What one entry says.
     *
     * @param sounds     the sound each kind plays instead of the game's, by its id
     * @param absorption how much of a sound it drinks, from 0 to 1, when said
     * @param through    how much of a sound gets through one block of it, from 0 to 1, when said
     */
    public record Material(Map<Kind, String> sounds, OptionalDouble absorption, OptionalDouble through) {

        /** An entry that says nothing. */
        public static final Material NOTHING = new Material(Map.of(), OptionalDouble.empty(), OptionalDouble.empty());

        public Material {
            sounds = sounds.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(sounds));
            if (absorption.isPresent() && (absorption.getAsDouble() < 0 || absorption.getAsDouble() > 1)
                    || through.isPresent() && (through.getAsDouble() < 0 || through.getAsDouble() > 1)) {
                throw new IllegalArgumentException("a share from 0 to 1: drinks " + absorption + ", lets through "
                        + through);
            }
        }

        /** The sound {@code kind} plays instead of the game's, when said. */
        public Optional<String> sound(final Kind kind) {
            return Optional.ofNullable(this.sounds.get(kind));
        }

        /** This entry, with what it leaves unsaid taken from {@code under}. */
        public Material over(final Material under) {
            final Map<Kind, String> merged = new EnumMap<>(Kind.class);
            merged.putAll(under.sounds);
            merged.putAll(this.sounds);
            return new Material(merged, this.absorption.isPresent() ? this.absorption : under.absorption,
                    this.through.isPresent() ? this.through : under.through);
        }
    }
}
