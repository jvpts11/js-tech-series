/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.AudioOutput;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.StereoSide;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The recordings playing in the world: a song on a computer, say, heard out of its monitor and its speakers. Each is
 * kept here by a key its owner chooses, with where it is heard from and the point it has reached, and the server
 * keeps its listeners: a player who comes within hearing is sent it from where it has got to, one who walks away is
 * told to stop, and one who was there all along hears it end when it ends. A song paused keeps its point and is heard
 * again from there.
 *
 * <p>The point is kept in the world's ticks, so a server running slow slows its songs with it; a client plays at its
 * own pace, which only matters to a player who arrives in the middle of one.
 */
public final class MediaSessions {

    /** How often the listeners are looked at, in ticks. */
    private static final int LOOK_EVERY = 10;
    /** How far past a sound's range a listener is let go, so one at the edge is not started and stopped in turn. */
    private static final double LET_GO_MARGIN = 8.0;
    private static final long MILLIS_PER_TICK = 50L;

    private static final Map<ResourceKey<Level>, Map<String, Session>> SESSIONS = new HashMap<>();

    private MediaSessions() {
    }

    /**
     * Plays a recording from a machine's own hardware: out of each of its outputs, as its device plays it, at its own
     * volume times {@code volume}. A device that plays no recordings plays it to nobody, though its point still runs.
     *
     * @param onEnd what to do when it has played to its end, on the server's thread; null for nothing
     */
    public static void play(final ServerLevel level, final String key, final IAudioHost host, final SoundKey sound,
                            final MediaId media, final float volume, final long fromMillis,
                            @Nullable final Runnable onEnd) {
        play(level, key, sound, media, placesOf(host), volume * host.audioVolume(), fromMillis, onEnd);
    }

    /** Plays a recording from those places, from {@code fromMillis} into it; whatever was under the key stops. */
    public static synchronized void play(final ServerLevel level, final String key, final SoundKey sound,
                                         final MediaId media, final List<MediaPlace> places, final float volume,
                                         final long fromMillis, @Nullable final Runnable onEnd) {
        if (!sound.spec().made()) {
            throw new IllegalArgumentException(sound.id() + " plays its own files, not a recording it is handed");
        }
        stop(level, key);
        final long duration = MediaStore.current().map(store -> {
            store.used(media);
            try {
                return store.info(media).millis();
            } catch (final IOException unreadable) {
                return 0L;
            }
        }).orElse(0L);
        final Session session = new Session(key, sound, media, List.copyOf(places), volume, duration, onEnd);
        session.startTick = level.getGameTime() - Math.max(0L, fromMillis) / MILLIS_PER_TICK;
        SESSIONS.computeIfAbsent(level.dimension(), dimension -> new HashMap<>()).put(key, session);
        listen(level, session);
    }

    /** Pauses what plays under the key and answers the point it stopped at, or -1 when nothing plays there. */
    public static synchronized long pause(final ServerLevel level, final String key) {
        final Session session = find(level, key);
        if (session == null) {
            return -1L;
        }
        if (session.pausedAt < 0) {
            session.pausedAt = session.position(level.getGameTime());
            quiet(level, session);
        }
        return session.pausedAt;
    }

    /** Takes a paused recording up again from where it stopped. */
    public static synchronized void resume(final ServerLevel level, final String key) {
        final Session session = find(level, key);
        if (session == null || session.pausedAt < 0) {
            return;
        }
        session.startTick = level.getGameTime() - session.pausedAt / MILLIS_PER_TICK;
        session.pausedAt = -1L;
        listen(level, session);
    }

    /** Stops what plays under the key, for everybody hearing it; its end is not run. */
    public static synchronized void stop(final ServerLevel level, final String key) {
        final Map<String, Session> here = SESSIONS.get(level.dimension());
        final Session session = here == null ? null : here.remove(key);
        if (session != null) {
            quiet(level, session);
        }
    }

    /** Where a machine's recording now comes from and how loud, after its outputs or its volume changed. */
    public static void moved(final ServerLevel level, final String key, final IAudioHost host, final float volume) {
        replace(level, key, placesOf(host), volume * host.audioVolume());
    }

    /** The same, from places given. The listeners hear it again from where it has got to. */
    public static synchronized void replace(final ServerLevel level, final String key, final List<MediaPlace> places,
                                            final float volume) {
        final Session session = find(level, key);
        if (session == null) {
            return;
        }
        quiet(level, session);
        session.places = List.copyOf(places);
        session.volume = volume;
        if (session.pausedAt < 0) {
            listen(level, session);
        }
    }

    /** How far into its recording what plays under the key has got, or -1 when nothing plays there. */
    public static synchronized long position(final ServerLevel level, final String key) {
        final Session session = find(level, key);
        return session == null ? -1L : session.pausedAt >= 0 ? session.pausedAt
                : session.position(level.getGameTime());
    }

    /** Whether something plays under the key, paused or not. */
    public static synchronized boolean has(final ServerLevel level, final String key) {
        return find(level, key) != null;
    }

    /** Whether what plays under the key is paused. */
    public static synchronized boolean paused(final ServerLevel level, final String key) {
        final Session session = find(level, key);
        return session != null && session.pausedAt >= 0;
    }

    /** The players hearing what plays under the key, for a test. */
    public static synchronized Set<UUID> listeners(final ServerLevel level, final String key) {
        final Session session = find(level, key);
        return session == null ? Set.of() : Set.copyOf(session.listeners);
    }

