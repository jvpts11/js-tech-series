/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.bigblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared lifecycle of a big block: one block bigger than a block, placed whole. When placed it raises its part
 * blocks round its origin, the cell the player placed, and binds them to it; when broken it takes the whole of itself
 * down again and gives back its contents and its own item, however it goes: broken by a player in survival, blown up,
 * or taken by whatever removes blocks without a player. A player in creative takes it and nothing falls.
 *
 * <p>A big block is not a multiblock: nothing is built in it block by block and it forms nothing. A multiblock is a
 * structure of its own blocks with a terminal (the structure kit in {@code dev.jstech.core.multiblock}).
 *
 * <p>Every big block has the identical skeleton and differs only in its footprint, the state it stamps on each part,
 * and what it gives back; those vary points are abstract hooks so the skeleton lives in exactly one place. A part
 * reaches back to its origin through the public {@link #dissolve} and {@link #brokenBy}, so breaking any cell of the
 * big block takes it down the same way.
 */
public abstract class AbstractBigBlock extends HorizontalDirectionalBlock implements EntityBlock, IBigBlock {

    /*
     * Per-block process-wide reentrancy guard. onRemove fires again for every cell removed during a dissolve, so
     * without this the teardown would recurse into itself; the origin keys the teardown in flight.
     */
    private final Set<BlockPos> dissolving = ConcurrentHashMap.newKeySet();
    /*
     * The big blocks a player is breaking right now, whose contents fell as the player broke them, or were forgone by a
     * player in creative: the removal that follows gives nothing back a second time. A removal with no player first
     * finds its origin missing here and gives everything back then.
     */
    private final Set<BlockPos> settled = ConcurrentHashMap.newKeySet();

    protected AbstractBigBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public final List<BlockPos> footprint(final BlockPos origin, final Direction facing) {
        return geometry().allPositions(origin, facing);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        /*
         * Build the whole footprint on BOTH sides so the client predicts the big block at once; only the server binds
         * each part back to its origin.
         */
        final Direction facing = state.getValue(FACING);
        final boolean server = !level.isClientSide();
        for (final BlockPos part : geometry().partPositions(pos, facing)) {
            level.setBlock(part, partStateFor(pos, facing, part, state), Block.UPDATE_ALL);
            if (server && level.getBlockEntity(part) instanceof BigBlockPartBlockEntity partBe) {
                partBe.setOrigin(pos);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        if (level instanceof ServerLevel serverLevel) {
            brokenBy(serverLevel, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Reentrancy-guarded teardown, also callable from a part block when it is the cell that is broken. */
    public void dissolve(final ServerLevel level, final BlockPos origin, final Direction facing) {
        if (!dissolving.add(origin.immutable())) {
            return;
        }
        try {
            for (final BlockPos part : geometry().partPositions(origin, facing)) {
                if (isOwnPart(level.getBlockState(part))) {
                    level.removeBlock(part, false);
                }
            }
            if (isOwnOrigin(level.getBlockState(origin))) {
                level.removeBlock(origin, false);
            }
        } finally {
            dissolving.remove(origin);
        }
    }

    /**
     * A player is breaking the big block, at its origin or at one of its parts: its contents and its own item fall now,
     * unless the player is in creative, who takes it and leaves nothing. Either way the removal that follows gives
     * nothing back a second time.
     */
    public void brokenBy(final ServerLevel level, final BlockPos origin, final Player player) {
        if (settled.add(origin.immutable()) && !player.getAbilities().instabuild) {
            dropContents(level, origin);
        }
    }

    /** Its footprint; also backs {@link IBigBlock#footprint(BlockPos, Direction)}. */
    protected abstract IBigBlockGeometry geometry();

    /** True only for a block that is one of THIS big block's parts. */
    protected abstract boolean isOwnPart(BlockState state);

    /** True only for this block itself at its origin (the cell the player placed). */
    protected abstract boolean isOwnOrigin(BlockState state);

    /** Builds the state of one raised part at {@code part}. */
    protected abstract BlockState partStateFor(BlockPos origin, Direction facing, BlockPos part,
                                               BlockState originState);

    /** Spills the big block's contents and pops its item; called once for each time it goes. */
    protected abstract void dropContents(ServerLevel level, BlockPos origin);

    /** Tells the block entity at the origin it is being taken down. Nothing by default. */
    protected void onOriginBroken(final ServerLevel level, final BlockPos origin) {
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        // Lifted whole, as a moving structure takes its blocks, the big block is moved, not broken.
        if (!state.is(newState.getBlock()) && !movedByPiston && level instanceof ServerLevel serverLevel) {
            if (!settled.remove(pos)) {
                dropContents(serverLevel, pos);
            }
            onOriginBroken(serverLevel, pos);
            dissolve(serverLevel, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
