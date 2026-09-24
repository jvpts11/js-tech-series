/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.audio.AudioChannel;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundSet;
import dev.jstech.core.audio.SoundSpace;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Everything about one cue, said once: where it is heard from, the channel a sound bound to it plays in when the
 * series did not declare that sound, and the rules that pick its sound by context, most particular first. The
 * generator writes the rules to the cue's file, which a resource pack replaces to bind the cue to other sounds.
 *
 * <pre>{@code
 * CONTENT.cue("computer/boot").world()
 *         .when(SoundContext.DEVICE, "jsc:pc_speaker", BOOT_BEEP)
 *         .when(SoundContext.ERA, "vintage", BOOT_VINTAGE)
 *         .otherwise(BOOT)
 *         .register();
 * }</pre>
 */
public final class CueBuilder {

    private final ModContent content;
    private final String path;
    private SoundSpace space = SoundSpace.WORLD;
    private AudioChannel channel = AudioChannels.MACHINES;
    private int range = DEFAULT_RANGE;
    private final List<SoundSet.Rule> rules = new ArrayList<>();

    /** How far a cue of the world carries when its declaration does not say: the game's own default. */
    private static final int DEFAULT_RANGE = 16;

    CueBuilder(final ModContent content, final String path) {
        this.content = content;
        this.path = path;
    }

    /** A cue heard from a place in the world: the default. */
    public CueBuilder world() {
        this.space = SoundSpace.WORLD;
        return this;
    }

    /** A cue heard from the player's own screen. */
    public CueBuilder onScreen() {
        this.space = SoundSpace.INTERFACE;
        this.channel = AudioChannels.INTERFACE;
        return this;
    }

    /** The channel a sound bound to it plays in when the series did not declare that sound; machines by default. */
    public CueBuilder channel(final AudioChannel in) {
        this.channel = in;
        return this;
    }

    /** How many blocks away a player still hears it. */
    public CueBuilder range(final int blocks) {
        this.range = blocks;
        return this;
    }

    /** Plays that sound when the context's dimension has that value, unless a rule before this one holds. */
    public CueBuilder when(final String dimension, final String value, final SoundKey sound) {
        return when(Map.of(dimension, value), sound.id());
    }

    /** Plays that sound, by its id, when the context says all of that, unless a rule before this one holds. */
    public CueBuilder when(final Map<String, String> conditions, final ResourceLocation sound) {
        rules.add(new SoundSet.Rule(conditions, sound.toString()));
        return this;
    }

    /** Plays that sound when no rule before this one holds. */
    public CueBuilder otherwise(final SoundKey sound) {
        return when(Map.of(), sound.id());
    }

    /** Registers the cue with the rules said so far; a cue with none is silent until a pack binds it. */
    public SoundCue register() {
        final SoundCue cue = new SoundCue(ResourceLocation.fromNamespaceAndPath(content.modid(), path), space, channel,
                range, new SoundSet(rules));
        content.declare(cue);
        return cue;
    }
}
