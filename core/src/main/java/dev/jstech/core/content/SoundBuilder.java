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
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundSpace;
import dev.jstech.core.audio.SoundSpec;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one sound, said once: where it is heard from, whether it runs on, its channel, its files, how far
 * it carries and what its subtitle says. {@link #register()} registers its event and keeps the declaration, from
 * which the generator writes its entry in {@code sounds.json} and its subtitle in the English.
 *
 * <p>Its files are {@code <namespace>:<path>} for one, and {@code <path>_1} to {@code <path>_n} for several variants,
 * under the mod's {@code sounds} folder; {@link #file} points at a file that already exists instead, the game's own
 * included, so a sound the game already has is not shipped twice.
 */
public final class SoundBuilder {

    private final ModContent content;
    private final String path;
    private SoundSpace space = SoundSpace.WORLD;
    private boolean loop;
    private AudioChannel channel = AudioChannels.MACHINES;
    private int variants = 1;
    private boolean stream;
    private int range = DEFAULT_RANGE;
    private int priority = SoundSpec.NORMAL;
    private boolean made;
    private boolean stereo;
    private final List<ResourceLocation> files = new ArrayList<>();
    private @Nullable String english;

    /** How far a sound of the world carries when its declaration does not say: the game's own default. */
    private static final int DEFAULT_RANGE = 16;

    SoundBuilder(final ModContent content, final String path) {
        this.content = content;
        this.path = path;
    }

    /** A sound of the world, from a place: the default. */
    public SoundBuilder world() {
        this.space = SoundSpace.WORLD;
        return this;
    }

    /** A sound of the player's own screen, which sits nowhere in the world. */
    public SoundBuilder onScreen() {
        this.space = SoundSpace.INTERFACE;
        this.channel = AudioChannels.INTERFACE;
        return this;
    }

    /** A sound that runs on until it is stopped, as a fan does. */
    public SoundBuilder loop() {
        this.loop = true;
        return this;
    }

    /** The channel it is mixed in; machines by default, the interface for a sound of the screen. */
    public SoundBuilder channel(final AudioChannel in) {
        this.channel = in;
        return this;
    }

    /** Plays one of that many files, picked at random, so a sound heard often does not repeat itself. */
    public SoundBuilder variants(final int count) {
        if (count < 1) {
            throw new IllegalArgumentException("a sound has at least one variant: " + count);
        }
        this.variants = count;
        return this;
    }

    /** Reads its file as it plays, rather than loading it whole: for a long sound or a long loop. */
    public SoundBuilder stream() {
        this.stream = true;
        return this;
    }

    /** How many blocks it carries before it fades out. */
    public SoundBuilder range(final int blocks) {
        this.range = blocks;
        return this;
    }

    /** How much it matters beside other sounds when there are too many to play; {@link SoundSpec#NORMAL} by default. */
    public SoundBuilder priority(final int value) {
        this.priority = value;
        return this;
    }

    /**
     * Has no file: what it plays is made as it plays, handed over each time (a synthesised tune, a recording from a
     * disk). It is declared all the same, for its subtitle and its channel and so a player can turn it off.
     */
    public SoundBuilder made() {
        this.made = true;
        return this;
    }

    /**
     * Its files are stereo recordings. Heard from a place in the world, one is mixed down to one channel as it
     * plays, so the world can place it; the speakers of a pair can each play one side of it.
     */
    public SoundBuilder stereo() {
        this.stereo = true;
        return this;
    }

    /** Plays that file, which already exists, instead of one of its own; call it again for more than one. */
    public SoundBuilder file(final ResourceLocation existing) {
        this.files.add(existing);
        return this;
    }

    /** What its subtitle says in English while it plays. */
    public SoundBuilder subtitle(final String text) {
        this.english = text;
        return this;
    }

    /**
     * Registers the sound's event, unless it is made as it plays and so has none, and keeps what was declared.
     *
     * @throws IllegalStateException when the sound was given no subtitle
     */
    public SoundKey register() {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(content.modid(), path);
        if (english == null) {
            throw new IllegalStateException(id + " needs a subtitle");
        }
        final List<ResourceLocation> played = made || !files.isEmpty() ? files : ownFiles(id);
        final SoundSpec spec = new SoundSpec(space, loop, channel, played, stream, range, priority, made, stereo);
        final Supplier<SoundEvent> event = made ? () -> {
            throw new IllegalStateException(id + " is made as it plays and has no event");
        } : content.soundRegister().register(path, () -> SoundEvent.createFixedRangeEvent(id, range));
        final SoundKey key = new SoundKey(id, spec, TextKey.of(SoundKey.subtitleKey(id), english), event);
        content.declare(key);
        return key;
    }

    private List<ResourceLocation> ownFiles(final ResourceLocation id) {
        if (variants == 1) {
            return List.of(id);
        }
        final List<ResourceLocation> out = new ArrayList<>(variants);
        for (int i = 1; i <= variants; i++) {
            out.add(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_" + i));
        }
        return out;
    }
}
