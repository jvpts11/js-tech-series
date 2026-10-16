/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * What a block holding parts, a cable or a space of parts alone, shows of them in its state: the light its parts give
 * and whether any needs its tick, and, for a space alone, the faces its pieces cover. The game reads light and ticks
 * from a block's state, so they are kept there, set again each time the parts change, and never read from the block
 * entity, which the light engine may reach before it is loaded.
 */
public final class PartStates {

    /** The light the block's parts give. */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    /** Whether any of the block's parts needs its tick. */
    public static final BooleanProperty TICKING = BooleanProperty.create("ticking");

    private PartStates() {
    }

    /** {@code state} showing {@code parts}: their light, whether they tick, and for a space alone what they cover. */
    public static BlockState shown(final BlockState state, final SpaceParts parts) {
        BlockState next = state;
        if (next.hasProperty(LIGHT)) {
            next = next.setValue(LIGHT, parts.light());
        }
        if (next.hasProperty(TICKING)) {
            next = next.setValue(TICKING, parts.ticks());
        }
        if (next.getBlock() instanceof SpaceBlock) {
            next = SpaceBlock.covering(next, parts.coveredFaces());
        }
        return next;
    }

    /** Sets in the state of the block at {@code pos} what {@code parts} show, when that changed; on the server. */
    public static void show(final Level level, final BlockPos pos, final SpaceParts parts) {
        final BlockState state = level.getBlockState(pos);
        final BlockState next = shown(state, parts);
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }

    /** Whether the state of the block at {@code pos} shows {@code parts} as they stand. */
    public static boolean showing(final Level level, final BlockPos pos, final SpaceParts parts) {
        final BlockState state = level.getBlockState(pos);
        return shown(state, parts) == state;
    }

    /** The light a state of a block holding parts gives: its parts'. */
    public static int lightOf(final BlockState state) {
        return state.hasProperty(LIGHT) ? state.getValue(LIGHT) : 0;
    }
}
