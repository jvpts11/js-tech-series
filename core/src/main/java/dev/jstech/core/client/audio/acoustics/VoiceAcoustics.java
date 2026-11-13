/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.acoustics.SoundMedium;
import dev.jstech.core.client.audio.AudioPrefsStore;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Players' voices, as a voice chat mod plays them, through the same world as every other sound: muffled and dull
 * behind walls, weaker in thin air, and silent through a vacuum unless the speakers touch, so in space players hear
 * each other only by radio. A voice that comes from no place (a radio, a group call) is left as it is.
 *
 * <p>The voice chat calls in from threads of its own. The listener's place and the speakers' eyes are read from what
 * the game noted on its own thread each tick; the walls between are read from the world as it stands, a block read
 * that at worst sees a block a tick old.
 */
public final class VoiceAcoustics {

    /* The rate a voice chat samples at, in hertz. */
    private static final double RATE = 48_000;
    /* The lowest a voice's high notes are cut to behind the thickest walls, and the highest, in hertz. */
    private static final double DULLEST = 500;
    private static final double CLEAREST = 20_000;
    /* Where each voice's filter left off, so one packet runs into the next without a click. */
    private static final Map<UUID, double[]> CARRIED = new ConcurrentHashMap<>();
    /*
     * The voice chat calls in from threads of its own, which must not walk the world's entities while the game changes
     * them: the game notes here, each tick, where the listener is and where each voice heard has its eyes.
     */
    private static final Map<UUID, Vec3> EYES = new ConcurrentHashMap<>();
    private static final Set<UUID> HEARD = ConcurrentHashMap.newKeySet();
    private static volatile @Nullable Vec3 listener;

    private VoiceAcoustics() {
    }

    /** Each tick, on the game's thread: notes where the listener stands and where each voice heard has its eyes. */
    public static void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || minecraft.gameRenderer == null) {
            listener = null;
            EYES.clear();
            return;
        }
        listener = minecraft.gameRenderer.getMainCamera().getPosition();
        for (final UUID speaker : HEARD) {
            final Entity entity = findEntity(level, speaker);
            if (entity == null) {
                EYES.remove(speaker);
            } else {
                EYES.put(speaker, entity.getEyePosition());
            }
        }
    }

    /** Forgets every voice heard, as the player leaves a world. */
    public static void reset() {
        CARRIED.clear();
        EYES.clear();
        HEARD.clear();
        listener = null;
    }

    /**
     * The voice of {@code speaker}, as the listener hears it where they both are; the samples changed in place. Called
     * from the voice chat's own threads: it reads what the game noted, and a voice's first packet passes as it is.
     */
    public static short[] fromEntity(final UUID speaker, final short[] samples) {
        HEARD.add(speaker);
        final Vec3 eye = EYES.get(speaker);
        return eye == null ? samples : from(speaker, eye, samples);
    }

    /** A voice played from a place, as the listener hears it there; the samples changed in place. */
    public static short[] fromPlace(final UUID voice, final double x, final double y, final double z,
                                    final short[] samples) {
        return from(voice, new Vec3(x, y, z), samples);
    }

    private static short[] from(final UUID voice, final Vec3 at, final short[] samples) {
        final Level level = Minecraft.getInstance().level;
        final Vec3 listener = VoiceAcoustics.listener;
        if (level == null || listener == null) {
            return samples;
        }
        final double blocks = listener.distanceTo(at);
        final SoundMedium medium = ListenerAir.medium();
        final double carried = medium.gain(blocks);
        if (carried <= 0) {
            CARRIED.remove(voice);
            return new short[samples.length];
        }
        final float walls = AudioPrefsStore.prefs().occlusion() ? Walls.through(level, listener, at) : 1F;
        final double gain = carried * walls * RingingEars.heard();
        final double highs = medium.highs(blocks) * walls * walls * RingingEars.highs();
        final double cutoff = DULLEST + (CLEAREST - DULLEST) * highs * highs;
        final double step = 1 - Math.exp(-2 * Math.PI * cutoff / RATE);
        final double[] state = CARRIED.computeIfAbsent(voice, id -> new double[1]);
        double last = state[0];
        for (int i = 0; i < samples.length; i++) {
            last += step * (samples[i] - last);
            samples[i] = (short) Math.clamp(Math.round(last * gain), Short.MIN_VALUE, Short.MAX_VALUE);
        }
        state[0] = last;
        return samples;
    }

    private static @Nullable Entity findEntity(final ClientLevel level, final UUID id) {
        final Entity player = level.getPlayerByUUID(id);
        if (player != null) {
            return player;
        }
        for (final Entity entity : level.entitiesForRendering()) {
            if (entity.getUUID().equals(id)) {
                return entity;
            }
        }
        return null;
    }
}
