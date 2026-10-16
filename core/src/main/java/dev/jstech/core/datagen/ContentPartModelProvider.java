/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.PartEntry;
import dev.jstech.core.content.PartLook;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the model of every part a mod declared drawn as a plate ({@link PartLook.Plate}), and of its plate at work:
 * facing north, as every part's model does, twelve pixels across and five deep against the north face, its front on
 * that face and hidden against a neighbour that covers it. A part drawn by a handmade model has nothing written.
 */
public final class ContentPartModelProvider extends BlockModelProvider {

    private final ModContent content;

    private static final float FROM = 2F;
    private static final float TO = 14F;
    private static final float DEPTH = 5F;

    public ContentPartModelProvider(final PackOutput output, final ModContent content,
                                    final ExistingFileHelper existingFiles) {
        super(output, content.modid(), existingFiles);
        this.content = content;
    }

    @Override
    public String getName() {
        return this.content.modid() + ":part_models";
    }

    @Override
    protected void registerModels() {
        for (final PartEntry<?> part : this.content.declaredParts()) {
            if (part.look() instanceof PartLook.Plate plate) {
                final String id = part.id().getPath();
                plate("block/part/" + id, plate.front(), plate.side());
                if (plate.busyFront() != null) {
                    plate("block/part/" + id + "_busy", plate.busyFront(), plate.side());
                }
            }
        }
    }

    private void plate(final String name, final String front, final String side) {
        // The game's block parent gives the part's item a block's ways of being held and shown in a slot.
        withExistingParent(name, mcLoc("block/block")).texture("front", ContentFiles.named(this.content.modid(), front))
                .texture("side", ContentFiles.named(this.content.modid(), side))
                .texture("particle", ContentFiles.named(this.content.modid(), side))
                .element().from(FROM, FROM, 0F).to(TO, TO, DEPTH)
                .allFaces((direction, face) -> {
                    face.texture(direction == Direction.NORTH ? "#front" : "#side");
                    if (direction == Direction.NORTH) {
                        face.cullface(Direction.NORTH);
                    }
                })
                .end();
    }
}
