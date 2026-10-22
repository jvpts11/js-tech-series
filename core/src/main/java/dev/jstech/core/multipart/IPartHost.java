/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** The block space a part is mounted in, as the part sees it: a cable block, or a space of parts alone. */
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

    /**
     * The redstone or the comparator reading a part on {@code face} gives changed: the block's neighbours, and the
     * block on that face, which the part may power through, hear of it.
     */
    default void partSignalChanged(final Direction face) {
        final ServerLevel level = partServerLevel();
        if (level == null) {
            return;
        }
        final BlockPos pos = partPos();
        final Block block = level.getBlockState(pos).getBlock();
        level.updateNeighborsAt(pos, block);
        level.updateNeighborsAt(pos.relative(face), block);
        level.updateNeighbourForOutputSignal(pos, block);
    }

    /** The server level the block is in, or null on a player's game or before it is placed. */
    default @Nullable ServerLevel partServerLevel() {
        return partLevel() instanceof ServerLevel server ? server : null;
    }

    /**
     * The room what runs through the space's middle takes, a cable's wires, in pixels: no part but one against a face
     * may share it, and that one only at the end that runs out of its face. Nothing, for a space with nothing in its
     * middle.
     */
    default List<PartBox> middle() {
        return List.of();
    }

    /**
     * Whether a facade may stand on its faces: on any space's, so a microblock's look covers any face of a space; on a
     * cable's it hides the wires and leaves the links open. A host whose faces must stay bare says no.
     */
    default boolean takesFacades() {
        return true;
    }
}
