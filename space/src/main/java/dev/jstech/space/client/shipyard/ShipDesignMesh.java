/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.space.client.ModelFaces;
import dev.jstech.space.ship.ShipPartModels;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The faces of a ship's models as the Starship Design draws them, in the ship's frame, worked out once for a ship as
 * it stands ({@link ModelFaces}): which of its parts the mouse is over, and a part's outline as the camera sees it.
 */
final class ShipDesignMesh {

    private final List<ShipPartModels.Placed> placed;
    private final ModelFaces faces;

    private ShipDesignMesh(final List<ShipPartModels.Placed> placed, final ModelFaces faces) {
        this.placed = placed;
        this.faces = faces;
    }

    /**
     * The faces of {@code placed}, the models of a ship drawn for {@code owner}, each where its instance stands now;
     * {@code was} again when it is of the same models.
     */
    static ShipDesignMesh of(final BlockEntity owner, final List<ShipPartModels.Placed> placed,
                             @Nullable final ShipDesignMesh was) {
        if (was != null && was.placed.equals(placed)) {
            return was;
        }
        final List<ModelFaces.Piece> pieces = new ArrayList<>();
        for (final ShipPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isEmpty()) {
                continue;
            }
            final Matrix4f place = new Matrix4f().translate(0, 0, part.z());
            if (part.flipped()) {
                place.scale(-1, 1, 1);
            }
            // The same instance the view draws the model with, so its hidden bones and moved gear are the same.
            pieces.add(new ModelFaces.Piece(part.part(), model.get(), KitAnimations.part(owner,
                    part.model().getPath() + "@" + part.z(), model.get()), place));
        }
        return new ShipDesignMesh(List.copyOf(placed), ModelFaces.of(pieces));
    }

    /** The number of the part whose faces the line from {@code from} along {@code way} meets first; -1 for none. */
    int partAt(final Vec3 from, final Vec3 way) {
        return this.faces.partAt(from, way);
    }

    /** The outline of the part numbered {@code part} seen from {@code eye}, each edge as its two ends. */
    List<float[]> outline(final int part, final Vec3 eye) {
        return this.faces.outline(part, eye);
    }
}
