/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A screen on a block's face that answers the player's crosshair as a screen answers the mouse: the block says where
 * on which face it shows a screen and how many pixels it has, and what a press at a pixel does, on the server. The
 * player's game finds the pixel under the crosshair, for the screen's picture to light what is under it, and a right
 * click there presses it instead of using the block.
 */
public interface ITouchScreen {

    /** The screen on the face of {@code state} looking {@code face}, if it shows one there. */
    Optional<TouchArea> area(BlockState state, Direction face);

    /** What a press at the pixel {@code x}, {@code y} of the screen on {@code face} does, on the server. */
    void pressed(ServerPlayer player, BlockPos pos, Direction face, int x, int y);
}
