/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AmbientClusters;
import dev.jstech.core.audio.AmbientField;
import dev.jstech.core.audio.AmbientFields;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.Occlusion;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.VoiceBudget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Keeps the world's running sounds: every source in the world says what it wants heard, and the director starts,
 * keeps, retunes, fades and stops the sounds to match, a few times a second and never on the server.
 *
 * <p>Along the way it makes a room of many machines one bed of their field, keeps inside a budget of the game's
 * channels (the sounds that matter most and are nearest first), muffles what is behind walls, and leaves out what the
 * player turned off. A source is known by itself, so the same sound of two machines is two sounds.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class SoundDirector {

    /** How many ticks apart the director looks again: often enough to follow a machine, and cheap. */
    private static final int EVERY_TICKS = 4;
    /** How many ticks apart the walls are looked at again, which is the dearest part of looking. */
    private static final int WALLS_EVERY_TICKS = 20;
    /** A little past a sound's own range, so one at its edge fades out rather than stopping as the player steps. */
    private static final double RANGE_SLACK = 4.0;
    /** The share of the game's channels the series' running sounds keep to: many short ones, few streamed. */
    private static final VoiceBudget BUDGET = new VoiceBudget(24, 2);

    private static final Set<IAudible> SOURCES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<String, LoopSoundInstance> PLAYING = new HashMap<>();
    /** The rooms made at the last look, for the debug screen. */
    private static final List<AudioStats.Room> ROOMS = new ArrayList<>();
    private static long ticks;

    private SoundDirector() {
    }

    /** Starts listening to a source; a block entity calls it when it loads on the client. */
    public static synchronized void track(final IAudible source) {
        SOURCES.add(source);
    }

    /** Stops listening to a source, whose sounds fade out; a block entity calls it when it is removed. */
    public static synchronized void untrack(final IAudible source) {
        SOURCES.remove(source);
    }

    /** What the director keeps now, for the game's debug screen: its budget, its rooms and what is muffled. */
    public static synchronized AudioStats stats() {
        int shorts = 0;
        int longs = 0;
        final List<Integer> walls = new ArrayList<>();
        for (final LoopSoundInstance sound : PLAYING.values()) {
            if (sound.leaving()) {
                continue;
            }
            if (sound.sound().spec().stream()) {
                longs++;
            } else {
                shorts++;
            }
            if (sound.muffled() < 1.0F) {
                walls.add(Occlusion.walls(sound.muffled()));
            }
        }
        walls.sort(null);
        return new AudioStats(shorts, BUDGET.maxStatic(), longs, BUDGET.maxStreaming(), List.copyOf(ROOMS), walls);
    }

    /** The running sounds the director is keeping, by what it knows each one as, for a test to look at. */
    public static synchronized Set<String> playing() {
        final Set<String> out = new HashSet<>();
        PLAYING.forEach((id, sound) -> {
            if (!sound.leaving()) {
                out.add(id);
            }
        });
        return out;
    }

    /** Looks at every source now rather than at the next turn, for a test that cannot wait. */
    public static synchronized void updateNow() {
        update(true);
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        ticks++;
        if (ticks % EVERY_TICKS == 0) {
            synchronized (SoundDirector.class) {
                update(ticks % WALLS_EVERY_TICKS == 0);
            }
        }
    }

    /* A player leaving a world takes nothing of it into the next. */
    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        synchronized (SoundDirector.class) {
            PLAYING.values().forEach(LoopSoundInstance::leave);
            PLAYING.clear();
            SOURCES.clear();
            ROOMS.clear();
        }
    }

    private static void update(final boolean lookAtWalls) {
        final Minecraft mc = Minecraft.getInstance();
        ROOMS.clear();
        if (mc.level == null || mc.player == null) {
            PLAYING.values().forEach(LoopSoundInstance::leave);
            PLAYING.clear();
            return;
        }
        final Vec3 listener = mc.gameRenderer.getMainCamera().getPosition();
        final Map<String, Wanted> wanted = gather(listener);
        final List<VoiceBudget.Candidate> candidates = new ArrayList<>(wanted.size());
        wanted.forEach((id, one) -> candidates.add(new VoiceBudget.Candidate(id, one.sound.spec().priority(),
                one.distance, one.sound.spec().stream())));
        final Set<String> chosen = new HashSet<>();
        for (final VoiceBudget.Candidate candidate : BUDGET.choose(candidates)) {
            chosen.add(candidate.id());
        }
        PLAYING.entrySet().removeIf(entry -> {
            final boolean gone = !chosen.contains(entry.getKey()) || entry.getValue().finished();
            if (gone) {
                entry.getValue().leave();
            }
            return gone;
        });
        final boolean muffle = AudioPrefsStore.prefs().occlusion();
        for (final String id : chosen) {
            final Wanted one = wanted.get(id);
            LoopSoundInstance sound = PLAYING.get(id);
            final boolean fresh = sound == null;
            if (fresh) {
                sound = new LoopSoundInstance(one.sound, one.x, one.y, one.z, one.volume, one.pitch);
                PLAYING.put(id, sound);
            } else {
                sound.retarget(one.x, one.y, one.z, one.volume, one.pitch);
            }
            if (lookAtWalls || fresh) {
                sound.muffle(muffle ? OcclusionProbe.through(mc.level, listener, new Vec3(one.x, one.y, one.z)) : 1F);
            }
            if (fresh) {
                AudioEngine.play(sound);
            }
        }
    }

    /* What every source wants heard and is near enough to be, with the rooms many of them make already made. */
    private static Map<String, Wanted> gather(final Vec3 listener) {
        final Map<String, Wanted> wanted = new HashMap<>();
        final List<AmbientClusters.Source> fielded = new ArrayList<>();
        for (final IAudible source : SOURCES) {
            final String who = Integer.toHexString(System.identityHashCode(source));
            for (final LoopRequest request : source.loops()) {
                final String id = who + "|" + request.sound().id();
                if (AudioPrefsStore.prefs().isMuted(request.sound().id().toString())) {
                    continue;
                }
                final Wanted one = new Wanted(request.sound(), source.audioX(), source.audioY(), source.audioZ(),
                        request.volume(), request.pitch(), listener);
                if (one.distance > request.sound().spec().range() + RANGE_SLACK) {
                    continue;
                }
                wanted.put(id, one);
                if (request.field() != null) {
                    fielded.add(new AmbientClusters.Source(id, request.field().toString(), one.x, one.y, one.z));
                }
            }
        }
        if (fielded.isEmpty()) {
            return wanted;
        }
        final Map<String, AmbientClusters.Rule> rules = new HashMap<>();
        final Map<String, AmbientField> fields = new HashMap<>();
        for (final AmbientField field : AmbientFields.all()) {
            rules.put(field.id().toString(), new AmbientClusters.Rule(field.threshold(), field.radius()));
            fields.put(field.id().toString(), field);
        }
        final AmbientClusters.Result rooms = AmbientClusters.group(fielded, rules);
        rooms.absorbed().forEach(wanted::remove);
        for (final AmbientClusters.Bed bed : rooms.beds()) {
            final SoundKey sound = fields.get(bed.field()).bed();
            ROOMS.add(new AudioStats.Room(bed.field(), bed.members().size(), bed.volume()));
            if (!AudioPrefsStore.prefs().isMuted(sound.id().toString())) {
                wanted.put("bed|" + bed.field() + "|" + bed.members().getFirst(),
                        new Wanted(sound, bed.x(), bed.y(), bed.z(), (float) bed.volume(), 1.0F, listener));
            }
        }
        return wanted;
    }

    /** One sound wanted now, where from, how loud and how high, and how far from the listener. */
    private static final class Wanted {

        private final SoundKey sound;
        private final double x;
        private final double y;
        private final double z;
        private final float volume;
        private final float pitch;
        private final double distance;

        Wanted(final SoundKey sound, final double x, final double y, final double z, final float volume,
               final float pitch, final Vec3 listener) {
            this.sound = sound;
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = volume;
            this.pitch = pitch;
            this.distance = listener.distanceTo(new Vec3(x, y, z));
        }
    }
}
