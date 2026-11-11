/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.OsFirstBootTrigger;
import dev.jstech.core.datagen.advancement.AdvancementTab;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * A tab of this mod's advancements: the Core's tab, under this mod's namespace, with the one criterion only this mod
 * has, a system booting for the first time.
 */
abstract class JscAdvancementTab extends AdvancementTab {

    protected JscAdvancementTab(final String tab, final ResourceLocation background) {
        super(JsComputers.MODID, tab, background);
    }

    /** Earned the first time the named system of this mod comes up in front of somebody. */
    protected static Supplier<Criterion<?>> booted(final String system) {
        return () -> OsFirstBootTrigger.booted(Optional.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                system)));
    }

    /** Earned the first time any system comes up in front of somebody. */
    protected static Supplier<Criterion<?>> bootedAny() {
        return () -> OsFirstBootTrigger.booted(Optional.empty());
    }

    /** The item of {@code id}, {@code namespace:path}, as an icon; one that is missing stops the datagen. */
    protected static ItemLike item(final String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                .orElseThrow(() -> new IllegalStateException("no item " + id + " for an advancement's icon"));
    }
}
