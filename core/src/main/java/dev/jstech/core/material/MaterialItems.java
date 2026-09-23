/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.content.ModContent;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Declares one item for every (material, form) pair where {@link ModMaterial#activeModForms()} is non-empty.
 *
 * <p>Call {@link #register(ModContent)} exactly once, with the content that owns the catalogue. Retrieve items via
 * {@link #get(ModMaterial, MaterialForm)}.
 */
public final class MaterialItems {

    private static final Map<ModMaterial, EnumMap<MaterialForm, DeferredItem<Item>>> REGISTRY =
            new EnumMap<>(ModMaterial.class);

    private MaterialItems() {}

    /**
     * Declares a plain item for every active (material × mod-form) pair, named after the material and the form
     * ("Iron Dust", "Copper Plate"), so activating a form is enough to have its item named. Must be called before any
     * field that calls {@link #get}.
     */
    public static void register(final ModContent content) {
        for (final ModMaterial mat : ModMaterial.values()) {
            if (mat.activeModForms().isEmpty()) continue;
            final EnumMap<MaterialForm, DeferredItem<Item>> formMap = new EnumMap<>(MaterialForm.class);
            for (final MaterialForm form : mat.activeModForms()) {
                formMap.put(form, content.item(form.itemKey(mat.materialName()), Item::new)
                        .named(capitalise(mat.materialName()) + " " + capitalise(form.name()))
                        .register());
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

    private static String capitalise(final String word) {
        final String lower = word.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
