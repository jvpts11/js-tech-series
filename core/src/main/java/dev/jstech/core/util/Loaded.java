/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the world only where it is loaded.
 *
 * <p>On the server, a block entity asked for at a place whose chunk is not loaded makes the game load that chunk there
 * and then, on the server's thread: a disk read or a generation in the middle of a tick. Worse, a chunk read that way
 * while it is being unloaded takes a hold the unload waits on, and the unload, rescheduling itself in the very queue
 * the server is emptying, then eats every tick's spare time, which is the time the chunk's own loading needed to
 * finish and let go of that hold: the server keeps ticking but no chunk ever loads again.
 *
 * <p>So whatever is remembered by position and may lie anywhere (a server of the network, a linked peripheral, the
 * rack of a cluster node) is read through here, and a place outside the loaded world answers as empty.
 */
public final class Loaded {

    private Loaded() {
    }

    /** The block entity at {@code pos}, or null when there is none or its chunk is not loaded. */
    @Nullable
    public static BlockEntity blockEntity(final Level level, final BlockPos pos) {
        return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

}
