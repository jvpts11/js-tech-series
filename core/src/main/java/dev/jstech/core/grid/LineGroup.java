/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;

/**
 * One group of a line that groups: the machines one connected run of it joins, and its colour, when its wires are dyed.
 * Two groups of the same machines and colour are the same group.
 *
 * @param kind    the line's grid
 * @param colour  the colour of its dyed wires, or {@link GridMember#NO_COLOUR}
 * @param members the machines it joins, in the order of their positions
 */
public record LineGroup(GridKind kind, int colour, List<BlockPos> members) {

    public LineGroup {
        Objects.requireNonNull(kind, "kind");
        members = List.copyOf(members);
    }

    /** The colour of its dyed wires, if any. */
    public Optional<DyeColor> dye() {
        return this.colour == GridMember.NO_COLOUR ? Optional.empty() : Optional.of(DyeColor.byId(this.colour));
    }

    /** Whether it joins the machine at {@code pos}. */
    public boolean has(final BlockPos pos) {
        return this.members.contains(pos);
    }
}
