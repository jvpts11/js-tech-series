/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Told of each fire burning, about once a second, as many as the fire kit looks at in a second: what a mod does with
 * a fire that burns on, the soot it gives off, a fire alarm's call. A watcher that needs the fire's class asks
 * {@link FireClasses#at} for it. Called on the server's thread.
 */
@FunctionalInterface
public interface IFireWatcher {

    /** The fire at {@code fire} in {@code level} burns on. */
    void burning(ServerLevel level, BlockPos fire);
}
