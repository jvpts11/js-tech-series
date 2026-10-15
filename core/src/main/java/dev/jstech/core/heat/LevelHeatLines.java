/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.cable.CableParts;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.IGridMover;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The heat carried along heat pipes in one dimension. A heat pipe carries heat from end to end almost whatever its
 * length, so each connected part of the grid is one node that holds no heat, joined to every block it plugs into by
 * the narrowest pipe's conductance, its cable's throughput read as watts a kelvin. Each tick the node stands at the
 * temperature where what flows in equals what flows out, and each end gives or takes accordingly: nothing is made or
 * lost, and no end is carried past the node's temperature.
 */
public final class LevelHeatLines implements IGridMover {

    private final GridKind kind;
    private final List<Part> parts = new ArrayList<>();
    private long builtVersion = -1L;
    private long taps;
    private long builtTaps = -1L;

    public LevelHeatLines(final GridKind kind) {
        this.kind = kind;
    }

    @Override
    public void tapsChanged() {
        this.taps++;
    }

    /** Carries the tick's heat along every part of the grid of {@code level}. */
    @Override
    public void tick(final ServerLevel level) {
        final Grid grid = CoreGrids.of(level, this.kind);
        if (grid.size() == 0) {
            this.parts.clear();
            return;
        }
        if (grid.version() != this.builtVersion || this.taps != this.builtTaps) {
            build(level, grid);
            this.builtVersion = grid.version();
            this.builtTaps = this.taps;
        }
        for (final Part part : this.parts) {
            carry(level, part);
        }
    }

    /* Moves a tick of heat between the ends of one part through its node. */
    private static void carry(final ServerLevel level, final Part part) {
        final List<IHeatPort> ports = new ArrayList<>(part.ends().size());
        double weighted = 0.0;
        for (final End end : part.ends()) {
            final IHeatPort port = level.isLoaded(end.at()) ? HeatPorts.at(level, end.at(), end.face()) : null;
            if (port == null || Double.isNaN(port.temperature())) {
                continue;
            }
            ports.add(port);
            weighted += port.temperature();
        }
        if (ports.size() < 2) {
            return;
        }
        final double node = weighted / ports.size();
        final double conductance = part.conductance();
        // A common factor keeps every end from passing the node, so what flows in still equals what flows out.
        double factor = 1.0;
        final double[] flows = new double[ports.size()];
        for (int i = 0; i < flows.length; i++) {
            final IHeatPort port = ports.get(i);
            flows[i] = conductance * (port.temperature() - node) * HeatMath.TICK_SECONDS;
            final double most = Math.abs(port.temperature() - node) * port.capacity();
            if (Math.abs(flows[i]) > most && flows[i] != 0.0) {
                factor = Math.min(factor, most / Math.abs(flows[i]));
            }
        }
        for (int i = 0; i < flows.length; i++) {
            if (flows[i] != 0.0) {
                ports.get(i).exchange(-flows[i] * factor);
            }
        }
    }

    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        for (final CableParts.Part found : CableParts.of(level, grid)) {
            long narrowest = Long.MAX_VALUE;
            for (final CableParts.WireAt wire : found.wires()) {
                narrowest = Math.min(narrowest, wire.runTooLong() ? 0L : wire.type().throughput());
            }
            final Set<String> seen = new HashSet<>();
            final List<End> ends = new ArrayList<>();
            for (final CableParts.Plug plug : found.plugs()) {
                if (plug.end().mode() != EndMode.OFF && seen.add(plug.at().asLong() + "/" + plug.face())) {
                    ends.add(new End(plug.at(), plug.face()));
                }
            }
            if (narrowest > 0L && narrowest != Long.MAX_VALUE && ends.size() >= 2) {
                this.parts.add(new Part(narrowest, ends));
            }
        }
    }

    /* One connected part: the conductance of its narrowest pipe, in watts a kelvin, and the blocks it plugs into. */
    private record Part(double conductance, List<End> ends) {
    }

    /* A block a part plugs into, and the face it meets. */
    private record End(BlockPos at, Direction face) {
    }
}
