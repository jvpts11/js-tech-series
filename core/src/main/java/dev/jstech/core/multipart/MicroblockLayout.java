/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.List;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * The microblocks a block space draws, which its block entity hands its model through its model data under
 * {@link #PROPERTY}, as {@link ModelLayout} hands it the devices; the model draws them into the world's mesh, built
 * again only when a part changes.
 *
 * @param blocks the microblocks, each in its place
 */
public record MicroblockLayout(List<Microblock> blocks) {

    /** Where a space's model finds its microblocks. */
    public static final ModelProperty<MicroblockLayout> PROPERTY = new ModelProperty<>();
    /** A space with no microblocks. */
    public static final MicroblockLayout EMPTY = new MicroblockLayout(List.of());

    public MicroblockLayout {
        blocks = List.copyOf(blocks);
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    /** The model data of a space: what its devices place and its microblocks. */
    public static ModelData modelData(final SpaceParts parts) {
        final List<Microblock> standing = parts.microblocks();
        return ModelData.builder().with(ModelLayout.PROPERTY, parts.layout())
                .with(PROPERTY, standing.isEmpty() ? EMPTY : new MicroblockLayout(standing)).build();
    }
}
