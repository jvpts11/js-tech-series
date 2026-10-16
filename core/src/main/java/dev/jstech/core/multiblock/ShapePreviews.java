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
 * a floor and walls, and a cuboid at their least size, grown by as many blocks as asked. Pure.
 */
final class ShapePreviews {

    private ShapePreviews() {
    }

    /** {@code pattern} standing {@code orientation}, its repeated slice there its least times and {@code grow} more. */
    static ShapePreview pattern(final MultiblockPattern pattern, final Orientation orientation, final int grow) {
        final int times = pattern.repeat().map(r -> clamp(r.times().least() + grow, r.times())).orElse(1);
        final MultiblockPattern.Grid grid = pattern.grown(times);
        final char insideLetter = pattern.inside().map(MultiblockPattern.Inside::letter)
                .orElse(MultiblockPattern.IGNORE_CHAR);
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        final char[][][] cells = grid.cells();
        for (int y = 0; y < cells.length; y++) {
            for (int z = 0; z < cells[y].length; z++) {
                for (int x = 0; x < cells[y][z].length; x++) {
                    final char letter = cells[y][z][x];
                    if (letter == MultiblockPattern.IGNORE_CHAR) {
                        continue;
                    }
                    final int[] w = orientation.apply(x - grid.controllerX(), y - grid.controllerY(),
                            z - grid.controllerZ());
                    final ShapePreview.Ghost ghost = letter == MultiblockPattern.CONTROLLER_CHAR
                            ? ShapePreview.Ghost.controllerAt(w[0], w[1], w[2])
                            : ShapePreview.Ghost.of(w[0], w[1], w[2], letter == insideLetter
                                    ? pattern.inside().orElseThrow().rule().allows()
                                    : pattern.places().get(letter).fits());
                    if (!ghost.empty()) {
                        ghosts.add(ghost);
                    }
                }
            }
        }
        final ShapePreview laid = ShapePreview.of(ghosts);
        return pattern.controllerBlock().map(laid::withController).orElse(laid);
    }

    /** {@code box} at its least sides and height, {@code grow} more each, its controller amid its north face. */
    static ShapePreview box(final BoxShape box, final int grow) {
        final int side = clamp(box.sides().least() + grow, box.sides());
        final int high = clamp(box.height().least() + grow, box.height());
        final int cx = side / 2;
        final int cy = high / 2;
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        for (int y = 0; y < high; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    final int bounds = (x == 0 || x == side - 1 ? 1 : 0) + (y == 0 || y == high - 1 ? 1 : 0)
                            + (z == 0 || z == side - 1 ? 1 : 0);
                    final ShapePreview.Ghost ghost = x == cx && y == cy && z == 0
                            ? ShapePreview.Ghost.controllerAt(0, 0, 0)
                            : ShapePreview.Ghost.of(x - cx, y - cy, z, bounds >= 2 ? box.frame().fits()
                                    : bounds == 1 ? box.face().fits() : box.inside().allows());
                    if (!ghost.empty()) {
                        ghosts.add(ghost);
                    }
                }
            }
        }
        return ShapePreview.of(ghosts);
    }

    /**
     * {@code walls} round a square floor of at least its least area, {@code grow} wider, its walls at their least
     * height, the corners left out, and its controller amid the north wall's bottom row.
     */
    static ShapePreview walls(final WallsShape walls, final int grow) {
        int side = 1;
        while (side * side < walls.area().least()) {
            side++;
        }
        side = Math.min(side + grow, RuleShape.MOST_SIDE - 2);
        final int high = walls.height().least();
        final int cx = side / 2;
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        for (int z = 0; z < side; z++) {
            for (int x = 0; x < side; x++) {
                ghosts.add(ShapePreview.Ghost.of(x - cx, -1, z + 1, walls.floor().fits()));
                for (int y = 0; y < high; y++) {
                    final ShapePreview.Ghost inside = ShapePreview.Ghost.of(x - cx, y, z + 1,
                            walls.inside().allows());
                    if (!inside.empty()) {
                        ghosts.add(inside);
                    }
                }
            }
        }
        for (int y = 0; y < high; y++) {
            for (int along = 0; along < side; along++) {
                if (along != cx || y != 0) {
                    ghosts.add(ShapePreview.Ghost.of(along - cx, y, 0, walls.wall().fits()));
                }
                ghosts.add(ShapePreview.Ghost.of(along - cx, y, side + 1, walls.wall().fits()));
                ghosts.add(ShapePreview.Ghost.of(-cx - 1, y, along + 1, walls.wall().fits()));
                ghosts.add(ShapePreview.Ghost.of(side - cx, y, along + 1, walls.wall().fits()));
            }
        }
        ghosts.add(ShapePreview.Ghost.controllerAt(0, 0, 0));
        return ShapePreview.of(ghosts);
    }

    /** {@code cuboid} as a cube of its least side, {@code grow} more, its controller at its lowest corner. */
    static ShapePreview cuboid(final CuboidShape cuboid, final int grow) {
        final int side = clamp(cuboid.sides().least() + grow, cuboid.sides());
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    ghosts.add(x == 0 && y == 0 && z == 0 ? ShapePreview.Ghost.controllerAt(0, 0, 0)
                            : ShapePreview.Ghost.of(x, y, z, cuboid.unit().fits()));
                }
            }
        }
        return ShapePreview.of(ghosts);
    }

    private static int clamp(final int value, final Limit limit) {
        return Math.max(limit.least(), Math.min(limit.most(), value));
    }
}
