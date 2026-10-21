/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The block entity of each block the studio asks what it is (a machine, what it sounds like, which model it is drawn
 * with): made once at the origin, in no world, and kept for the rest of the game, since a machine's is slow to make
 * and the answers never change. Asked only on the player's game's own thread.
 */
final class MadeBlockEntities {

    private static final Map<Block, Optional<BlockEntity>> MADE = new IdentityHashMap<>();

    private MadeBlockEntities() {
    }

    /** The block entity {@code block} makes, or none when it makes none or cannot make one alone. */
    static Optional<BlockEntity> of(final Block block) {
        return MADE.computeIfAbsent(block, MadeBlockEntities::make);
    }

    private static Optional<BlockEntity> make(final Block block) {
        if (!(block instanceof EntityBlock maker)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(maker.newBlockEntity(BlockPos.ZERO, block.defaultBlockState()));
        } catch (final RuntimeException notMadeAlone) {
            return Optional.empty();
        }
    }
}
