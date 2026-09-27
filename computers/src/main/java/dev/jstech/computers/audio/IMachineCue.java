/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.SoundCue;

/**
 * A sound a computer plays through its sound card, out of its monitors and speakers: raised by what happened, its
 * sound picked by the cue from the machine's context (its era, its system, its desktop), and holding one of the card's
 * voices while it rings.
 */
public sealed interface IMachineCue permits SystemSound, ProgramCue {

    /** The cue it raises. */
    SoundCue cue();

    /** How long it holds a voice of the machine's sound hardware, in ticks: as long as its longest sound rings. */
    int voiceTicks();
}
