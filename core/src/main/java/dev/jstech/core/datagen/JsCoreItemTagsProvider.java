/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.ModMaterial;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Declares the mod's material items as members of the NeoForge common-tag ({@code c:}) conventions
 * so that machines from other mods can interact with our materials and vice-versa. Also registers
 * vanilla ingots and nuggets into the corresponding {@code c:} tags for interop.
 *
 * <p>Adding a new material or form is automatic once {@link ModMaterial} or {@link MaterialForm}
 * is updated, and no changes are needed here.
 */
public class JsCoreItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {

    public JsCoreItemTagsProvider(final PackOutput output,
                                  final CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  final ExistingFileHelper existingFileHelper) {
        super(output, Registries.ITEM, lookupProvider,
                item -> item.builtInRegistryHolder().key(),
                JsCore.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        for (final ModMaterial mat : ModMaterial.values()) {
            for (final MaterialForm form : MaterialForm.values()) {
                if (!mat.isFormActive(form)) continue;
                final TagKey<Item> tag = c(form.tagPath(mat.materialName()));
                if (mat.isVanillaForm(form)) {
                    tag(tag).add(vanillaItem(mat, form));
                } else {
                    tag(tag).add(MaterialItems.get(mat, form).get());
                }
            }
        }
    }

    private static Item vanillaItem(final ModMaterial mat, final MaterialForm form) {
        // The vanilla forms are declared by the material; the item itself is found by its vanilla name, so a
        // form vanilla does not have (such as a copper nugget) fails the data run instead of being mapped wrongly.
        final ResourceLocation id = ResourceLocation.withDefaultNamespace(form.itemKey(mat.materialName()));
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(
                () -> new IllegalArgumentException("No vanilla item " + id + " for " + mat + "/" + form));
    }

    private static TagKey<Item> c(final String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
