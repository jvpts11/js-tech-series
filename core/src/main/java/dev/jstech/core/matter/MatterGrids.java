/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerLevel;

/**
 * Where a dimension's grid of a state of matter is found, which moves what its lines carry after the machines have
 * ticked, with every other grid that shares what it carries.
 */
public final class MatterGrids {

    private MatterGrids() {
    }

    /** How the grid of {@code kind} moves its state of matter in {@code level}, or none for a grid of another kind. */
    public static Optional<LevelMatter> of(final ServerLevel level, final GridKind kind) {
        return CoreGrids.mover(level, kind).filter(LevelMatter.class::isInstance).map(LevelMatter.class::cast);
    }

    /** How many millibuckets the part of the grid of {@code kind} the line at {@code place} is in moved last tick. */
    public static OptionalLong movedLastTickAt(final ServerLevel level, final GridKind kind, final GridPlace place) {
        final OptionalLong number = CoreGrids.places(level).find(place);
        final Optional<LevelMatter> grid = of(level, kind);
        return number.isEmpty() || grid.isEmpty() ? OptionalLong.empty()
                : grid.get().movedLastTickOf(number.getAsLong());
    }
}
