/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The shapes of the starship parts' formed models, each worked out by the data run from the blocks of its piece of the
 * kit: once a part is built and formed, each of its blocks is collided with, outlined and aimed at as its model is
 * drawn there, so a hull's floor and ceiling leave room to walk inside it. A block of the kit that stands for a block
 * of the game, a furnace in a workshop, keeps itself and has no face of the model; it is left out. Some models stand
 * out of their kits' blocks further than a block: the ramp lowered from the tail, the twin bells' nozzles, and the
 * gear's bays under the belly, as long as their legs folded into the floor; what lies out there is seen, and stands
 * in no block.
 */
public final class ShipShapes {

    /* Where the lowest corner of a piece's block at its anchor is in its model's space. */
    private static final double[] ORIGIN = {0, 0, 0};
    /* The places out of their blocks the models reach into, by their names: from one corner to the other. */
    private static final Map<String, int[]> REACH = Map.of(
            "small_ramp", new int[] {-2, -3, 6, 1, -1, 7},
            "small_twin_bells", new int[] {-2, 0, 3, 1, 2, 3},
            "small_wheels_cockpit_joint", new int[] {-1, -1, -2, 0, -1, -1},
            "small_wheels_wing_root", new int[] {-2, -1, 0, 1, -1, 1},
            "small_legs_cockpit_joint", new int[] {-1, -1, -2, 0, -1, 0},
            "small_legs_spine_end", new int[] {-2, -1, -5, 1, -1, -3});

    private ShipShapes() {
    }

    /** Declares the models laid, once, from the mod's constructor, after the parts' kits and models. */
    public static void declare() {
        ShipKits.declared().forEach((part, kit) -> {
            if (!JsSpace.MODID.equals(part.getNamespace())) {
                return;
            }
            for (final ShipKit.Piece piece : kit.pieces()) {
                final ResourceLocation model = ShipPartModels.shape(part, kit, piece);
                final String name = model.getPath().substring(model.getPath().lastIndexOf('/') + 1);
                FormedShapes.declareLaid(model, ORIGIN, own(kit, piece), reach(REACH.get(name)), Set.of());
            }
        });
    }

    /* The places between the corners {@code box}, none for no box. */
    private static Set<Long> reach(final int @Nullable [] box) {
        final Set<Long> out = new HashSet<>();
        if (box == null) {
            return out;
        }
        for (int x = box[0]; x <= box[3]; x++) {
            for (int y = box[1]; y <= box[4]; y++) {
                for (int z = box[2]; z <= box[5]; z++) {
                    out.add(MeshCells.key(x, y, z));
                }
            }
        }
        return out;
    }

    /* The blocks of {@code piece} its model is made of, both sides of a mirrored one, those of the game's left out. */
    private static Set<Long> own(final ShipKit kit, final ShipKit.Piece piece) {
        final Set<Long> out = new HashSet<>();
        final Map<String, ResourceLocation> key = kit.key();
        for (int y = 0; y < piece.layers().size(); y++) {
            final List<String> layer = piece.layers().get(y);
            for (int z = 0; z < layer.size(); z++) {
                final String row = layer.get(z);
                for (int x = 0; x < row.length(); x++) {
                    final ResourceLocation block = key.get(String.valueOf(row.charAt(x)));
                    if (row.charAt(x) == ' ' || block == null || !JsSpace.MODID.equals(block.getNamespace())) {
                        continue;
                    }
                    out.add(MeshCells.key(piece.x() + x, piece.y() + y, piece.z() + z));
                    if (piece.mirrored()) {
                        out.add(MeshCells.key(-1 - (piece.x() + x), piece.y() + y, piece.z() + z));
                    }
                }
            }
        }
        return out;
    }
}
