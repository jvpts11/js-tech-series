/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.registry.ClientHook;
import net.minecraft.core.BlockPos;

/**
 * Opens the client-only boot manager menu. Mirrors {@link IPostScreenOpener}: the payload handler lives in common
 * code, so the client registers the screen-opening lambda here during client setup and the dedicated server never
 * touches a screen class.
 */
@FunctionalInterface
public interface IBootMenuScreenOpener {

    /**
     * Shows the machine's boot manager on its monitor.
     *
     * @param pos        the block position of the computer
     * @param monitorPos the monitor the menu renders on
     * @param menu       the entries the machine found, and which of them it will boot by itself
     * @param remaining  the ticks left of the wait, or zero once a key has stopped it
     */
    void open(BlockPos pos, BlockPos monitorPos, BootMenu menu, int remaining);

    final class Holder {

        private static final ClientHook<IBootMenuScreenOpener> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final IBootMenuScreenOpener opener) {
            HOOK.set(opener);
        }

        public static void open(final BlockPos pos, final BlockPos monitorPos, final BootMenu menu,
                                final int remaining) {
            HOOK.ifPresent(target -> target.open(pos, monitorPos, menu, remaining));
        }
    }
}
