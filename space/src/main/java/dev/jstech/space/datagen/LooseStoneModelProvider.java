/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen;

import dev.jstech.space.JsSpace;
import dev.jstech.space.geology.LooseStones;
import dev.jstech.space.geology.StoneArrangements;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the models of the loose stones: one for each of the {@link StoneArrangements}, its stones' boxes drawn with a
 * side texture and a top texture, and one for each kind of stones in each arrangement, which gives it its rock's
 * textures. The boxes take the part of the texture where they stand, as the game draws a box with no texture
 * coordinates given.
 */
public final class LooseStoneModelProvider extends BlockModelProvider {

    /* What a model is to the files a data run checks. */
    private static final ExistingFileHelper.ResourceType MODEL = new ExistingFileHelper.ResourceType(
            PackType.CLIENT_RESOURCES, ".json", "models");

    public LooseStoneModelProvider(final PackOutput output, final ExistingFileHelper existingFiles) {
        super(output, JsSpace.MODID, existingFiles);
    }

    /**
     * Tells {@code existingFiles} of every model this writes, before any provider asks for one: the block states of
     * the stones name them.
     */
    public static void announce(final ExistingFileHelper existingFiles) {
        for (int arrangement = 0; arrangement < StoneArrangements.ALL.size(); arrangement++) {
            existingFiles.trackGenerated(file(LooseStones.arrangement(arrangement)), MODEL);
            for (final LooseStones.Kind kind : LooseStones.KINDS) {
                existingFiles.trackGenerated(file(LooseStones.model(kind, arrangement)), MODEL);
            }
        }
    }

    @Override
    public String getName() {
        return JsSpace.MODID + ":loose_stone_models";
    }

    @Override
    protected void registerModels() {
        for (int arrangement = 0; arrangement < StoneArrangements.ALL.size(); arrangement++) {
            // The game's block parent gives the stones' item a block's ways of being held and shown in a slot.
            final BlockModelBuilder model = withExistingParent(BLOCK_FOLDER + "/" + LooseStones.arrangement(
                    arrangement), mcLoc("block/block")).texture("particle", "#side");
            for (final StoneArrangements.Box box : StoneArrangements.ALL.get(arrangement)) {
                model.element().from(box.x0(), box.y0(), box.z0()).to(box.x1(), box.y1(), box.z1())
                        .allFaces((direction, face) -> face.texture(direction == Direction.UP ? "#top" : "#side"))
                        .end();
            }
            for (final LooseStones.Kind kind : LooseStones.KINDS) {
                withExistingParent(BLOCK_FOLDER + "/" + LooseStones.model(kind, arrangement),
                        modLoc(BLOCK_FOLDER + "/" + LooseStones.arrangement(arrangement)))
                        .texture("side", modLoc(kind.side())).texture("top", modLoc(kind.top()));
            }
        }
    }

    private static ResourceLocation file(final String model) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, BLOCK_FOLDER + "/" + model);
    }
}
