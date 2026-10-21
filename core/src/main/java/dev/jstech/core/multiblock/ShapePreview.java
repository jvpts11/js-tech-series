/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * A shape laid out as it stands, for showing it before it is built: in the world as ghosts, in a screen layer by
 * layer, in a recipe viewer with its materials. Each place has its offset from the controller, its layer from the
 * bottom, and the blocks and tags that go there; the controller's place is marked, and places left empty (air
 * inside a tank) are not in it. Pure.
 *
 * @param ghosts the places, the bottom layer first
 * @param minX   the least offset from the controller along x of any place, the controller's counted in
 * @param minY   along y
 * @param minZ   along z
 * @param sizeX  its size along x, in blocks
 * @param sizeY  its height, which is how many layers it has
 * @param sizeZ  its size along z
 */
public record ShapePreview(List<Ghost> ghosts, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {

    public ShapePreview {
        ghosts = List.copyOf(ghosts);
    }

    /**
     * Lays out {@code places}, each an offset from the controller with what goes there, the controller's included:
     * also blocks that are no multiblock, such as a manual's scene shown in the world, with no controller among them.
     */
    public static ShapePreview of(final List<Ghost> places) {
        final int[] lo = {0, 0, 0};
        final int[] hi = {0, 0, 0};
        for (final Ghost ghost : places) {
            final int[] at = {ghost.x(), ghost.y(), ghost.z()};
            for (int axis = 0; axis < 3; axis++) {
                lo[axis] = Math.min(lo[axis], at[axis]);
                hi[axis] = Math.max(hi[axis], at[axis]);
            }
        }
        final List<Ghost> layered = new ArrayList<>();
        for (final Ghost ghost : places) {
            layered.add(new Ghost(ghost.x(), ghost.y(), ghost.z(), ghost.y() - lo[1], ghost.blocks(), ghost.tags(),
                    ghost.controller()));
        }
        layered.sort((a, b) -> a.layer() != b.layer() ? Integer.compare(a.layer(), b.layer())
                : a.z() != b.z() ? Integer.compare(a.z(), b.z()) : Integer.compare(a.x(), b.x()));
        return new ShapePreview(layered, lo[0], lo[1], lo[2], hi[0] - lo[0] + 1, hi[1] - lo[1] + 1,
                hi[2] - lo[2] + 1);
    }

    /** The same standing {@code orientation}: each place turned, mirrored or laid down round the controller. */
    public ShapePreview turned(final Orientation orientation) {
        final List<Ghost> moved = new ArrayList<>();
        for (final Ghost ghost : this.ghosts) {
            final int[] w = orientation.apply(ghost.x(), ghost.y(), ghost.z());
            moved.add(new Ghost(w[0], w[1], w[2], 0, ghost.blocks(), ghost.tags(), ghost.controller()));
        }
        return of(moved);
    }

    /**
     * Where its controller stands when it is shown in front of something at {@code x}, {@code y}, {@code z} that
     * faces along axis {@code axis} (0 for x, 2 for z) the way {@code sign} says: its near side one block in front,
     * its middle across from it, its bottom level with it.
     */
    public int[] anchor(final int x, final int y, final int z, final int axis, final int sign) {
        final int[] at = {x, y, z};
        final int[] lo = {this.minX, this.minY, this.minZ};
        final int[] hi = {this.minX + this.sizeX - 1, this.minY + this.sizeY - 1, this.minZ + this.sizeZ - 1};
        final int[] controller = new int[3];
        final int across = axis == 0 ? 2 : 0;
        controller[axis] = at[axis] + sign - (sign > 0 ? lo[axis] : hi[axis]);
        controller[across] = at[across] - Math.floorDiv(lo[across] + hi[across], 2);
        controller[1] = at[1] - lo[1];
        return controller;
    }

    /**
     * The same with {@code block}, by id, at its controller's place, when the shape named none there: the block a
     * machine's controller is, known to whoever declared the machine rather than to its shape.
     */
    public ShapePreview withController(final String block) {
        final List<Ghost> named = new ArrayList<>();
        for (final Ghost ghost : this.ghosts) {
            named.add(ghost.controller() && ghost.blocks().isEmpty() ? new Ghost(ghost.x(), ghost.y(), ghost.z(),
                    ghost.layer(), List.of(block), List.of(), true) : ghost);
        }
        return new ShapePreview(named, this.minX, this.minY, this.minZ, this.sizeX, this.sizeY, this.sizeZ);
    }

    /** The id of the block at its controller's place, when it is known. */
    public Optional<String> controllerBlock() {
        return this.ghosts.stream().filter(Ghost::controller).findFirst().map(Ghost::first);
    }

    /**
     * The places where more than one block will do, grouped by what will do there, in the order they are first met:
     * each group's blocks and tags, air left out, and how many places take it. A place that takes one block or one
     * tag is not in it.
     */
    public List<Choice> choices() {
        final Map<List<String>, Choice> groups = new LinkedHashMap<>();
        for (final Ghost ghost : this.ghosts) {
            if (ghost.controller()) {
                continue;
            }
            final List<String> blocks = ghost.blocks().stream().filter(block -> !Cells.AIR.equals(block)).toList();
            if (blocks.size() + ghost.tags().size() < 2) {
                continue;
            }
            final List<String> key = new ArrayList<>(blocks);
            ghost.tags().forEach(tag -> key.add("#" + tag));
            groups.merge(key, new Choice(blocks, ghost.tags(), 1),
                    (known, more) -> new Choice(known.blocks(), known.tags(), known.places() + 1));
        }
        return List.copyOf(groups.values());
    }

    /** How many layers it has. */
    public int layers() {
        return this.sizeY;
    }

    /** Its places of layer {@code layer}, from 0 at the bottom. */
    public List<Ghost> layer(final int layer) {
        return this.ghosts.stream().filter(ghost -> ghost.layer() == layer).toList();
    }

    /** How many places it has, its controller's counted in. */
    public int blocks() {
        return this.ghosts.size();
    }

    /**
     * What it takes to build, but its controller: how many of each first choice of its places, by block id or, for a
     * place that names only tags, by its first tag written {@code #namespace:path}, most first.
     */
    public Map<String, Integer> materials() {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final Ghost ghost : this.ghosts) {
            if (!ghost.controller() && ghost.first() != null) {
                counts.merge(ghost.first(), 1, Integer::sum);
            }
        }
        final List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        final Map<String, Integer> out = new LinkedHashMap<>();
        sorted.forEach(entry -> out.put(entry.getKey(), entry.getValue()));
        return out;
    }

    /**
     * What will do at some of a shape's places.
     *
     * @param blocks the blocks that do, by id
     * @param tags   the tags whose blocks do too
     * @param places how many places take any of them
     */
    public record Choice(List<String> blocks, List<String> tags, int places) {

        public Choice {
            blocks = List.copyOf(blocks);
            tags = List.copyOf(tags);
        }
    }

    /**
     * A place of a shape, as it is shown.
     *
     * @param x          its offset from the controller along x, as the shape stands
     * @param y          along y
     * @param z          along z
     * @param layer      its layer, from 0 at the bottom
     * @param blocks     the blocks that go there, by id, the first the one shown
     * @param tags       the tags whose blocks go there too
     * @param controller whether it is the controller's place, which takes the controller's block
     */
    public record Ghost(int x, int y, int z, int layer, List<String> blocks, List<String> tags, boolean controller) {

        public Ghost {
            blocks = List.copyOf(blocks);
            tags = List.copyOf(tags);
        }

        /** A place where the blocks and tags of {@code fits} go, or anything when it names none it can show. */
        static Ghost of(final int x, final int y, final int z, final IBlockMatcher fits) {
            return fits instanceof BlockMatch match ? new Ghost(x, y, z, 0, match.blocks(), match.tags(), false)
                    : new Ghost(x, y, z, 0, List.of(), List.of(), false);
        }

        /** The controller's place. */
        static Ghost controllerAt(final int x, final int y, final int z) {
            return new Ghost(x, y, z, 0, List.of(), List.of(), true);
        }

        /** What is shown there: its first block, else its first tag as {@code #namespace:path}; none for neither. */
        @Nullable
        public String first() {
            if (!this.blocks.isEmpty()) {
                return this.blocks.getFirst();
            }
            return this.tags.isEmpty() ? null : "#" + this.tags.getFirst();
        }

        /**
         * Whether nothing is shown there: a place inside that takes air and nothing else, or one whose blocks are
         * chosen by code, which names none.
         */
        boolean empty() {
            return !this.controller && this.tags.isEmpty() && this.blocks.stream().allMatch(Cells.AIR::equals);
        }
    }
}
