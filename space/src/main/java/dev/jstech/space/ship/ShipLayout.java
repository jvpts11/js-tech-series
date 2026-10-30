/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.space.api.ShipKit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where every block of a starship goes, laid from its parts' kits in the ship's own frame ({@link ShipKit}): the
 * spine's sections one behind the other from the nose, each from its own front, and the pieces of the parts hung on it
 * from their anchors, a mirrored piece set on both sides of the centre line. Where two parts want the same place they
 * clash. The parts are counted in order, the spine's from the nose first, then the hung ones.
 */
public final class ShipLayout {

    private ShipLayout() {
    }

    /** The blocks of a ship of the spine {@code spine} and the parts {@code hung}, each hung part its pieces. */
    public static Laid lay(final List<Section> spine, final List<List<ShipKit.Piece>> hung) {
        final int[] fronts = fronts(spine.stream().map(Section::length).toList());
        final Map<Cell, Spot> cells = new LinkedHashMap<>();
        final Set<Cell> clashes = new HashSet<>();
        int part = 0;
        for (int k = 0; k < spine.size(); k++) {
            for (final ShipKit.Piece piece : spine.get(k).pieces()) {
                set(piece, anchor(piece.anchor(), fronts, k), part, cells, clashes);
            }
            part++;
        }
        for (final List<ShipKit.Piece> pieces : hung) {
            for (final ShipKit.Piece piece : pieces) {
                set(piece, anchor(piece.anchor(), fronts, 0), part, cells, clashes);
            }
            part++;
        }
        return new Laid(cells, clashes, fronts[spine.size()]);
    }

    /** Where each section of a spine of {@code lengths} begins from the nose, and last where the spine ends. */
    public static int[] fronts(final List<Integer> lengths) {
        final int[] fronts = new int[lengths.size() + 1];
        for (int k = 0; k < lengths.size(); k++) {
            fronts[k + 1] = fronts[k] + lengths.get(k);
        }
        return fronts;
    }

    /**
     * Where along a spine whose sections begin at {@code fronts} the anchor {@code anchor} stands; a section's own
     * front is that of its section {@code section}, the nose for a part hung on the spine.
     */
    public static int anchor(final ShipKit.Anchor anchor, final int[] fronts, final int section) {
        final int sections = fronts.length - 1;
        return switch (anchor) {
            case SECTION -> fronts[Math.min(section, sections)];
            case COCKPIT_JOINT -> fronts[Math.min(1, sections)];
            case WING_ROOT -> fronts[sections > 2 ? 2 : Math.min(1, sections)];
            case SPINE_END -> fronts[sections];
        };
    }

    /* Sets the blocks of {@code piece} from {@code z0} along the spine, on both sides if it is mirrored. */
    private static void set(final ShipKit.Piece piece, final int z0, final int part, final Map<Cell, Spot> cells,
                            final Set<Cell> clashes) {
        for (int y = 0; y < piece.layers().size(); y++) {
            final List<String> layer = piece.layers().get(y);
            for (int z = 0; z < layer.size(); z++) {
                final String row = layer.get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    if (letter == ' ') {
                        continue;
                    }
                    final int at = piece.x() + x;
                    put(new Cell(at, piece.y() + y, z0 + piece.z() + z), new Spot(part, letter), cells, clashes);
                    if (piece.mirrored()) {
                        put(new Cell(-1 - at, piece.y() + y, z0 + piece.z() + z), new Spot(part, letter), cells,
                                clashes);
                    }
                }
            }
        }
    }

    private static void put(final Cell cell, final Spot spot, final Map<Cell, Spot> cells, final Set<Cell> clashes) {
        final Spot was = cells.putIfAbsent(cell, spot);
        if (was != null && !was.equals(spot)) {
            clashes.add(cell);
        }
    }

    /**
     * A section of the spine.
     *
     * @param length its length along the spine, in blocks
     * @param pieces its kit's pieces
     */
    public record Section(int length, List<ShipKit.Piece> pieces) {

        public Section {
            pieces = List.copyOf(pieces);
        }
    }

    /** A place on the ship, in its own frame. */
    public record Cell(int x, int y, int z) {
    }

    /**
     * A block laid: the part it is of and the letter of its kit.
     *
     * @param part   the part, the spine's from the nose first, then the hung ones
     * @param letter its letter in the part's kit
     */
    public record Spot(int part, char letter) {
    }

    /**
     * A ship laid.
     *
     * @param cells   every block, by its place
     * @param clashes the places two parts want
     * @param length  its spine's length
     */
    public record Laid(Map<Cell, Spot> cells, Set<Cell> clashes, int length) {

        public Laid {
            cells = Map.copyOf(cells);
            clashes = Set.copyOf(clashes);
        }
    }
}
