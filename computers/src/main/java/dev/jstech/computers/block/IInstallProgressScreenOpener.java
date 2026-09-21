/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.os.FirmwareKind;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the client-only installer screen at a copy already under way. Mirrors {@link IInstallDoneScreenOpener}:
 * the payload handler lives in common code, so the client registers the screen-opening lambda here during client
 * setup and the dedicated server never touches a screen class.
 */
@FunctionalInterface
public interface IInstallProgressScreenOpener {

    /**
     * Shows the installer copying, with the machine's own count of what is left.
     *
     * @param pos         the block position of the computer
     * @param monitorPos  the monitor the screen renders on
     * @param kind        the firmware variant matching the computer's hardware era
     * @param osName      the system being installed
     * @param targetLabel the disk it is going onto
     * @param ticksLeft   what the machine says is left of the copy
     * @param ticksTotal  how long the whole copy takes, so the bar knows how far along it is
     */
    void open(BlockPos pos, BlockPos monitorPos, FirmwareKind kind, String osName, String targetLabel,
              int ticksLeft, int ticksTotal);

    final class Holder {

        private Holder() {
        }

        @Nullable
        private static IInstallProgressScreenOpener instance;

        public static void set(final IInstallProgressScreenOpener opener) {
            instance = opener;
        }

        public static void open(final BlockPos pos, final BlockPos monitorPos, final FirmwareKind kind,
                                final String osName, final String targetLabel, final int ticksLeft,
                                final int ticksTotal) {
            if (instance != null) {
                instance.open(pos, monitorPos, kind, osName, targetLabel, ticksLeft, ticksTotal);
            }
        }
    }
}
