/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * The microblocks a block space draws, each with the boxes it is drawn as ({@link SpaceDrawing}), which its block
 * entity hands its model through its model data under {@link #PROPERTY}, as {@link ModelLayout} hands it the
 * devices; the model draws them into the world's mesh, built again only when a part changes.
 *
 * @param drawn the microblocks, each in its place, with how it is drawn
 */
public record MicroblockLayout(List<Drawn> drawn) {

    /** Where a space's model finds its microblocks. */
    public static final ModelProperty<MicroblockLayout> PROPERTY = new ModelProperty<>();
    /** A space with no microblocks. */
    public static final MicroblockLayout EMPTY = new MicroblockLayout(List.of());

    public MicroblockLayout {
        drawn = List.copyOf(drawn);
    }

    public boolean isEmpty() {
        return this.drawn.isEmpty();
    }

    /** The model data of a space: what its devices place, its microblocks and its small blocks. */
    public static ModelData modelData(final SpaceParts parts) {
        return ModelData.builder().with(ModelLayout.PROPERTY, parts.layout()).with(PROPERTY, of(parts.microblocks()))
                .with(SmallBlockLayout.PROPERTY, SmallBlockLayout.of(parts)).build();
    }

    /** How {@code blocks}, standing in one space, are drawn. */
    public static MicroblockLayout of(final List<Microblock> blocks) {
        if (blocks.isEmpty()) {
            return EMPTY;
        }
        final List<SpaceDrawing.Shown> shown = new ArrayList<>(blocks.size());
        for (final Microblock block : blocks) {
            shown.add(new SpaceDrawing.Shown(block.slot(), block.boxes(),
                    MicroblockMaterials.opaque(block.piece().material())));
        }
        final List<List<SpaceDrawing.DrawnBox>> boxes = SpaceDrawing.of(shown);
        final List<Drawn> drawn = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            drawn.add(new Drawn(blocks.get(i), boxes.get(i)));
        }
        return new MicroblockLayout(drawn);
    }

    /**
     * A microblock and the boxes it is drawn as.
     *
     * @param block the microblock
     * @param boxes its boxes as drawn, with the faces of each that are hidden
     */
    public record Drawn(Microblock block, List<SpaceDrawing.DrawnBox> boxes) {

        public Drawn {
            Objects.requireNonNull(block, "block");
            boxes = List.copyOf(boxes);
        }
    }
}
