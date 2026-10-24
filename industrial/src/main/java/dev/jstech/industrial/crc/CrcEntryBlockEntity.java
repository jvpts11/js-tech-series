/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An entry of a Computational Research Centre: a part of its structure that the structure makes the port of its kind,
 * passing energy, water or heat to its terminal; and the Link Socket, which the Research Link Cable plugs into on its
 * front to join the CRC to a group. The terminal reads the group itself, once a second, so the socket only says it is
 * a member.
 */
public class CrcEntryBlockEntity extends MultiblockPartBlockEntity {

    /* The socket as a member of a group: the terminal reads which group, so it keeps nothing it hears. */
    private static final LineGroups.IGroupMember MEMBER = new LineGroups.IGroupMember() {
        @Override
        public void joined(final LineGroup group) {
            // The terminal reads its group once a second.
        }

        @Override
        public void left(final GridKind kind) {
            // As above.
        }
    };

    public CrcEntryBlockEntity(final BlockPos pos, final BlockState state) {
        super(CrcContent.ENTRY_BE.get(), pos, state);
    }

    /** Lets go of its terminal, and draws itself again: the structure came apart. */
    @Override
    public void release() {
        super.release();
        final BlockState state = getBlockState();
        if (this.level != null && !this.level.isClientSide() && state.getBlock() instanceof CrcBlock
                && state.getValue(CrcBlock.FORMED)) {
            this.level.setBlock(this.worldPosition, state.setValue(CrcBlock.FORMED, false), Block.UPDATE_ALL);
        }
    }

    /** What the Research Link Cable meets on {@code side}: a member on a Link Socket's front, nothing elsewhere. */
    public @Nullable LineGroups.IGroupMember member(final @Nullable Direction side) {
        final BlockState state = getBlockState();
        return state.is(CrcContent.LINK_SOCKET.get()) && (side == null
                || side == state.getValue(HorizontalDirectionalBlock.FACING)) ? MEMBER : null;
    }
}
