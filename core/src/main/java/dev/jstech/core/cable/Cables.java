/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.GridPlaces;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Reading cable blocks from the blocks around them: which wires reach a device's face, the number each wire is known by
 * in its grid, and the blocks a set of those numbers stand in.
 */
public final class Cables {

    private Cables() {
    }

    /**
     * The wires of the cable block beside the block at {@code pos}, beyond its {@code face}, that cross into it: those
     * that join it, as the cable block worked out from what the block takes on that face. None when no cable block
     * stands there.
     */
    public static List<Wire> reaching(final BlockGetter level, final BlockPos pos, final Direction face) {
        return level.getBlockEntity(pos.relative(face)) instanceof CableBlockEntity cable
                ? cable.wiresThrough(face.getOpposite())
                : List.of();
    }

    /** The cable block at {@code pos}, or null when none stands there. */
    public static @Nullable CableBlockEntity at(final BlockGetter level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CableBlockEntity cable ? cable : null;
    }

    /** Whether the block at {@code pos} is a cable block holding a wire of {@code type}. */
    public static boolean holds(final BlockGetter level, final BlockPos pos, final CableType type) {
        return level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.holds(type);
    }

    /** The number the wire of {@code type} in the cable block at {@code pos} is known by in its grid, if it is in. */
    public static OptionalLong number(final ServerLevel level, final BlockPos pos, final CableType type) {
        return CoreGrids.places(level).find(GridPlace.wire(pos.asLong(), Wire.of(type).slot().id()));
    }

    /** The blocks {@code numbers} stand in, each once, in the order first met. */
    public static Set<BlockPos> blocksOf(final ServerLevel level, final Collection<Long> numbers) {
        final GridPlaces places = CoreGrids.places(level);
        final Set<BlockPos> blocks = new LinkedHashSet<>();
        for (final long number : numbers) {
            final GridPlace place = places.place(number);
            if (place != null) {
                blocks.add(BlockPos.of(place.pos()));
            }
        }
        return blocks;
    }

    /** The block a number stands in, or null when it stands for no place. */
    public static @Nullable BlockPos blockOf(final ServerLevel level, final long number) {
        final GridPlace place = CoreGrids.places(level).place(number);
        return place == null ? null : BlockPos.of(place.pos());
    }

    /** Lays a wire of {@code type} at {@code pos}, in the cable block there or a new one; false when it could not. */
    public static boolean lay(final Level level, final BlockPos pos, final CableType type) {
        return CableItem.lay(level, pos, type);
    }
}
