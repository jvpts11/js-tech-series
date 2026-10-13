/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.id.IStableName;

/**
 * The sounds the series' own programs make through the machine's sound card: the minefield's clicks, its explosion
 * and its win. A long task finishing or failing has none of its own: the program raises a notice or an error box, and
 * the system sounds it, as real programs do.
 */
public enum ProgramCue implements IMachineCue, IStableName {

    MINESWEEPER_CLICK("minesweeper_click"),
    MINESWEEPER_EXPLODE("minesweeper_explode"),
    MINESWEEPER_WIN("minesweeper_win");

    private final String serializedName;

    ProgramCue(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public SoundCue cue() {
        return switch (this) {
            case MINESWEEPER_CLICK -> ComputingSounds.PROGRAM_MINESWEEPER_CLICK;
            case MINESWEEPER_EXPLODE -> ComputingSounds.PROGRAM_MINESWEEPER_EXPLODE;
            case MINESWEEPER_WIN -> ComputingSounds.PROGRAM_MINESWEEPER_WIN;
        };
    }

    @Override
    public int voiceTicks() {
        return switch (this) {
            case MINESWEEPER_CLICK -> 2;
            case MINESWEEPER_EXPLODE -> 10;
            case MINESWEEPER_WIN -> 46;
        };
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
