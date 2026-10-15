/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerLevel;

/**
 * Where a dimension's fluid grid is found, which moves its fluids after the machines have ticked, with every other grid
 * that shares what it carries.
 */
public final class FluidGrids {

    private FluidGrids() {
    }

    /** How the Core's fluid grid of {@code level} moves fluids. */
    public static LevelFluids of(final ServerLevel level) {
        return (LevelFluids) CoreGrids.mover(level, GridKind.FLUID).orElseThrow();
    }

    /** How many millibuckets the part of the fluid grid the pipe at {@code place} is in moved in the last tick. */
    public static OptionalLong movedLastTickAt(final ServerLevel level, final GridPlace place) {
        final OptionalLong number = CoreGrids.places(level).find(place);
        return number.isEmpty() ? OptionalLong.empty() : of(level).movedLastTickOf(number.getAsLong());
    }
}
