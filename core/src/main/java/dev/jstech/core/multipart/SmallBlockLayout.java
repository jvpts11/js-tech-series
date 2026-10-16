/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * The game's small blocks a block space draws, a torch or a lever, which its block entity hands its model through its
 * model data under {@link #PROPERTY}; the model draws each with the game's own model of its state, into the world's
 * mesh, built again only when a part changes.
 *
 * @param states the small blocks, each in the state it stands in
 */
public record SmallBlockLayout(List<BlockState> states) {

    /** Where a space's model finds its small blocks. */
    public static final ModelProperty<SmallBlockLayout> PROPERTY = new ModelProperty<>();
    /** A space with no small blocks. */
    public static final SmallBlockLayout EMPTY = new SmallBlockLayout(List.of());

    public SmallBlockLayout {
        states = List.copyOf(states);
    }

    public boolean isEmpty() {
        return this.states.isEmpty();
    }

    /** What {@code parts} draw of their small blocks. */
    public static SmallBlockLayout of(final SpaceParts parts) {
        final List<BlockState> states = parts.smallBlocks();
        return states.isEmpty() ? EMPTY : new SmallBlockLayout(states);
    }
}
