/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.GridPlaces;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.overhead.WireAnchor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The connected parts of a grid of wires, each with its wires and the blocks they plug into: what a grid that moves
 * something (energy, fluid) works out again whenever its shape changes, and moves through every tick until then. A
 * wire whose block is not loaded is left out until it loads.
 */
public final class CableParts {

    private CableParts() {
    }

    /** The connected parts of {@code grid}, in the order of their roots, each wire in the order of its number. */
    public static List<Part> of(final ServerLevel level, final Grid grid) {
        final GridPlaces places = CoreGrids.places(level);
        final Map<Integer, List<WireAt>> wires = new TreeMap<>();
        final Map<Integer, List<Plug>> plugs = new TreeMap<>();
        for (final long number : new TreeSet<>(grid.positions())) {
            final GridPlace place = places.place(number);
            if (place == null || place.lane() == GridPlace.WHOLE) {
                continue;
            }
            if (place.isHung() || place.isAnchor()) {
                overhead(level, grid, place, number, wires, plugs);
                continue;
            }
            final BlockPos pos = BlockPos.of(place.pos());
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
                continue;
            }
            final Lane lane = Lane.byId(place.lane());
            final Wire wire = cable.wireIn(lane);
            if (wire == null) {
                continue;
            }
            final int root = grid.rootOf(number);
            wires.computeIfAbsent(root, key -> new ArrayList<>())
                    .add(new WireAt(number, wire.type(), grid.runTooLong(number), 1));
            final int plugged = cable.plugs(lane);
            for (final Direction face : Direction.values()) {
                final BlockPos at = pos.relative(face);
                // An unloaded neighbour is left out: reading its block entity would load its chunk.
                if ((plugged & 1 << face.get3DDataValue()) != 0 && level.isLoaded(at)
                        && !(level.getBlockEntity(at) instanceof CableBlockEntity)) {
                    plugs.computeIfAbsent(root, key -> new ArrayList<>())
                            .add(new Plug(number, at, face.getOpposite(), cable.end(lane, face)));
                }
            }
        }
        final List<Part> parts = new ArrayList<>(wires.size());
        for (final Map.Entry<Integer, List<WireAt>> part : wires.entrySet()) {
            parts.add(new Part(part.getKey(), part.getValue(), plugs.getOrDefault(part.getKey(), List.of())));
        }
        return parts;
    }

    /*
     * An overhead line's place in a part: a hung wire, a cable as long as itself; an insulator, a cable of no length,
     * which a connector's block is plugged into on the face the insulator meets.
     */
    private static void overhead(final ServerLevel level, final Grid grid, final GridPlace place, final long number,
                                 final Map<Integer, List<WireAt>> wires, final Map<Integer, List<Plug>> plugs) {
        final int root = grid.rootOf(number);
        if (place.isHung()) {
            OverheadLines.wireAt(level, place).ifPresent(wire -> CoreCables.REGISTRY.getOptional(wire.cable())
                    .ifPresent(type -> wires.computeIfAbsent(root, key -> new ArrayList<>())
                            .add(new WireAt(number, type, false, wire.blocks()))));
            return;
        }
        final Optional<CableType> line = OverheadLines.lineAt(level, place);
        if (line.isEmpty()) {
            return;
        }
        wires.computeIfAbsent(root, key -> new ArrayList<>()).add(new WireAt(number, line.get(), false, 0));
        OverheadLines.anchorAt(level, place).filter(anchor -> anchor.role() == WireAnchor.Role.CONNECTOR)
                .ifPresent(anchor -> plugs.computeIfAbsent(root, key -> new ArrayList<>())
                        .add(new Plug(number, BlockPos.of(place.pos()), anchor.face(), CableEnd.DEFAULT)));
    }

    /**
     * One connected part of a grid.
     *
     * @param root  the grid's root of the part
     * @param wires its wires
     * @param plugs where its wires plug into a block, wire by wire
     */
    public record Part(int root, List<WireAt> wires, List<Plug> plugs) {

        public Part {
            wires = List.copyOf(wires);
            plugs = List.copyOf(plugs);
        }
    }

    /**
     * A wire of a part.
     *
     * @param number     its number in the grid
     * @param type       its cable
     * @param runTooLong whether the run it is on is longer than its cable reaches, so it carries nothing across
     * @param blocks     how many blocks of cable it stands for, as what it loses counts them: one for a wire in a cable
     *                   block, its length for a hung wire, none for the insulator it is hung from
     */
    public record WireAt(long number, CableType type, boolean runTooLong, int blocks) {
    }

    /**
     * Where a wire plugs into a block, and how that end of it is set.
     *
     * @param wire the wire's number in the grid
     * @param at   the block it plugs into
     * @param face the face of that block it meets
     * @param end  how the end meets it
     */
    public record Plug(long wire, BlockPos at, Direction face, CableEnd end) {
    }
}
