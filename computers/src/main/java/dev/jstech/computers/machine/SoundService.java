/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.audio.ProgramSounds;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The machine's sound, as what runs on it reaches it: a beep from the speaker in its case, a tune through its sound
 * card, a recording from its disks out of its monitors and speakers or out of one speaker by name.
 */
public final class SoundService {

    private final AbstractComputerBlockEntity machine;
    private final ServerLevel level;

    public SoundService(final AbstractComputerBlockEntity machine, final ServerLevel level) {
        this.machine = machine;
        this.level = level;
    }

    /** A beep of that pitch and length from the speaker in the case, which cuts the one before it off. */
    public void beep(final int frequency, final int millis) {
        this.sounds().beep(this.level, frequency, millis);
    }

    /**
     * Plays a tune, its notes by name or pitch with how long each lasts, several together joined by a plus.
     *
     * @throws IllegalArgumentException when an item of it is no note, naming it
     */
    public boolean tones(final String tune) {
        return this.sounds().tones(this.level, tune);
    }

    /** Plays a recording from the machine's disks out of its monitors and speakers. */
    public boolean play(final String path) {
        return this.sounds().play(this.level, path);
    }

    /** Stops the recordings the machine's programs are playing. */
    public void stop() {
        this.sounds().stop(this.level);
    }

    /** The speaker of that name linked to the machine, or null. */
    @Nullable
    public SpeakerBlockEntity speaker(final String name) {
        return this.sounds().speaker(name);
    }

    /** Plays a recording out of the speaker of that name alone; false when there is none, or it cannot play it. */
    public boolean playOn(final String name, final String path) {
        final SpeakerBlockEntity speaker = this.speaker(name);
        return speaker != null && this.sounds().playOn(this.level, speaker, path);
    }

    private ProgramSounds sounds() {
        return this.machine.programSounds();
    }
}
