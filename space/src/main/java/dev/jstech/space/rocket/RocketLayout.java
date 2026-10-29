/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketPart;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Where the blocks of a rocket's parts go, model by model, from the middle of the rocket's foot: each model a part is
 * drawn with laid on the blocks of its kit's layers, a block of its kit at each of its cells, turned about the rocket's
 * axis as its part was set. A tank built in layers is a model a layer, each laid on its own layer of the kit. The cells
 * of a model are its kit's letters, {@code x} from west to east, {@code z} from north to south, its layer up from its
 * foot; a part's turns turn them clockwise seen from above, as its model is drawn turned.
 */
public final class RocketLayout {

    /* A part's quarter turns, as the orientations a formed model can stand in. */
    private static final List<Orientation> TURNS = List.of(Orientation.NORTH, Orientation.EAST, Orientation.SOUTH,
            Orientation.WEST);

    private RocketLayout() {
    }

    /**
     * The models {@code stack} is drawn with, standing in the hangar, each with the cells it is laid on; a model whose
     * part or kit is not known is left out.
     */
    public static List<Laid> of(final Map<ResourceLocation, RocketPart> parts,
                                final Map<ResourceLocation, RocketKit> kits, final RocketStack stack) {
        final List<Laid> out = new ArrayList<>();
        final List<RocketPartModels.Placed> placed = RocketPartModels.of(parts, kits, stack, false);
        int layerOfPart = 0;
        int lastSection = -1;
        for (final RocketPartModels.Placed model : placed) {
            final RocketStack.Section section = stack.sections().get(model.section());
            final RocketPart part = parts.get(section.part().id());
            final RocketKit kit = kits.get(section.part().id());
            layerOfPart = model.section() == lastSection ? layerOfPart + 1 : 0;
            lastSection = model.section();
            if (part == null || kit == null) {
                continue;
            }
            final List<List<String>> layers;
            if (part.stretches()) {
                final RocketPart below = model.section() > 0 ? parts.get(stack.sections().get(model.section() - 1)
                        .part().id()) : null;
                final boolean onEngine = below != null && below.kind() == RocketPart.Kind.ENGINE;
                final Optional<List<List<String>>> built = kit.built(Math.max(1, section.part().layers()), onEngine);
                if (built.isEmpty() || layerOfPart >= built.get().size()) {
                    continue;
                }
                layers = List.of(built.get().get(layerOfPart));
            } else {
                layers = kit.layers();
            }
            final List<Cell> cells = new ArrayList<>();
            for (int layer = 0; layer < layers.size(); layer++) {
                final List<String> rows = layers.get(layer);
                for (int z = 0; z < rows.size(); z++) {
                    for (int x = 0; x < rows.get(z).length(); x++) {
                        final ResourceLocation block = kit.key().get(String.valueOf(rows.get(z).charAt(x)));
                        if (block != null) {
                            cells.add(new Cell(x, layer, z, block));
                        }
                    }
                }
            }
            out.add(new Laid(model, kit.width(), List.copyOf(cells)));
        }
        return List.copyOf(out);
    }

    /** The orientation a formed model stands in for a part turned {@code turns} quarter turns. */
    public static int orientation(final int turns) {
        return FormedShapes.ORIENTATIONS.indexOf(TURNS.get(Math.floorMod(turns, TURNS.size())));
    }

    /**
     * A model of a part, laid.
     *
     * @param placed   the model, where it stands and how it is turned
     * @param diameter how wide its kit is, in blocks
     * @param cells    the cells of its kit it is laid on, each with its block
     */
    public record Laid(RocketPartModels.Placed placed, int diameter, List<Cell> cells) {

        public Laid {
            cells = List.copyOf(cells);
        }

        /** Where its cell {@code x, layer, z} is from the middle of the rocket's foot, turned as its part is. */
        public BlockPos at(final int x, final int layer, final int z) {
            int dx = x - (this.diameter - 1) / 2;
            int dz = z - (this.diameter - 1) / 2;
            for (int t = 0; t < Math.floorMod(this.placed.turns(), TURNS.size()); t++) {
                final int was = dx;
                dx = -dz;
                dz = was;
            }
            return new BlockPos(dx, this.placed.y() + layer, dz);
        }

        /** The cell of it the place {@code offset} from the middle of the rocket's foot is, turned back. */
        public long cellOf(final BlockPos offset) {
            int dx = offset.getX();
            int dz = offset.getZ();
            for (int t = 0; t < Math.floorMod(this.placed.turns(), TURNS.size()); t++) {
                final int was = dz;
                dz = -dx;
                dx = was;
            }
            return MeshCells.key(dx + (this.diameter - 1) / 2, offset.getY() - this.placed.y(),
                    dz + (this.diameter - 1) / 2);
        }

        /** Its cells, as the keys of its formed model's blocks. */
        public Set<Long> keys() {
            final Set<Long> keys = new LinkedHashSet<>();
            for (final Cell cell : this.cells) {
                keys.add(MeshCells.key(cell.x(), cell.layer(), cell.z()));
            }
            return keys;
        }

        /** Which block of its formed model the cell {@code key} is, as its part is turned. */
        public FormedPlace place(final long key) {
            return new FormedPlace(this.placed.model(), orientation(this.placed.turns()), MeshCells.x(key),
                    MeshCells.y(key), MeshCells.z(key));
        }

        /** Where the lowest corner of its cell {@code (0, 0, 0)} is in its model's space, in blocks. */
        public double[] origin() {
            return new double[] {-this.diameter / 2.0, 0, -this.diameter / 2.0};
        }
    }

    /**
     * A cell of a model, with the block of its kit laid there.
     *
     * @param x     from west to east
     * @param layer up from the model's foot
     * @param z     from north to south
     * @param block the block
     */
    public record Cell(int x, int layer, int z, ResourceLocation block) {
    }
}
