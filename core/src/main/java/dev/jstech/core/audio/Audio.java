/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.audio.pcm.Tone;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
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
 *
 * <p>A cue ({@link #cue}) is sent as what happened and the context it happened in, and each client picks the sound
 * from its own packs' bindings. A machine with hardware of its own ({@link IAudioHost}) plays cues and notes from its
 * speakers, at its volume, through its device.
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

    /** Raises a cue of the world from the middle of that block, its sound picked by that context on each client. */
    public static boolean cue(final ServerLevel level, final BlockPos pos, final SoundCue cue,
                              final SoundContext context) {
        return cue(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cue, context, 1.0F, 1.0F);
    }

    /**
     * Raises a cue of the world from that point, to every player near enough to hear it; each picks the sound from the
     * bindings their own packs give, by that context.
     *
     * @return whether it was raised, which it is not when the same cue came from the same place a moment ago
     * @throws IllegalArgumentException when the cue belongs to the interface and not to the world
     */
    public static boolean cue(final ServerLevel level, final double x, final double y, final double z,
                              final SoundCue cue, final SoundContext context, final float volume, final float pitch) {
        return cue(level, x, y, z, cue, context, volume, pitch, StereoSide.BOTH, FrequencyResponse.FULL);
    }

    /**
     * Raises {@code cue} at a point in the world through a speaker that plays {@code side} of a stereo recording and
     * reproduces what {@code response} lets it: one of a pair, or a cheap one.
     */
    public static boolean cue(final ServerLevel level, final double x, final double y, final double z,
                              final SoundCue cue, final SoundContext context, final float volume, final float pitch,
                              final StereoSide side, final FrequencyResponse response) {
        if (cue.space() != SoundSpace.WORLD) {
            throw new IllegalArgumentException(cue.id() + " is a cue of the interface, not of the world");
        }
        final String source = level.dimension().location() + "@" + BlockPos.containing(x, y, z).asLong();
        if (!GATE.allow(source, cue.id().toString(), level.getGameTime())) {
            return false;
        }
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, cue.range(),
                new CueSoundPayload(cue.id(), false, x, y, z, context, volume, pitch, side, response));
        return true;
    }

    /**
     * Raises a cue of the interface for one player alone, from their own screen.
     *
     * @throws IllegalArgumentException when the cue belongs to the world and not to the interface
     */
    public static void cueOnScreen(final ServerPlayer player, final SoundCue cue, final SoundContext context) {
        if (cue.space() != SoundSpace.INTERFACE) {
            throw new IllegalArgumentException(cue.id() + " is a cue of the world, not of the interface");
        }
        PacketDistributor.sendToPlayer(player, new CueSoundPayload(cue.id(), true, 0, 0, 0, context, 1.0F, 1.0F));
    }

    /**
     * Raises a cue of the world through a machine's own hardware: from each of its speakers, at its volume, picked by
     * its context with its device in it. A machine whose device makes no sound, or whose volume is down, is silent.
     *
     * @return how many of its speakers it was raised from
     */
    public static int cue(final ServerLevel level, final IAudioHost host, final SoundCue cue) {
        final AudioDevice device = host.audioDevice();
        final float volume = host.audioVolume();
        if (!device.audible() || volume <= 0.0F) {
            return 0;
        }
        final SoundContext context = host.soundContext().with(SoundContext.DEVICE, device.id());
        int raised = 0;
        for (final AudioOutput speaker : host.outputs()) {
            final Vec3 at = speaker.position();
            // What the device keeps of a recording reaches every speaker, and a mono device has no sides to give.
            final StereoSide side = device.stereo() ? speaker.side() : StereoSide.BOTH;
            if (cue(level, at.x, at.y, at.z, cue, context, Math.min(1.0F, volume), 1.0F, side,
                    device.response().through(speaker.response()))) {
                raised++;
            }
        }
        return raised;
    }

    /**
     * Plays notes through a machine's own hardware: as its device plays them (a PC speaker makes every shape a square
     * wave), from each of its speakers, at its volume. A device that synthesises nothing plays none of them.
     *
     * @return how many of its speakers played them
     * @throws IllegalArgumentException when the sound is not a sound of the world made as it plays, or there are no
     *                                  notes or more than {@link ToneSoundPayload#MAX_TONES}
     */
    public static int tones(final ServerLevel level, final IAudioHost host, final SoundKey sound,
                            final List<Tone> tones) {
        requireMade(sound, tones);
        if (sound.spec().space() != SoundSpace.WORLD) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the interface, not of the world");
        }
        final float volume = Math.min(1.0F, host.audioVolume());
        final List<Tone> played = new ArrayList<>();
        for (final Tone tone : host.audioDevice().adapt(tones)) {
            played.add(new Tone(tone.wave(), tone.frequency(), tone.millis(), tone.volume() * volume));
        }
        if (played.isEmpty() || volume <= 0.0F) {
            return 0;
        }
        for (final Vec3 speaker : host.audioOutputs()) {
            PacketDistributor.sendToPlayersNear(level, null, speaker.x, speaker.y, speaker.z, sound.spec().range(),
                    new ToneSoundPayload(sound.id(), false, speaker.x, speaker.y, speaker.z, played));
        }
        return host.audioOutputs().size();
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
