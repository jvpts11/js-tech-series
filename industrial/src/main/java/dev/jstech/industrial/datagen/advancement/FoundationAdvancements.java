/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.api.IndustrialAdvancementTab;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/** Foundation, T0 to T2: the first machines, power, steel, chemistry. Its tab opens with the first machine. */
public final class FoundationAdvancements extends AdvancementTab {

    public FoundationAdvancements() {
        super(JsIndustrial.MODID, IndustrialAdvancementTab.FOUNDATION.path(),
                ResourceLocation.withDefaultNamespace("textures/block/bricks.png"));
        this.root(IndustrialModule.COAL_GENERATOR.item(), "Foundation", "Get your hands on your first machine",
                () -> InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(machines())));
    }

    /* The mod's machines, any of which opens the tab. */
    private static ItemLike[] machines() {
        return new ItemLike[] {IndustrialModule.COAL_GENERATOR.item(), IndustrialModule.MACERATOR.item(),
                IndustrialModule.COMPRESSOR.item(), IndustrialModule.ELECTRIC_FURNACE.item()};
    }
}
