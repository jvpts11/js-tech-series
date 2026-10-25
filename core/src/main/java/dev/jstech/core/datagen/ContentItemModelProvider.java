/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.util.DeclarationError;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
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
                throw DeclarationError.of(entry.getId().getNamespace(), "the item " + entry.getId(),
                        "is drawn as a block's item but is no block's item",
                        "give it a look of its own with look(...), or declare it as its block's item()");
            }
            model(entry.getId().getPath(), entry.get(), entry.look());
        }
    }

    private void model(final String id, final Item item, final IItemLook look) {
        switch (look) {
            case IItemLook.Standard.OF_BLOCK ->
                    getBuilder(id).parent(new ModelFile.UncheckedModelFile(modLoc("block/" + id)));
            case IItemLook.Standard.FLAT -> basicItem(item);
            case IItemLook.Standard.HANDHELD -> getBuilder(id)
                    .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                    .texture("layer0", modLoc("item/" + id));
            case IItemLook.Standard.DRAWN_BY_ENTITY -> drawnByEntity(id);
            case IItemLook.Standard.HANDMADE -> {
                // Written by hand beside the textures; nothing to generate.
            }
            case IItemLook.Standard.HELD -> held(id);
            case IItemLook.Standard.CABLE -> cable(id, item);
            case IItemLook.Standard.BUCKET -> bucket(id, item);
            case IItemLook.Parent parent -> getBuilder(id)
                    .parent(new ModelFile.UncheckedModelFile(ContentFiles.named(modid, parent.model())));
        }
    }

    /* The game's bucket with its fluid drawn in, by NeoForge's fluid container model. */
    private void bucket(final String id, final Item item) {
        if (!(item instanceof BucketItem bucket)) {
            throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id,
                    "is drawn as a bucket but is no bucket", "make it a BucketItem, or give it another look");
        }
        getBuilder(id)
                .parent(new ModelFile.UncheckedModelFile("neoforge:item/bucket"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(bucket.content)
                .applyTint(true)
                .end();
    }

    /*
     * The model held/<id> in the hand, on the ground and on the head, and the flat texture item/<id> in a slot and an
     * item frame, by NeoForge's model of separate transforms.
     */
    private void held(final String id) {
        getBuilder(id)
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(nested().parent(new ModelFile.UncheckedModelFile(modLoc("held/" + id))))
                .perspective(ItemDisplayContext.GUI, flat(id))
                .perspective(ItemDisplayContext.FIXED, flat(id))
                .end();
    }

    /* The flat texture item/<id>, as a flat item draws it. */
    private ItemModelBuilder flat(final String id) {
        return nested().parent(new ModelFile.UncheckedModelFile("item/generated")).texture("layer0",
                modLoc("item/" + id));
    }

    /* A length of the cable in its own jacket, from the Core's model of a cable as thick as it is. */
    private void cable(final String id, final Item item) {
        if (!(item instanceof CableItem laid)) {
            throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id,
                    "is drawn as a cable but lays none", "declare it with cable(...), or give it another look");
        }
        final CableType type = laid.type();
        // A pipe's item shows its glass in its windows; a cable with none fills them with its jacket. The Core writes
        // the model of every thickness (CableItemModelProvider).
        final ItemModelBuilder model = getBuilder(id)
                .parent(new ModelFile.UncheckedModelFile(JsCore.MODID + ":block/cable/item_" + type.thickness()))
                .texture("jacket", type.jacket());
        if (type.window().isPresent() || type.thickness() == CableItemModelProvider.WINDOWED_THICKNESS) {
            model.texture("glass", type.window().orElse(type.jacket()));
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
