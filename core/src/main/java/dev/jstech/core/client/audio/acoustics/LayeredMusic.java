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
import dev.jstech.core.audio.acoustics.MusicConductor;
import dev.jstech.core.audio.acoustics.MusicSet;
import dev.jstech.core.audio.acoustics.SoundConditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.slf4j.Logger;

/**
 * Music that follows the moment, as resource packs write it in {@code assets/<namespace>/music_sets/<path>.json}: a
 * {@code priority}, {@code when} the set plays and its {@code layers}, each a {@code sound}, a {@code volume} and
 * {@code when} it is heard (see {@link SoundConditionsJson}). The layers of the set playing start together and keep
 * in step, each coming in and going out with its own conditions; while a set plays, the game's own music waits.
 */
final class LayeredMusic extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LayeredMusic INSTANCE = new LayeredMusic();
    private static final String CHANNEL = AudioChannels.MUSIC.id().toString();
    /* How loud a layer silent for now still plays, so it keeps in step to come in on the beat: never heard. */
    private static final float HUSHED = 1.0E-4F;
    private static final MusicConductor CONDUCTOR = new MusicConductor();
    private static final Map<String, SurroundingSound> LAYERS = new HashMap<>();

    private static volatile List<MusicSet> loaded = List.of();
    private static volatile String playing = "";
    private static volatile MusicConductor.Plan plan = MusicConductor.Plan.SILENT;

    private LayeredMusic() {
        super(new Gson(), "music_sets");
    }

    /** The listener the game reads the packs' files with. */
    static LayeredMusic listener() {
        return INSTANCE;
    }

    /** Keeps the game's own music waiting while a set plays. */
    static void onSelectMusic(final SelectMusicEvent event) {
        if (!playing.isEmpty()) {
            event.setMusic(null);
        }
    }

    /** A second at {@code eye}: the set to play, and how loud each of its layers is. */
    static void second(final Level level, final Vec3 eye) {
        final List<MusicSet> sets = loaded;
        if (sets.isEmpty() && playing.isEmpty()) {
            return;
        }
        final List<SoundConditions.Near> asked = new ArrayList<>();
        for (final MusicSet set : sets) {
            asked.addAll(set.conditions().near());
            set.layers().forEach(layer -> asked.addAll(layer.conditions().near()));
        }
        final ListeningPlace place = ListeningPlaces.at(level, eye, asked);
        final MusicConductor.Plan now = CONDUCTOR.second(sets, place);
        plan = now;
        if (!now.set().equals(playing)) {
            LAYERS.values().forEach(layer -> layer.want(0));
            LAYERS.clear();
            if (now.plays()) {
                Minecraft.getInstance().getMusicManager().stopPlaying();
                // Every layer starts in the same tick, so they keep in step for as long as the set plays.
                now.volumes().forEach((sound, volume) -> {
                    final SurroundingSound layer = new SurroundingSound(ResourceLocation.parse(sound),
                            SoundSource.MUSIC, CHANNEL, Math.max(HUSHED, volume));
                    LAYERS.put(sound, layer);
                    Minecraft.getInstance().getSoundManager().play(layer);
                });
            }
            playing = now.set();
            return;
        }
        now.volumes().forEach((sound, volume) -> {
            final SurroundingSound layer = LAYERS.get(sound);
            if (layer != null) {
                layer.want(Math.max(HUSHED, volume));
            }
        });
    }

    /** The set playing, or empty for none, for a test to look at. */
    static String set() {
        return playing;
    }

    /** Each layer of the set playing, by sound, and how loud it is asked to play now, for a test to look at. */
    static Map<String, Float> layers() {
        return plan.volumes();
    }

    /** Stops the music at once, as the player leaves a world. */
    static void stop() {
        LAYERS.values().forEach(layer -> Minecraft.getInstance().getSoundManager().stop(layer));
        LAYERS.clear();
        playing = "";
        plan = MusicConductor.Plan.SILENT;
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resources,
                         final ProfilerFiller profiler) {
        final List<MusicSet> read = new ArrayList<>();
        files.keySet().stream().sorted().forEach(file -> {
            try {
                final JsonObject json = files.get(file).getAsJsonObject();
                final List<MusicSet.Layer> layers = new ArrayList<>();
                for (final JsonElement element : json.getAsJsonArray("layers")) {
                    final JsonObject layer = element.getAsJsonObject();
                    layers.add(new MusicSet.Layer(ResourceLocation.parse(layer.get("sound").getAsString()).toString(),
                            layer.has("volume") ? layer.get("volume").getAsFloat() : 1F,
                            SoundConditionsJson.read(layer.has("when") ? layer.getAsJsonObject("when") : null)));
                }
                read.add(new MusicSet(file.toString(), json.has("priority") ? json.get("priority").getAsInt() : 0,
                        SoundConditionsJson.read(json.has("when") ? json.getAsJsonObject("when") : null), layers));
            } catch (final RuntimeException wrong) {
                LOGGER.warn("The music set {} could not be read and is left out: {}", file, wrong.getMessage());
            }
        });
        loaded = List.copyOf(read);
    }
}
