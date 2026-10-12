/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
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
 * The shared lifecycle of a multiblock controller: when placed it raises its part blocks and binds them to itself, and
 * when broken it takes the whole structure down again and gives back its contents and its own item, however it goes:
 * broken by a player in survival, blown up, or taken by whatever removes blocks without a player. A player in
 * creative takes it and nothing falls. Every controller has the identical skeleton and differs only in its footprint
 * geometry, the per-part state it stamps, and what it gives back; those vary points are abstract hooks so the skeleton
 * lives in exactly one place. A part block reaches back into its controller through the public {@link #dissolve} /
 * {@link #brokenBy} entry points, so breaking any cell of the structure tears down the whole thing the same way.
 */
public abstract class AbstractMultiblockControllerBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock, IMultiblockBlock {

    /*
     * Per-controller process-wide reentrancy guard. onRemove fires again for every cell removed during a dissolve, so
     * without this the teardown would recurse into itself; the controller position keys the in-flight teardown.
     */
    private final Set<BlockPos> dissolving = ConcurrentHashMap.newKeySet();
    /*
     * The controllers a player is breaking right now, whose contents fell as the player broke them, or were forgone by
     * a player in creative: the removal that follows gives nothing back a second time. A removal with no player first
     * finds its controller missing here and gives everything back then.
     */
    private final Set<BlockPos> settled = ConcurrentHashMap.newKeySet();

    protected AbstractMultiblockControllerBlock(final Properties properties) {
        super(properties);
    }

    // the vary points each subclass supplies

    /** Footprint geometry; also backs {@link IMultiblockBlock#footprint(BlockPos, Direction)}. */
    protected abstract IMultiblockGeometry geometry();

    /** True only for a block that belongs to THIS controller's part set. */
    protected abstract boolean isOwnPart(BlockState state);

    /** True only for this controller block itself (the cell the player placed). */
    protected abstract boolean isOwnController(BlockState state);

    /** Build the BlockState for one raised part at {@code part}. */
    protected abstract BlockState partStateFor(
            BlockPos controller, Direction facing, BlockPos part, BlockState controllerState);

    /** Spill the controller's contents and pop the controller item; called once for each time the structure goes. */
    protected abstract void dropContents(ServerLevel level, BlockPos controller);

    /** Notify the controller block entity it is being torn down. No-op by default. */
    protected void onControllerBroken(final ServerLevel level, final BlockPos controller) {
    }

    // the shared lifecycle (identical for every controller)

    @Override
    public final List<BlockPos> footprint(final BlockPos origin, final Direction facing) {
        return geometry().allPositions(origin, facing);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        /*
         * Build the whole footprint on BOTH sides so the client predicts the structure at once; only the server binds
         * each part back to its controller.
         */
        final Direction facing = state.getValue(FACING);
        final boolean server = !level.isClientSide();
        for (final BlockPos part : geometry().partPositions(pos, facing)) {
            level.setBlock(part, partStateFor(pos, facing, part, state), Block.UPDATE_ALL);
            if (server && level.getBlockEntity(part) instanceof MultiblockPartBlockEntity partBe) {
                partBe.setController(pos);
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

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            if (!settled.remove(pos)) {
                dropContents(serverLevel, pos);
            }
            onControllerBroken(serverLevel, pos);
            dissolve(serverLevel, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Reentrancy-guarded teardown, also callable from a part block when it is the cell that is broken. */
    public void dissolve(final ServerLevel level, final BlockPos controllerPos, final Direction facing) {
        if (!dissolving.add(controllerPos.immutable())) {
            return;
        }
        try {
            for (final BlockPos part : geometry().partPositions(controllerPos, facing)) {
                if (isOwnPart(level.getBlockState(part))) {
                    level.removeBlock(part, false);
                }
            }
            if (isOwnController(level.getBlockState(controllerPos))) {
                level.removeBlock(controllerPos, false);
            }
        } finally {
            dissolving.remove(controllerPos);
        }
    }

    /**
     * A player is breaking the structure, at its controller or at one of its parts: its contents and its own item fall
     * now, unless the player is in creative, who takes the machine and leaves nothing. Either way the removal that
     * follows gives nothing back a second time.
     */
    public void brokenBy(final ServerLevel level, final BlockPos controllerPos, final Player player) {
        if (settled.add(controllerPos.immutable()) && !player.getAbilities().instabuild) {
            dropContents(level, controllerPos);
        }
    }
}
