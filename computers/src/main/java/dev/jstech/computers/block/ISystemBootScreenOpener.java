/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.os.boot.BootSequence;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the client-only screen of a system coming up. Mirrors {@link IPostScreenOpener}: the payload handler lives
 * in common code, so the client registers the screen-opening lambda here during client setup and the dedicated
 * server never touches a screen class.
 */
@FunctionalInterface
public interface ISystemBootScreenOpener {

    /**
     * Shows the system coming up on that machine's monitor.
     *
     * @param pos        the block position of the computer
     * @param monitorPos the monitor the sequence renders on
     * @param sequence   what the system has to say while it comes up, worked out from the machine
     * @param remaining  what the machine says is left of it
     * @param total      how long the whole thing takes, so the steps and the bar know how far along they are
     */
    void open(BlockPos pos, BlockPos monitorPos, BootSequence sequence, int remaining, int total);

    final class Holder {

        private Holder() {
        }

        @Nullable
        private static ISystemBootScreenOpener instance;

        public static void set(final ISystemBootScreenOpener opener) {
            instance = opener;
        }

        public static void open(final BlockPos pos, final BlockPos monitorPos, final BootSequence sequence,
                                final int remaining, final int total) {
            if (instance != null) {
                instance.open(pos, monitorPos, sequence, remaining, total);
            }
        }
    }
}
