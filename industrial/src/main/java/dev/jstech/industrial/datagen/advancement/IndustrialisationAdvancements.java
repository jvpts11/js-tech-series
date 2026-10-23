/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.api.IndustrialAdvancementTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Industrialisation, T3 to T5: multiblocks, the grid, the Clean Room, nuclear power. Its tab opens on reaching the
 * third tier.
 */
public final class IndustrialisationAdvancements extends AdvancementTab {

    public IndustrialisationAdvancements() {
        super(JsIndustrial.MODID, IndustrialAdvancementTab.INDUSTRIALISATION.path(),
                ResourceLocation.withDefaultNamespace("textures/block/iron_block.png"));
        this.root(Items.IRON_BLOCK, "Industrialisation", "Reach the third tier",
                reached(SeriesAxes.INDUSTRIAL_TIER, IndustrialTier.T3));
    }
}
