/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the item model of every item a mod declared, and of every declared block's item, from the look it was
 * declared with.
 */
public final class ContentItemModelProvider extends ItemModelProvider {

    private final ModContent content;

    public ContentItemModelProvider(final PackOutput output, final ModContent content,
                                    final ExistingFileHelper existingFiles) {
        super(output, content.modid(), existingFiles);
        this.content = content;
    }

    @Override
    protected void registerModels() {
        for (final BlockEntry<?> entry : content.declaredBlocks()) {
            final IItemLook look = entry.itemLook();
            if (look != null) {
                model(entry.getId().getPath(), entry.item(), look);
            }
        }
        for (final ItemEntry<?> entry : content.declaredItems()) {
            if (entry.look() == IItemLook.OF_BLOCK) {
                throw new IllegalStateException(entry.getId() + " is not a block's item and has no block to show");
            }
            model(entry.getId().getPath(), entry.get(), entry.look());
        }
    }

    private void model(final String id, final Item item, final IItemLook look) {
        switch (look) {
            case IItemLook.Standard.OF_BLOCK ->
                    getBuilder(id).parent(new ModelFile.UncheckedModelFile(modLoc("block/" + id)));
            case IItemLook.Standard.FLAT -> basicItem(item);
            case IItemLook.Standard.DRAWN_BY_ENTITY -> drawnByEntity(id);
            case IItemLook.Standard.HANDMADE -> {
                // Written by hand beside the textures; nothing to generate.
            }
            case IItemLook.Parent parent -> getBuilder(id)
                    .parent(new ModelFile.UncheckedModelFile(ContentFiles.named(modid, parent.model())));
        }
    }

    /**
     * The built-in entity model, so the block entity's own renderer draws the item, with the display transforms a
     * block item uses so it sits in the slot and the hand like any other block.
     */
    private void drawnByEntity(final String id) {
        getBuilder(id)
                .parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .transforms()
                .transform(ItemDisplayContext.GUI)
                .rotation(30, 225, 0).scale(0.625F).end()
                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, 0).scale(0.25F).end()
                .transform(ItemDisplayContext.FIXED)
                .scale(0.5F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 45, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 225, 0).scale(0.4F).end()
                .end();
    }
}
