/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableType;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the Core's model of a length of cable as thick as each a cable may be, {@code block/cable/item_<pixels>},
 * which the item of every cable of that thickness, of any mod, is drawn from in its own jacket: a bar across the block,
 * its sides from the jacket's long strip and its ends from its cut end. The windowed pipe's, ten pixels thick, has its
 * glass over its jacket, seen through.
 */
public final class CableItemModelProvider extends BlockModelProvider {

    /** The thickness of a windowed pipe, whose item shows its glass. */
    public static final int WINDOWED_THICKNESS = 10;
    /* Where the jacket's cut end starts on its texture, under its long strip. */
    private static final float END_V = 8F;
    private static final float LENGTH = 16F;
    private static final float MIDDLE = 8F;

    public CableItemModelProvider(final PackOutput output, final ExistingFileHelper existingFiles) {
        super(output, JsCore.MODID, existingFiles);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":cable_item_models";
    }

    @Override
    protected void registerModels() {
        for (int pixels = 1; pixels <= CableType.THICKEST; pixels++) {
            final float half = pixels / 2F;
            final BlockModelBuilder model = withExistingParent("block/cable/item_" + pixels, mcLoc("block/block"))
                    .texture("particle", "#jacket");
            bar(model, half, "#jacket", true);
            if (pixels == WINDOWED_THICKNESS) {
                model.renderType("minecraft:translucent");
                bar(model, half, "#glass", false);
            }
        }
    }

    /* A bar across the block, {@code half} of its thickness either side of the middle, in {@code texture}. */
    private static void bar(final BlockModelBuilder model, final float half, final String texture,
                            final boolean ends) {
        final BlockModelBuilder.ElementBuilder bar = model.element().from(0F, MIDDLE - half, MIDDLE - half)
                .to(LENGTH, MIDDLE + half, MIDDLE + half);
        for (final Direction side : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN}) {
            bar.face(side).uvs(0F, 0F, MIDDLE, half).texture(texture).end();
        }
        if (ends) {
            for (final Direction end : new Direction[] {Direction.EAST, Direction.WEST}) {
                bar.face(end).uvs(0F, END_V, half, END_V + half).texture(texture).end();
            }
        }
        bar.end();
    }
}
