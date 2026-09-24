/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.audio.pcm.Tone;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * How the server plays the series' sounds: every sound goes through here and never through the game's own calls, so
 * each passes the same gate and is mixed in its channel on every client that hears it.
 *
 * <p>A sound of the world is heard by whoever is near where it comes from, a machine's sounds included: a computer
 * that powers on is heard by the players in the room, not only by the one at its monitor. A sound of the interface
 * is the player's own screen answering them, and nobody else hears it.
 *
 * <p>A sound made as it plays is sent as its notes ({@link #tones}), which each client synthesises. It does not pass
 * the gate: a program that beeps twice in a row means both beeps.
 */
public final class Audio {

    /** The same sound from the same place is heard once inside this many ticks. */
    private static final int COOLDOWN_TICKS = 2;

    private static final SoundGate GATE = new SoundGate(COOLDOWN_TICKS);

    private Audio() {
    }

    /** Plays a sound of the world from the middle of that block, as loud and as high as it was recorded. */
    public static boolean at(final ServerLevel level, final BlockPos pos, final SoundKey sound) {
        return at(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, 1.0F, 1.0F);
    }

    /**
     * Plays a sound of the world from that point, to every player near enough to hear it.
     *
     * @return whether it was played, which it is not when the same sound came from the same place a moment ago
     * @throws IllegalArgumentException when the sound belongs to the interface, or is made as it plays
     */
    public static boolean at(final ServerLevel level, final double x, final double y, final double z,
                             final SoundKey sound, final float volume, final float pitch) {
        requireFiles(sound);
        if (sound.spec().space() != SoundSpace.WORLD) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the interface, not of the world");
        }
        final String source = level.dimension().location() + "@" + BlockPos.containing(x, y, z).asLong();
        if (!GATE.allow(source, sound.id().toString(), level.getGameTime())) {
            return false;
        }
        level.playSound(null, x, y, z, sound.event().get(), sound.spec().channel().source(), volume, pitch);
        return true;
    }

    /**
     * Plays a sound of the interface for one player alone, from their own screen.
     *
     * @throws IllegalArgumentException when the sound belongs to the world, or is made as it plays
     */
    public static void onScreen(final ServerPlayer player, final SoundKey sound, final float volume,
                                final float pitch) {
        requireFiles(sound);
        if (sound.spec().space() != SoundSpace.INTERFACE) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the world, not of the interface");
        }
        PacketDistributor.sendToPlayer(player, new ScreenSoundPayload(sound.id(), volume, pitch));
    }

    /**
     * Plays notes from the middle of that block, synthesised by each client near enough to hear them: a computer's
     * speaker, a machine's chime.
     *
     * @throws IllegalArgumentException when the sound is not a sound of the world made as it plays, or there are no
     *                                  notes or more than {@link ToneSoundPayload#MAX_TONES}
     */
    public static void tones(final ServerLevel level, final BlockPos pos, final SoundKey sound,
                             final List<Tone> tones) {
        requireMade(sound, tones);
        if (sound.spec().space() != SoundSpace.WORLD) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the interface, not of the world");
        }
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, sound.spec().range(),
                new ToneSoundPayload(sound.id(), false, x, y, z, tones));
    }

    /**
     * Plays notes for one player alone, from their own screen.
     *
     * @throws IllegalArgumentException when the sound is not a sound of the interface made as it plays, or there are
     *                                  no notes or more than {@link ToneSoundPayload#MAX_TONES}
     */
    public static void tonesOnScreen(final ServerPlayer player, final SoundKey sound, final List<Tone> tones) {
        requireMade(sound, tones);
        if (sound.spec().space() != SoundSpace.INTERFACE) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the world, not of the interface");
        }
        PacketDistributor.sendToPlayer(player, new ToneSoundPayload(sound.id(), true, 0, 0, 0, tones));
    }

    private static void requireFiles(final SoundKey sound) {
        if (sound.spec().made()) {
            throw new IllegalArgumentException(sound.id() + " is made as it plays, so it is played from its notes");
        }
    }

    private static void requireMade(final SoundKey sound, final List<Tone> tones) {
        if (!sound.spec().made()) {
            throw new IllegalArgumentException(sound.id() + " plays its own files, not notes it is handed");
        }
        if (tones.isEmpty() || tones.size() > ToneSoundPayload.MAX_TONES) {
            throw new IllegalArgumentException("between 1 and " + ToneSoundPayload.MAX_TONES + " notes are sent at"
                    + " once, not " + tones.size());
        }
    }
}
