/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A flame a block burns, a torch, a candle or a mod's lamp, which needs oxygen as fire does: where the air holds too
 * little for a fire, the Core puts it out, when it is lit or set there and when the room it burns in runs short of
 * oxygen. Declared with {@link Flames#declare}.
 */
public interface IFlame {

    /** Whether {@code state} is this flame, burning. */
    boolean burning(BlockState state);

    /** Puts out the flame of {@code state} at {@code pos}: a candle left unlit, a torch fallen, as the flame has it. */
    void putOut(ServerLevel level, BlockPos pos, BlockState state);
}
