/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.List;

/**
 * How each shape lays itself out to be shown: a drawn pattern as drawn, its slice there as many times as asked; a box,
 * a floor and walls, and a cuboid at their least size, grown by as many blocks as asked. A place that takes air alone
 * is one that must be left empty, kept apart. Pure.
 */
final class ShapePreviews {

    private ShapePreviews() {
    }

    /**
     * {@code pattern} standing {@code orientation}, its repeated slice there its least times and {@code grow} more,
     * meeting the ground where it says, upright; its bottom laid down.
     */
    static ShapePreview pattern(final MultiblockPattern pattern, final Orientation orientation, final int grow) {
        final int times = pattern.repeat().map(r -> clamp(r.times().least() + grow, r.times())).orElse(1);
        final MultiblockPattern.Grid grid = pattern.grown(times);
        final char insideLetter = pattern.inside().map(MultiblockPattern.Inside::letter)
                .orElse(MultiblockPattern.IGNORE_CHAR);
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        final List<ShapePreview.Ghost> clears = new ArrayList<>();
        final char[][][] cells = grid.cells();
        for (int y = 0; y < cells.length; y++) {
            for (int z = 0; z < cells[y].length; z++) {
                for (int x = 0; x < cells[y][z].length; x++) {
                    final char letter = cells[y][z][x];
                    if (letter == MultiblockPattern.IGNORE_CHAR) {
                        continue;
                    }
                    final int[] w = orientation.apply(x - grid.terminalX(), y - grid.terminalY(),
                            z - grid.terminalZ());
                    final ShapePreview.Ghost ghost = letter == MultiblockPattern.TERMINAL_CHAR
                            ? ShapePreview.Ghost.terminalAt(w[0], w[1], w[2])
                            : ShapePreview.Ghost.of(w[0], w[1], w[2], letter == insideLetter
                                    ? pattern.inside().orElseThrow().rule().allows()
                                    : pattern.places().get(letter).fits());
                    keep(ghost, ghosts, clears);
                }
            }
        }
        // The ground is counted in the layers as drawn, so it stays as far from the terminal however the slice grows.
        final int ground = orientation.upright() ? pattern.ground() - pattern.terminalY() : Integer.MIN_VALUE;
        final ShapePreview laid = ShapePreview.of(ghosts, clears, ground);
        return pattern.terminalBlock().map(laid::withTerminal).orElse(laid);
    }

    /** {@code box} at its least sides and height, {@code grow} more each, its terminal amid its north face. */
    static ShapePreview box(final BoxShape box, final int grow) {
        final int side = clamp(box.sides().least() + grow, box.sides());
        final int high = clamp(box.height().least() + grow, box.height());
        final int cx = side / 2;
        final int cy = high / 2;
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        final List<ShapePreview.Ghost> clears = new ArrayList<>();
        for (int y = 0; y < high; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    final int bounds = (x == 0 || x == side - 1 ? 1 : 0) + (y == 0 || y == high - 1 ? 1 : 0)
                            + (z == 0 || z == side - 1 ? 1 : 0);
                    final ShapePreview.Ghost ghost = x == cx && y == cy && z == 0
                            ? ShapePreview.Ghost.terminalAt(0, 0, 0)
                            : ShapePreview.Ghost.of(x - cx, y - cy, z, bounds >= 2 ? box.frame().fits()
                                    : bounds == 1 ? box.face().fits() : box.inside().allows());
                    keep(ghost, ghosts, clears);
                }
            }
        }
        return ShapePreview.of(ghosts, clears, Integer.MIN_VALUE);
    }

    /**
     * {@code walls} round a floor as near a square as its area lets, of at least its least area, {@code grow} wider,
     * and never more than its most; its walls at their least height, the corners left out, and its terminal amid the
     * north wall's bottom row.
     */
    static ShapePreview walls(final WallsShape walls, final int grow) {
        int side = 1;
        while (side * side < walls.area().least()) {
            side++;
        }
        final int[] floor = floorOf(Math.min(side + grow, RuleShape.MOST_SIDE - 2), walls.area());
        final int wide = floor[0];
        final int deep = floor[1];
        final int high = walls.height().least();
        final int cx = wide / 2;
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        final List<ShapePreview.Ghost> clears = new ArrayList<>();
        for (int z = 0; z < deep; z++) {
            for (int x = 0; x < wide; x++) {
                ghosts.add(ShapePreview.Ghost.of(x - cx, -1, z + 1, walls.floor().fits()));
                for (int y = 0; y < high; y++) {
                    keep(ShapePreview.Ghost.of(x - cx, y, z + 1, walls.inside().allows()), ghosts, clears);
                }
            }
        }
        for (int y = 0; y < high; y++) {
            for (int along = 0; along < wide; along++) {
                if (along != cx || y != 0) {
                    ghosts.add(ShapePreview.Ghost.of(along - cx, y, 0, walls.wall().fits()));
                }
                ghosts.add(ShapePreview.Ghost.of(along - cx, y, deep + 1, walls.wall().fits()));
            }
            for (int along = 0; along < deep; along++) {
                ghosts.add(ShapePreview.Ghost.of(-cx - 1, y, along + 1, walls.wall().fits()));
                ghosts.add(ShapePreview.Ghost.of(wide - cx, y, along + 1, walls.wall().fits()));
            }
        }
        ghosts.add(ShapePreview.Ghost.terminalAt(0, 0, 0));
        return ShapePreview.of(ghosts, clears, Integer.MIN_VALUE);
    }

    /** {@code cuboid} as a cube of its least side, {@code grow} more, its terminal at its lowest corner. */
    static ShapePreview cuboid(final CuboidShape cuboid, final int grow) {
        final int side = clamp(cuboid.sides().least() + grow, cuboid.sides());
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    ghosts.add(x == 0 && y == 0 && z == 0 ? ShapePreview.Ghost.terminalAt(0, 0, 0)
                            : ShapePreview.Ghost.of(x, y, z, cuboid.unit().fits()));
                }
            }
        }
        return ShapePreview.of(ghosts);
    }

    /* Keeps {@code ghost} where it belongs: among the places that must be left empty, among the others, or neither. */
    private static void keep(final ShapePreview.Ghost ghost, final List<ShapePreview.Ghost> ghosts,
                             final List<ShapePreview.Ghost> clears) {
        if (ghost.clear()) {
            clears.add(ghost);
        } else if (!ghost.empty()) {
            ghosts.add(ghost);
        }
    }

    /*
     * A floor's width and depth for walls of {@code area}: the widest no wider than {@code wide}, and of it the
     * deepest no deeper than it is wide, whose places are within the area; a square of {@code wide} when none is.
     */
    private static int[] floorOf(final int wide, final Limit area) {
        for (int w = wide; w >= 1; w--) {
            for (int d = w; d >= 1; d--) {
                if (area.contains(w * d)) {
                    return new int[] {w, d};
                }
            }
        }
        return new int[] {wide, wide};
    }

    private static int clamp(final int value, final Limit limit) {
        return Math.max(limit.least(), Math.min(limit.most(), value));
    }
}
