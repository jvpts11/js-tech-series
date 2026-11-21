/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** What a mod tells an agent of a block, a machine working or a computer and its work; nothing of others' blocks. */
@FunctionalInterface
public interface IBlockReading {

    /**
     * Tells {@code report} what there is to tell of the block at {@code pos} of {@code level}, on the side the agent
     * reads from, a place of a moving structure's yard for a block aboard one; nothing for a block not the mod's.
     */
    void read(Level level, BlockPos pos, AgentReport report);
}
