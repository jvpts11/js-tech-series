/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.List;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * What a multipart block draws besides its own model: its parts and the pieces of its wires, each a placed standalone
 * model. A block entity hands it to the block's model through its model data under {@link #PROPERTY}; the model is
 * drawn into the world's mesh, which is built again only when the block changes, never every frame.
 *
 * @param models the placed models, in the order they are drawn
 */
public record ModelLayout(List<PlacedModel> models) {

    /** Where a multipart block's model finds its layout. */
    public static final ModelProperty<ModelLayout> PROPERTY = new ModelProperty<>();
    /** A block that draws nothing besides its own model. */
    public static final ModelLayout EMPTY = new ModelLayout(List.of());

    public ModelLayout {
        models = List.copyOf(models);
    }

    public boolean isEmpty() {
        return this.models.isEmpty();
    }
}
