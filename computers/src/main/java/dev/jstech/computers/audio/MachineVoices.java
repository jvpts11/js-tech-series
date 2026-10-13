/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.VoicePool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;

/**
 * The voices of a computer's sound hardware, and the sounds holding them: the sound card, or the sound on its board,
 * plays as many sounds at once as it has voices, and the speaker inside the case one. A sound that finds none free
 * takes those of the sound that started first, which stops, the way the chips of the time gave a new note the channel
 * of the oldest; a beep cuts the one before it off.
 *
 * <p>A stereo song takes two voices of a stereo device and a mono one one, each note of a chord one, a system's chime
 * one. It is part of the computer, like the music it plays; the voices are forgotten when the computer is.
 */
public final class MachineVoices {

    private final AbstractComputerBlockEntity computer;
    private final VoicePool card = new VoicePool();
    private final VoicePool caseSpeaker = new VoicePool();
    /** What stops each sound holding voices, should another take them. */
    private final Map<String, Consumer<ServerLevel>> stops = new HashMap<>();
    private long next;

    /** A sound that stops by itself and is never cut short from here: a chime already on its way. */
    public static final Consumer<ServerLevel> LET_RING = level -> { };

    public MachineVoices(final AbstractComputerBlockEntity computer) {
        this.computer = computer;
    }

    /** A new name for one sound of this machine, told apart from every other it makes. */
    public String name(final String what) {
        return JsComputers.MODID + ":voice/" + computer.getBlockPos().asLong() + "/" + what + "/" + next++;
    }

    /** Whether its notes come from the speaker in the case, which is all a machine with no sound card has. */
    public boolean inCase() {
        final AudioDevice device = computer.audioHost().audioDevice();
        return !device.samples();
    }

    /**
     * Gives a sound voices, stopping the sounds whose voices it takes.
     *
     * @param inCase whether it plays from the speaker in the case rather than through the sound card
     * @param id     what the sound is known by, from {@link #name}
     * @param endsAt when it ends by itself, in ticks, or {@link Long#MAX_VALUE}
     * @param stop   what stops it should another sound take its voices
     * @return whether it has a voice at all; a device with none plays nothing
     */
    public boolean take(final ServerLevel level, final boolean inCase, final String id, final int voices,
                        final long endsAt, final Consumer<ServerLevel> stop) {
        final VoicePool pool = inCase ? caseSpeaker : card;
        final int capacity = inCase ? 1 : computer.audioHost().audioDevice().voices();
        if (capacity <= 0) {
            return false;
        }
        final List<String> stolen = pool.claim(capacity, id, voices, level.getGameTime(), endsAt);
        stops.put(id, stop);
        for (final String taken : stolen) {
            final Consumer<ServerLevel> stopIt = stops.remove(taken);
            if (stopIt != null) {
                stopIt.accept(level);
            }
        }
        final long now = level.getGameTime();
        stops.keySet().removeIf(held -> !card.holds(held, now) && !caseSpeaker.holds(held, now));
        return true;
    }

    /** The sound stopped by itself, or was stopped: its voices are free. */
    public void release(final String id) {
        card.release(id);
        caseSpeaker.release(id);
        stops.remove(id);
    }

    /** How many voices of the card, or of the speaker in the case, are held now. */
    public int held(final ServerLevel level, final boolean inCase) {
        return (inCase ? caseSpeaker : card).held(level.getGameTime());
    }

    /** Whether that sound still holds its voices. */
    public boolean holds(final ServerLevel level, final String id) {
        return card.holds(id, level.getGameTime()) || caseSpeaker.holds(id, level.getGameTime());
    }

    /** The machine went off or is gone: every sound it held stops and every voice is free. */
    public void silence(final ServerLevel level) {
        if (stops.isEmpty() && card.empty() && caseSpeaker.empty()) {
            return;
        }
        final List<Consumer<ServerLevel>> all = List.copyOf(stops.values());
        stops.clear();
        card.clear();
        caseSpeaker.clear();
        all.forEach(stop -> stop.accept(level));
    }
}
