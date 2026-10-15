/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.FlowTrace;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.GridPlaces;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a line's mover shows of what flows along it, in one dimension: a trace for each part of the line, and, at the
 * end of each window of ticks, every part's flows told to the cable blocks ({@link WireFlows}).
 */
public final class LineFlows {

    private Set<Long> shown = Set.of();
    private long ticks;

    /** The trace of {@code part} of {@code grid}, made as the mover works its parts out again. */
    public static FlowTrace traceOf(final ServerLevel level, final Grid grid, final CableParts.Part part) {
        final GridPlaces places = CoreGrids.places(level);
        final Set<Long> numbers = new HashSet<>();
        for (final CableParts.WireAt wire : part.wires()) {
            numbers.add(wire.number());
        }
        return new FlowTrace(numbers, grid::neighbours, number -> {
            final GridPlace place = places.place(number);
            return place == null ? BlockPos.ZERO : BlockPos.of(place.pos());
        });
    }

    /** Counts a tick; at the end of each window, shows what {@code traces} saw on their cable blocks. */
    public void tick(final ServerLevel level, final List<FlowTrace> traces) {
        this.ticks++;
        if (this.ticks % FlowTrace.WINDOW != 0) {
            return;
        }
        final Map<Long, FlowTrace.Flow> window = new HashMap<>();
        for (final FlowTrace trace : traces) {
            window.putAll(trace.read());
        }
        this.shown = WireFlows.show(level, window, this.shown);
    }
}
