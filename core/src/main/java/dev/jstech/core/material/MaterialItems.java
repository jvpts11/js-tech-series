/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The items of the old fixed catalogue, {@link ModMaterial} by {@link MaterialForm}, kept by their ids
 * ({@code jscore:iron_dust}) as things of the open catalogue of {@link Materials} and {@link Forms}, registered with the
 * rest of the materials' things and drawn the same way.
 *
 * @deprecated declare a use with {@link MaterialUses#use} and keep the item it gives
 */
@Deprecated
public final class MaterialItems {

    private static final Map<ModMaterial, EnumMap<MaterialForm, DeferredItem<Item>>> REGISTRY =
            new EnumMap<>(ModMaterial.class);

    private MaterialItems() {}

    /**
     * Keeps an item for every active (material × mod-form) pair. Must be called before any field that calls
     * {@link #get}. The content is no longer where they are declared: the materials' things are registered together.
     */
    public static void register(final ModContent content) {
        for (final ModMaterial mat : ModMaterial.values()) {
            if (mat.activeModForms().isEmpty()) continue;
            final EnumMap<MaterialForm, DeferredItem<Item>> formMap = new EnumMap<>(MaterialForm.class);
            for (final MaterialForm form : mat.activeModForms()) {
                formMap.put(form, MaterialUses.keep(
                        ResourceLocation.fromNamespaceAndPath(JsCore.MODID, mat.materialName()),
                        ResourceLocation.fromNamespaceAndPath(JsCore.MODID, form.name().toLowerCase(Locale.ROOT))));
            }
            REGISTRY.put(mat, formMap);
        }
    }

    /**
     * Returns the registered {@link DeferredItem} for the given material+form pair.
     *
     * @throws IllegalStateException if the pair is not in {@link ModMaterial#activeModForms()} or
     *                               if {@link #register} has not been called yet
     */
    public static DeferredItem<Item> get(final ModMaterial mat, final MaterialForm form) {
        final EnumMap<MaterialForm, DeferredItem<Item>> formMap = REGISTRY.get(mat);
        if (formMap == null || !formMap.containsKey(form)) {
            throw new IllegalStateException(
                    "Not registered: " + mat + "/" + form
                    + ". Check activeModForms or call MaterialItems.register() first");
        }
        return formMap.get(form);
    }

    /** Every material item declared, material by material and form by form. */
    public static List<DeferredItem<Item>> all() {
        final List<DeferredItem<Item>> out = new ArrayList<>();
        REGISTRY.values().forEach(forms -> out.addAll(forms.values()));
        return out;
    }
}
