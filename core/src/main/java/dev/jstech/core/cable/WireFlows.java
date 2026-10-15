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
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.GridPlaces;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Tells the cable blocks what their wires carried over a window of a line's flow ({@link FlowTrace}), so the players
 * who see them see it: each wire named gets its flow, and each wire that showed one and carried nothing since is
 * emptied. A block is only told what changed, so a steady flow sends nothing.
 */
public final class WireFlows {

    private WireFlows() {
    }

    /**
     * Shows {@code window}, the flows of a part's wires by number, on their cable blocks in {@code level}, emptying
     * those of {@code shown} that carried nothing; the wires that show a flow now.
     */
    public static Set<Long> show(final ServerLevel level, final Map<Long, FlowTrace.Flow> window,
                                 final Set<Long> shown) {
        final GridPlaces places = CoreGrids.places(level);
        final Set<Long> now = new HashSet<>();
        for (final Map.Entry<Long, FlowTrace.Flow> each : window.entrySet()) {
            final CableBlockEntity cable = cableOf(level, places, each.getKey());
            final GridPlace place = places.place(each.getKey());
            if (cable == null || place == null) {
                continue;
            }
            final Lane lane = Lane.byId(place.lane());
            final Wire wire = cable.wireIn(lane);
            if (wire == null) {
                continue;
            }
            final FlowTrace.Flow flow = each.getValue();
            cable.showFlow(lane, WireFlow.of(flow.what(), flow.toward(), flow.perTick(), wire.type().throughput()));
            now.add(each.getKey());
        }
        for (final long gone : shown) {
            if (now.contains(gone)) {
                continue;
            }
            final CableBlockEntity cable = cableOf(level, places, gone);
            final GridPlace place = places.place(gone);
            if (cable != null && place != null) {
                cable.showFlow(Lane.byId(place.lane()), WireFlow.EMPTY);
            }
        }
        return now;
    }

    private static @Nullable CableBlockEntity cableOf(final ServerLevel level, final GridPlaces places,
                                                      final long wire) {
        final GridPlace place = places.place(wire);
        if (place == null || place.lane() == GridPlace.WHOLE) {
            return null;
        }
        final BlockPos pos = BlockPos.of(place.pos());
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CableBlockEntity cable ? cable : null;
    }
}