    /** Ends the recordings that have played out, and brings listeners in and lets them go. */
    public static void tick(final MinecraftServer server) {
        if (server.getTickCount() % LOOK_EVERY != 0) {
            return;
        }
        final List<Runnable> ended = new ArrayList<>();
        synchronized (MediaSessions.class) {
            for (final ServerLevel level : server.getAllLevels()) {
                final Map<String, Session> here = SESSIONS.get(level.dimension());
                if (here == null || here.isEmpty()) {
                    continue;
                }
                here.values().removeIf(session -> {
                    // A recording that cannot be read has no length to wait for, so it ends at the next look: an end
                    // that never came would stall whatever chains the next song from it.
                    if (session.pausedAt < 0 && (session.duration <= 0
                            || session.position(level.getGameTime()) >= session.duration)) {
                        quiet(level, session);
                        if (session.onEnd != null) {
                            ended.add(session.onEnd);
                        }
                        return true;
                    }
                    if (session.pausedAt < 0) {
                        listen(level, session);
                    }
                    return false;
                });
            }
        }
        // Run outside the lock: an end usually starts the next song, which comes back in here.
        ended.forEach(Runnable::run);
    }

    /** The player left: they hear nothing any more, and are sent everything again should they come back. */
    public static synchronized void forget(final UUID player) {
        for (final Map<String, Session> here : SESSIONS.values()) {
            for (final Session session : here.values()) {
                session.listeners.remove(player);
            }
        }
    }

    /** Everything stops, as the server does. */
    public static synchronized void clear() {
        SESSIONS.clear();
    }

    /** The recordings playing right now, paused ones included, in every world. */
    public static synchronized Set<MediaId> playingMedia() {
        final Set<MediaId> playing = new HashSet<>();
        for (final Map<String, Session> here : SESSIONS.values()) {
            for (final Session session : here.values()) {
                playing.add(session.media);
            }
        }
        return playing;
    }

    /** The places a machine's recording is heard from: its outputs, as its device plays them. */
    public static List<MediaPlace> placesOf(final IAudioHost host) {
        final AudioDevice device = host.audioDevice();
        if (!device.samples() || host.audioVolume() <= 0.0F) {
            return List.of();
        }
        final List<MediaPlace> places = new ArrayList<>();
        for (final AudioOutput output : host.outputs()) {
            final Vec3 at = output.position();
            // A mono device has no sides to give, and what it keeps of a recording reaches every output.
            final StereoSide side = device.stereo() ? output.side() : StereoSide.BOTH;
            places.add(new MediaPlace(at.x, at.y, at.z, side, device.response().through(output.response())));
        }
        return places;
    }

    @Nullable
    private static Session find(final ServerLevel level, final String key) {
        final Map<String, Session> here = SESSIONS.get(level.dimension());
        return here == null ? null : here.get(key);
    }

    /* Sends it to every player newly in hearing, and stops it for every one who went out of hearing. */
    private static void listen(final ServerLevel level, final Session session) {
        final double range = session.sound.spec().range();
        final long now = session.position(level.getGameTime());
        final Set<UUID> hearing = new HashSet<>();
        for (final ServerPlayer player : level.players()) {
            final boolean listening = session.listeners.contains(player.getUUID());
            final double reach = listening ? range + LET_GO_MARGIN : range;
            if (session.within(player, reach)) {
                hearing.add(player.getUUID());
                if (!listening) {
                    PacketDistributor.sendToPlayer(player, new MediaPlayPayload(session.key, session.sound.id(),
                            session.media, session.places, session.volume, now));
                }
            }
        }
        for (final UUID gone : session.listeners) {
            if (!hearing.contains(gone)) {
                final ServerPlayer player = level.getServer().getPlayerList().getPlayer(gone);
                if (player != null) {
                    PacketDistributor.sendToPlayer(player, new MediaStopPayload(session.key));
                }
            }
        }
        session.listeners.clear();
        session.listeners.addAll(hearing);
    }

    /* Stops it for everybody hearing it. */
    private static void quiet(final ServerLevel level, final Session session) {
        for (final UUID listener : session.listeners) {
            final ServerPlayer player = level.getServer().getPlayerList().getPlayer(listener);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, new MediaStopPayload(session.key));
            }
        }
        session.listeners.clear();
    }

    /** One recording playing in one world. */
    private static final class Session {

        private final String key;
        private final SoundKey sound;
        private final MediaId media;
        private final long duration;
        @Nullable
        private final Runnable onEnd;
        private final Set<UUID> listeners = new HashSet<>();
        private List<MediaPlace> places;
        private float volume;
        /** The tick its point was nought at. */
        private long startTick;
        /** The point it was paused at, or -1 while it plays. */
        private long pausedAt = -1L;

        Session(final String key, final SoundKey sound, final MediaId media, final List<MediaPlace> places,
                final float volume, final long duration, @Nullable final Runnable onEnd) {
            this.key = key;
            this.sound = sound;
            this.media = media;
            this.places = places;
            this.volume = volume;
            this.duration = duration;
            this.onEnd = onEnd;
        }

        long position(final long gameTime) {
            return Math.max(0L, (gameTime - startTick) * MILLIS_PER_TICK);
        }

        boolean within(final ServerPlayer player, final double reach) {
            final double reachSquared = reach * reach;
            for (final MediaPlace place : places) {
                if (place.distanceSquared(player.getX(), player.getY(), player.getZ()) <= reachSquared) {
                    return true;
                }
            }
            return false;
        }
    }
}
