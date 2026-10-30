/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.multiblock.shape.ModelTriangles;
import dev.jstech.core.util.ShippedModels;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/*
 * The faces of each formed model's blocks at rest, read from the model's own file as its mod ships it and shared out
 * among its blocks as the data run shares them for the shape files: the same on the server and on a player's game, so
 * both re-shape a damaged block alike. Read the first time a model is struck, and again for a shape file read anew.
 */
final class FormedFaces {

    private static final Logger LOGGER = CoreLogs.MODELS.logger();
    private static final Map<ResourceLocation, Read> READ = new ConcurrentHashMap<>();

    private FormedFaces() {
    }

    /* Each block's faces at rest in its own space, nine numbers to a triangle; none when the model does not read. */
    static Optional<Map<Long, float[]>> of(final ResourceLocation model, final FormedShape shape) {
        final Read known = READ.get(model);
        if (known != null && known.shape() == shape) {
            return known.pieces();
        }
        final Optional<Map<Long, float[]>> pieces = read(model, shape);
        READ.put(model, new Read(shape, pieces));
        return pieces;
    }

    private static Optional<Map<Long, float[]>> read(final ResourceLocation model, final FormedShape shape) {
        final Optional<DeclaredModel> declared = ShippedModels.declared(model);
        if (declared.isEmpty()) {
            LOGGER.warn("The formed model {} is declared by no mod, so damage to it changes no shape", model);
            return Optional.empty();
        }
        try {
            final ModelData data = ShippedModels.read(declared.get());
            final float[] faces = ModelTriangles.of(data, GeometryBaker.bake(data, Set.of()),
                    declared.get().definition().detail().scale());
            return Optional.of(MeshCells.split(faces, shape.origin(), shape.cells().keySet(), shape.apart())
                    .pieces());
        } catch (final RuntimeException unreadable) {
            LOGGER.warn("The formed model {} does not read, so damage to it changes no shape", model, unreadable);
            return Optional.empty();
        }
    }

    /* What was read for a model, and from which shape file. */
    private record Read(FormedShape shape, Optional<Map<Long, float[]>> pieces) {

        Read {
            pieces = pieces.map(found -> Map.copyOf(found));
        }
    }
}
