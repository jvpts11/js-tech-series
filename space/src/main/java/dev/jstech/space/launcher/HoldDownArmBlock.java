/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A hold-down arm, at a corner of a mobile launcher's opening, which keeps the rocket's foot down until its engines
 * reach full power. Once the deck forms, the one at the corner behind and left of the tower measures the deck, its
 * opening, its length to the tower and its width, and draws it all, deck, opening, clamps and masts, as the approved
 * launcher draws them; their blocks then show nothing of their own.
 */
public final class HoldDownArmBlock extends LauncherPieceBlock implements EntityBlock {

    private static final MapCodec<HoldDownArmBlock> CODEC = simpleCodec(HoldDownArmBlock::new);
    /* The farthest it looks across the opening and along the deck for its edges. */
    private static final int FARTHEST = 48;
    /* The mast a quarter turn round from the tower's way, straight out to its right. */
    private static final double RIGHT_ANGLE = 90.0;

    public HoldDownArmBlock(final BlockBehaviour.Properties properties) {
        super(properties, Kind.HOLD_DOWN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new LauncherDeckBlockEntity(pos, state);
    }

    /** It comes apart: what it drew goes. */
    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        super.partFormed(level, pos, formed);
        if (!formed && level.getBlockEntity(pos) instanceof LauncherDeckBlockEntity entity) {
            entity.deck(null);
        }
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /*
     * Formed, it measures the deck from the opening's corner its jaw reaches toward, and keeps the measure when it is
     * the arm behind and left of the tower; come apart, it keeps nothing.
     */
    @Override
    protected BlockState shaped(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (level.getBlockEntity(pos) instanceof LauncherDeckBlockEntity entity) {
            entity.deck(measure(level, pos, state.getValue(FACING)));
        }
        return state;
    }

    /* The deck as the arm at {@code pos}, its jaw toward {@code jaw} and its left, finds it; none unless it draws. */
    private static LauncherDeckBlockEntity.@Nullable Deck measure(final BlockGetter level, final BlockPos pos,
                                                                 final Direction jaw) {
        final Direction left = jaw.getCounterClockWise();
        final BlockPos corner = pos.relative(jaw).relative(left).below();
        int width = 0;
        while (width < FARTHEST && level.getBlockState(corner.relative(jaw, width)).isAir()) {
            width++;
        }
        if (width == 0) {
            return null;
        }
        final int open = (width - 1) / 2;
        final BlockPos axis = corner.relative(jaw, open).relative(left, open);
        // The mast standing straight out to the right of the tower's way, the deck's own, says which way the tower is.
        Direction side = null;
        final int mast = LauncherLayout.mast(open, RIGHT_ANGLE)[1];
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            if (kindOf(level, axis.above().relative(way, mast)) == Kind.MAST) {
                side = way.getCounterClockWise();
            }
        }
        if (side == null) {
            return null;
        }
        final Direction across = side.getClockWise();
        final BlockPos from = pos.subtract(axis.above());
        final int u = from.getX() * side.getStepX() + from.getZ() * side.getStepZ();
        final int v = from.getX() * across.getStepX() + from.getZ() * across.getStepZ();
        if (u >= 0 || v >= 0) {
            return null;
        }
        return new LauncherDeckBlockEntity.Deck(side, open, axis.above().subtract(pos), edge(level, axis, open,
                side.getOpposite()), edge(level, axis, open, side), edge(level, axis, open, across));
    }

    /* How far from the axis the deck's top reaches toward {@code way}. */
    private static int edge(final BlockGetter level, final BlockPos axis, final int open, final Direction way) {
        int step = open + 1;
        while (step < FARTHEST) {
            final Kind next = kindOf(level, axis.relative(way, step + 1));
            if (next != Kind.DECK && next != Kind.EXHAUST) {
                break;
            }
            step++;
        }
        return step;
    }
}
