/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A screen in the world a computer's picture is shown on: a monitor, or the glass of a computer that is its own
 * screen, as an all-in-one is. A player sits down at a screen, and every window opened there stays open only while
 * the screen still shows the machine it was opened for.
 */
public interface IScreen {

    /** Where the screen stands. */
    BlockPos getBlockPos();

    /** Whether the screen is lit: the machine it shows runs and drives it. */
    boolean lit();

    /**
     * The machine whose picture the screen shows: the one a remote session put there, or else its own (the computer its
     * cable links to, or the computer it is the glass of); null for none.
     */
    @Nullable
    BlockPos shownMachine();

    /** Whether the screen legitimately shows {@code machine} now, which the windows open on it validate through. */
    boolean shows(BlockPos machine);

    /** Starts (or ends, with {@code null}) a remote session showing {@code machine} on this screen. */
    void setRemoteSession(@Nullable BlockPos machine);
}
