/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.acoustics.ListeningPlace;
import dev.jstech.core.audio.acoustics.SoundConditions;
import dev.jstech.core.audio.acoustics.Soundscape;
import dev.jstech.core.stepaside.StepAside;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * The ambience of the place the listener is in, as resource packs write it in {@code
 * assets/<namespace>/soundscapes/<path>.json}: a {@code sound}, its {@code volume} and {@code when} it is heard
 * (see {@link SoundConditionsJson}). Once a second each soundscape whose conditions hold plays all round the
 * listener, fading in, and each that no longer holds fades out, in the Ambience channel.
 */
final class Soundscapes extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Soundscapes INSTANCE = new Soundscapes();
    private static final String CHANNEL = AudioChannels.AMBIENCE.id().toString();
    private static final Map<String, SurroundingSound> PLAYING = new HashMap<>();

    private static volatile List<Soundscape> loaded = List.of();

    private Soundscapes() {
        super(new Gson(), "soundscapes");
    }

    /** The listener the game reads the packs' files with. */
    static Soundscapes listener() {
        return INSTANCE;
    }

    /** A second at {@code eye}: the soundscapes that hold play, the rest fade out. */
    static void second(final Level level, final Vec3 eye) {
        // While a mod of ambience plays the places' sounds, the Core's fade out and none start.
        final List<Soundscape> all = StepAside.AMBIENCE.steps() ? List.of() : loaded;
        if (all.isEmpty() && PLAYING.isEmpty()) {
            return;
        }
        final List<SoundConditions.Near> asked = new ArrayList<>();
        all.forEach(one -> asked.addAll(one.conditions().near()));
        final ListeningPlace place = ListeningPlaces.at(level, eye, asked);
        final Set<String> wanted = new HashSet<>();
        for (final Soundscape one : all) {
            if (!one.conditions().matches(place)) {
                continue;
            }
            wanted.add(one.id());
            final SurroundingSound playing = PLAYING.get(one.id());
            if (playing == null || playing.isStopped()) {
                final SurroundingSound sound = new SurroundingSound(ResourceLocation.parse(one.sound()),
                        SoundSource.AMBIENT, CHANNEL, one.volume());
                PLAYING.put(one.id(), sound);
                Minecraft.getInstance().getSoundManager().play(sound);
            } else {
                playing.want(one.volume());
            }
        }
        PLAYING.entrySet().removeIf(entry -> {
            if (!wanted.contains(entry.getKey())) {
                entry.getValue().want(0);
            }
            return entry.getValue().isStopped();
        });
    }

    /** The soundscapes heard now, by id, for a test to look at. */
    static Set<String> playing() {
        final Set<String> out = new HashSet<>();
        PLAYING.forEach((id, sound) -> {
            if (!sound.leaving() && !sound.isStopped()) {
                out.add(id);
            }
        });
        return out;
    }

    /** Stops every soundscape at once, as the player leaves a world. */
    static void stop() {
        PLAYING.values().forEach(sound -> Minecraft.getInstance().getSoundManager().stop(sound));
        PLAYING.clear();
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resources,
                         final ProfilerFiller profiler) {
        final List<Soundscape> read = new ArrayList<>();
        files.keySet().stream().sorted().forEach(file -> {
            try {
                final JsonObject json = files.get(file).getAsJsonObject();
                read.add(new Soundscape(file.toString(), ResourceLocation.parse(json.get("sound").getAsString())
                        .toString(), json.has("volume") ? json.get("volume").getAsFloat() : 1F,
                        SoundConditionsJson.read(json.has("when") ? json.getAsJsonObject("when") : null)));
            } catch (final RuntimeException wrong) {
                LOGGER.warn("The soundscape {} could not be read and is left out: {}", file, wrong.getMessage());
            }
        });
        loaded = List.copyOf(read);
    }
}
