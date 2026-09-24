/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

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
     * @throws IllegalArgumentException when the sound belongs to the interface and not to the world
     */
    public static boolean at(final ServerLevel level, final double x, final double y, final double z,
                             final SoundKey sound, final float volume, final float pitch) {
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
     * @throws IllegalArgumentException when the sound belongs to the world and not to the interface
     */
    public static void onScreen(final ServerPlayer player, final SoundKey sound, final float volume,
                                final float pitch) {
        if (sound.spec().space() != SoundSpace.INTERFACE) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the world, not of the interface");
        }
        PacketDistributor.sendToPlayer(player, new ScreenSoundPayload(sound.id(), volume, pitch));
    }
}
