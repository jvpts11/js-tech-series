/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** The block a part is mounted on, as the part sees it. */
public interface IPartHost {

    /** The level the block is in, or null before it is placed in one. */
    @Nullable
    Level partLevel();

    /** Where the block is. */
    BlockPos partPos();

    /** Something a part keeps changed: the block is to be saved and its players sent what they see. */
    void partChanged();

    /**
     * Only how a part looks changed, such as whether it is at work: its players are sent what they see, and nothing
     * a part closes or opens on the block is worked out again.
     */
    default void partLooksChanged() {
        partChanged();
    }

    /** The server level the block is in, or null on a player's game or before it is placed. */
    default @Nullable ServerLevel partServerLevel() {
        return partLevel() instanceof ServerLevel server ? server : null;
    }
}
