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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Players' voices, as a voice chat mod plays them, through the same world as every other sound: muffled and dull
 * behind walls, weaker in thin air, and silent through a vacuum unless the speakers touch, so in space players hear
 * each other only by radio. A voice that comes from no place (a radio, a group call) is left as it is.
 */
public final class VoiceAcoustics {

    /* The rate a voice chat samples at, in hertz. */
    private static final double RATE = 48_000;
    /* The lowest a voice's high notes are cut to behind the thickest walls, and the highest, in hertz. */
    private static final double DULLEST = 500;
    private static final double CLEAREST = 20_000;
    /* Where each voice's filter left off, so one packet runs into the next without a click. */
    private static final Map<UUID, double[]> CARRIED = new ConcurrentHashMap<>();

    private VoiceAcoustics() {
    }

    /** The voice of {@code speaker}, as the listener hears it where they both are; the samples changed in place. */
    public static short[] fromEntity(final UUID speaker, final short[] samples) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft.level;
        final Entity entity = level == null ? null : findEntity(level, speaker);
        return entity == null ? samples : from(speaker, entity.getEyePosition(), samples);
    }

    /** A voice played from a place, as the listener hears it there; the samples changed in place. */
    public static short[] fromPlace(final UUID voice, final double x, final double y, final double z,
                                    final short[] samples) {
        return from(voice, new Vec3(x, y, z), samples);
    }

    private static short[] from(final UUID voice, final Vec3 at, final short[] samples) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft.level;
        if (level == null || minecraft.gameRenderer == null) {
            return samples;
        }
        final Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
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

    private static @Nullable Entity findEntity(final Level level, final UUID id) {
        final Entity player = level.getPlayerByUUID(id);
        if (player != null) {
            return player;
        }
        for (final Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity.getUUID().equals(id)) {
                return entity;
            }
        }
        return null;
    }
}
