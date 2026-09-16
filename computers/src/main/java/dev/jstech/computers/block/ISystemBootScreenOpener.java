/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
     * @param osId       the system coming up, as the registry names it
     * @param osName     the system as a person reads it
     * @param remaining  what the machine says is left of it
     * @param total      how long the whole thing takes, so the bar knows how far along it is
     */
    void open(BlockPos pos, BlockPos monitorPos, ResourceLocation osId, String osName, int remaining, int total);

    final class Holder {

        private Holder() {
        }

        @Nullable
        private static ISystemBootScreenOpener instance;

        public static void set(final ISystemBootScreenOpener opener) {
            instance = opener;
        }

        public static void open(final BlockPos pos, final BlockPos monitorPos, final ResourceLocation osId,
                                final String osName, final int remaining, final int total) {
            if (instance != null) {
                instance.open(pos, monitorPos, osId, osName, remaining, total);
            }
        }
    }
}
